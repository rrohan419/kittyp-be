package com.kittyp.doctor.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.service.S3StorageService;
import com.kittyp.doctor.dto.CreateConsultationInvoiceDto;
import com.kittyp.doctor.dto.TreatmentInvoiceData;
import com.kittyp.doctor.dto.TreatmentLineItemDto;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.enums.TreatmentInvoiceItemType;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.payment.util.PdfGenerator;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreatmentInvoiceService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ConsultationInvoiceRepository invoiceRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PdfGenerator pdfGenerator;
    private final S3StorageService s3StorageService;

    @Transactional
    public ConsultationInvoice create(User doctor, CreateConsultationInvoiceDto request) {
        List<TreatmentLineItemDto> items = normalizeItems(request);
        if (items.isEmpty()) {
            throw new CustomException("At least one invoice line item is required", HttpStatus.BAD_REQUEST);
        }

        BigDecimal computed = items.stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = nz(request.getDiscount());
        BigDecimal cgst = nz(request.getCgst());
        BigDecimal sgst = nz(request.getSgst());
        BigDecimal igst = nz(request.getIgst());
        BigDecimal tax = request.getTax() != null ? request.getTax() : cgst.add(sgst).add(igst);
        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : computed;
        BigDecimal grandTotal = request.getAmount() != null
                ? request.getAmount()
                : subtotal.subtract(discount).add(tax).max(BigDecimal.ZERO);
        BigDecimal paid = nz(request.getPaidAmount());
        BigDecimal balance = request.getBalance() != null
                ? request.getBalance()
                : grandTotal.subtract(paid).max(BigDecimal.ZERO);

        Clinic clinic = request.getClinicUuid() == null ? null : requireClinic(request.getClinicUuid());
        User owner = request.getOwnerUserUuid() == null ? null
                : userRepository.findByUuid(request.getOwnerUserUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", request.getOwnerUserUuid()));

        String invoiceNumber = nextInvoiceNumber();

        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .uuid(UUID.randomUUID().toString())
                .invoiceNumber(invoiceNumber)
                .doctor(doctor)
                .clinic(clinic)
                .petUuid(request.getPetUuid())
                .owner(owner)
                .lineItems(writeJson(items))
                .petSnapshot(writeJson(petSnapshot(request)))
                .ownerSnapshot(writeJson(ownerSnapshot(request, owner)))
                .consultationDate(request.getConsultationDate() != null
                        ? request.getConsultationDate()
                        : LocalDate.now())
                .reason(request.getReason())
                .diagnosis(request.getDiagnosis())
                .amount(grandTotal)
                .subtotal(subtotal)
                .discount(discount)
                .tax(tax)
                .cgst(cgst)
                .sgst(sgst)
                .igst(igst)
                .paidAmount(paid)
                .balance(balance)
                .paymentStatus(blankTo(request.getPaymentStatus(), paid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNPAID"))
                .paymentMode(request.getPaymentMode())
                .transactionId(request.getTransactionId())
                .currency(blankTo(request.getCurrency(), "INR").toUpperCase())
                .status(ConsultationInvoiceStatus.DRAFT)
                .notes(request.getNotes())
                .doctorNotes(request.getDoctorNotes())
                .nextVisitNotes(request.getNextVisitNotes())
                .build();

        invoice = invoiceRepository.save(invoice);

        if (Boolean.TRUE.equals(request.getGeneratePdf())) {
            invoice = generateAndAttachPdf(invoice);
        }
        return invoice;
    }

    @Transactional
    public ConsultationInvoice generateAndAttachPdf(ConsultationInvoice invoice) {
        TreatmentInvoiceData data = toPdfData(invoice);
        byte[] pdf = pdfGenerator.generateTreatmentInvoicePdf(data);
        String key = s3StorageService.uploadTreatmentInvoice(invoice.getUuid(), pdf);
        invoice.setPdfUrl(key);
        if (invoice.getStatus() == ConsultationInvoiceStatus.DRAFT) {
            invoice.setStatus(ConsultationInvoiceStatus.ISSUED);
            invoice.setIssuedAt(LocalDateTime.now());
        }
        return invoiceRepository.save(invoice);
    }

    public String getPresignedPdfUrl(ConsultationInvoice invoice) {
        if (invoice.getPdfUrl() == null || invoice.getPdfUrl().isBlank()) {
            throw new CustomException("PDF not generated for this invoice yet", HttpStatus.NOT_FOUND);
        }
        return s3StorageService.presignedTreatmentInvoiceUrl(invoice.getUuid(), java.time.Duration.ofMinutes(15))
                .toString();
    }

    public TreatmentInvoiceData toPdfData(ConsultationInvoice invoice) {
        User doctor = invoice.getDoctor();
        Clinic clinic = invoice.getClinic();
        Map<String, Object> pet = readMap(invoice.getPetSnapshot());
        Map<String, Object> ownerSnap = readMap(invoice.getOwnerSnapshot());
        List<TreatmentLineItemDto> items = readItems(invoice.getLineItems());

        TreatmentInvoiceData.TreatmentInvoiceDataBuilder builder = TreatmentInvoiceData.builder()
                .title("TAX INVOICE / MEDICAL INVOICE")
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getConsultationDate() != null
                        ? invoice.getConsultationDate().format(DATE_FMT)
                        : invoice.getCreatedAt() != null
                                ? invoice.getCreatedAt().toLocalDate().format(DATE_FMT)
                                : LocalDate.now().format(DATE_FMT))
                .paymentStatus(blankTo(invoice.getPaymentStatus(), invoice.getStatus().name()))
                .paymentMode(invoice.getPaymentMode())
                .transactionId(invoice.getTransactionId())
                .doctorName(fullName(doctor))
                .consultationDate(invoice.getConsultationDate() != null
                        ? invoice.getConsultationDate().format(DATE_FMT)
                        : null)
                .reason(invoice.getReason())
                .diagnosis(invoice.getDiagnosis())
                .subtotal(nz(invoice.getSubtotal(), invoice.getAmount()))
                .discount(nz(invoice.getDiscount()))
                .taxableAmount(nz(invoice.getSubtotal(), invoice.getAmount()).subtract(nz(invoice.getDiscount())))
                .cgst(nz(invoice.getCgst()))
                .sgst(nz(invoice.getSgst()))
                .igst(nz(invoice.getIgst()))
                .taxTotal(nz(invoice.getTax()))
                .grandTotal(invoice.getAmount())
                .paidAmount(nz(invoice.getPaidAmount()))
                .balance(nz(invoice.getBalance()))
                .doctorNotes(invoice.getDoctorNotes())
                .nextVisitNotes(invoice.getNextVisitNotes())
                .currency(blankTo(invoice.getCurrency(), "INR"))
                .petName(str(pet, "petName"))
                .petSpecies(str(pet, "petSpecies"))
                .petBreed(str(pet, "petBreed"))
                .petGender(str(pet, "petGender"))
                .petAge(str(pet, "petAge"))
                .petWeight(str(pet, "petWeight"))
                .petMicrochip(str(pet, "petMicrochip"))
                .patientId(str(pet, "patientId", invoice.getPetUuid()))
                .ownerName(str(ownerSnap, "ownerName", invoice.getOwner() != null ? fullName(invoice.getOwner()) : null))
                .ownerPhone(str(ownerSnap, "ownerPhone"))
                .ownerEmail(str(ownerSnap, "ownerEmail", invoice.getOwner() != null ? invoice.getOwner().getEmail() : null))
                .ownerAddress(str(ownerSnap, "ownerAddress"));

        if (clinic != null) {
            builder.clinicName(clinic.getName())
                    .clinicAddress(clinic.getAddress())
                    .clinicPhone(clinic.getPhone())
                    .clinicEmail(clinic.getEmail())
                    .clinicRegistrationNumber(clinic.getLicenseNumber());
        } else {
            builder.clinicName("Kittyp Veterinary Practice")
                    .clinicEmail(doctor != null ? doctor.getEmail() : null)
                    .clinicPhone(doctor != null ? doctor.getPhoneNumber() : null);
        }

        TreatmentInvoiceData data = builder.build();
        for (TreatmentLineItemDto item : items) {
            TreatmentInvoiceData.LineItem line = TreatmentInvoiceData.LineItem.builder()
                    .description(item.getDescription())
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .rate(item.getUnitPrice())
                    .amount(lineTotal(item))
                    .itemType(item.getItemType() != null ? item.getItemType().name() : "OTHER")
                    .build();
            switch (item.getItemType() != null ? item.getItemType() : TreatmentInvoiceItemType.OTHER) {
                case MEDICINE -> data.getMedicines().add(line);
                case CONSUMABLE -> data.getConsumables().add(line);
                case LAB_TEST -> data.getLaboratory().add(line);
                case SURGERY -> data.getSurgery().add(line);
                case HOSPITALIZATION -> data.getHospitalization().add(line);
                case CONSULTATION, SERVICE, VACCINATION -> data.getServices().add(line);
                default -> data.getOther().add(line);
            }
        }
        return data;
    }

    private List<TreatmentLineItemDto> normalizeItems(CreateConsultationInvoiceDto request) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            return request.getItems();
        }
        if (request.getLineItems() != null && !request.getLineItems().isBlank()) {
            try {
                List<Map<String, Object>> raw = objectMapper.readValue(request.getLineItems(),
                        new TypeReference<List<Map<String, Object>>>() {});
                List<TreatmentLineItemDto> parsed = new ArrayList<>();
                for (Map<String, Object> row : raw) {
                    TreatmentLineItemDto dto = new TreatmentLineItemDto();
                    String type = String.valueOf(row.getOrDefault("itemType",
                            row.getOrDefault("type", "CONSULTATION")));
                    try {
                        dto.setItemType(TreatmentInvoiceItemType.valueOf(type.toUpperCase().replace('-', '_')));
                    } catch (Exception e) {
                        dto.setItemType(TreatmentInvoiceItemType.OTHER);
                    }
                    dto.setDescription(String.valueOf(row.getOrDefault("description",
                            row.getOrDefault("name", "Item"))));
                    dto.setQuantity(toBd(row.getOrDefault("quantity", 1)));
                    dto.setUnitPrice(toBd(row.getOrDefault("unitPrice",
                            row.getOrDefault("amount", row.getOrDefault("price", 0)))));
                    dto.setUnit(row.get("unit") != null ? String.valueOf(row.get("unit")) : null);
                    dto.setTotal(toBd(row.get("total")));
                    parsed.add(dto);
                }
                return parsed;
            } catch (Exception e) {
                TreatmentLineItemDto single = new TreatmentLineItemDto();
                single.setItemType(TreatmentInvoiceItemType.CONSULTATION);
                single.setDescription(request.getNotes() != null ? request.getNotes() : "Consultation");
                single.setQuantity(BigDecimal.ONE);
                single.setUnitPrice(nz(request.getAmount()));
                return List.of(single);
            }
        }
        return List.of();
    }

    private String nextInvoiceNumber() {
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%d-%06d", LocalDate.now().getYear(), count);
    }

    private Map<String, Object> petSnapshot(CreateConsultationInvoiceDto request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("petName", request.getPetName());
        map.put("petSpecies", request.getPetSpecies());
        map.put("petBreed", request.getPetBreed());
        map.put("petGender", request.getPetGender());
        map.put("petAge", request.getPetAge());
        map.put("petWeight", request.getPetWeight());
        map.put("petMicrochip", request.getPetMicrochip());
        map.put("patientId", request.getPatientId() != null ? request.getPatientId() : request.getPetUuid());
        return map;
    }

    private Map<String, Object> ownerSnapshot(CreateConsultationInvoiceDto request, User owner) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ownerName", request.getOwnerName() != null ? request.getOwnerName()
                : owner != null ? fullName(owner) : null);
        map.put("ownerPhone", request.getOwnerPhone() != null ? request.getOwnerPhone()
                : owner != null ? owner.getPhoneNumber() : null);
        map.put("ownerEmail", request.getOwnerEmail() != null ? request.getOwnerEmail()
                : owner != null ? owner.getEmail() : null);
        map.put("ownerAddress", request.getOwnerAddress());
        return map;
    }

    private BigDecimal lineTotal(TreatmentLineItemDto item) {
        if (item.getTotal() != null) {
            return item.getTotal();
        }
        BigDecimal qty = nz(item.getQuantity(), BigDecimal.ONE);
        BigDecimal rate = nz(item.getUnitPrice());
        BigDecimal disc = nz(item.getDiscount());
        return qty.multiply(rate).subtract(disc).setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
    }

    private Clinic requireClinic(String uuid) {
        Clinic clinic = clinicRepository.findByUuid(uuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("Clinic", "uuid", uuid);
        }
        return clinic;
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException("Failed to serialize invoice data", HttpStatus.BAD_REQUEST, e);
        }
    }

    private List<TreatmentLineItemDto> readItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TreatmentLineItemDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String str(Map<String, Object> map, String key) {
        return str(map, key, null);
    }

    private String str(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank() || "null".equals(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private BigDecimal toBd(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal nz(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : (fallback != null ? fallback : BigDecimal.ZERO);
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
