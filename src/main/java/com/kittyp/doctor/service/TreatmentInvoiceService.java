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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;
import com.kittyp.common.service.S3StorageService;
import com.kittyp.common.util.AlphanumericIdService;
import com.kittyp.doctor.dto.CreateConsultationInvoiceDto;
import com.kittyp.doctor.dto.CreateInvoiceResultDto;
import com.kittyp.doctor.dto.OwnerInvoiceModel;
import com.kittyp.doctor.dto.TreatmentInvoiceData;
import com.kittyp.doctor.dto.TreatmentLineItemDto;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.enums.TreatmentInvoiceItemType;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.payment.util.InvoicePdfFileNamer;
import com.kittyp.payment.util.PdfGenerator;
import com.kittyp.notification.service.OutboundMessageService;
import com.kittyp.notification.service.WhatsAppPhones;
import com.kittyp.notification.service.WhatsAppSenderCredentials;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.user.service.PetAccessGuard;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreatmentInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(TreatmentInvoiceService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ConsultationInvoiceRepository invoiceRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicPetOwnerRepository clinicPetOwnerRepository;
    private final ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    private final DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;
    private final PetsRepository petsRepository;
    private final UserRepository userRepository;
    private final PetAccessGuard petAccessGuard;
    private final DoctorProfileRepository doctorProfileRepository;
    private final VisitDao visitDao;
    private final ObjectMapper objectMapper;
    private final PdfGenerator pdfGenerator;
    private final S3StorageService s3StorageService;
    private final OutboundMessageService outboundMessageService;
    private final AlphanumericIdService alphanumericIdService;

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
        BigDecimal paid = nz(request.getPaidAmount());
        // Always derive money server-side — never trust client amount/balance/subtotal/tax totals.
        BigDecimal subtotal = computed;
        rejectNegativeMoney(discount, cgst, sgst, igst, paid);
        if (discount.compareTo(subtotal) > 0) {
            throw new CustomException("Discount cannot exceed subtotal", HttpStatus.BAD_REQUEST);
        }
        BigDecimal tax = cgst.add(sgst).add(igst);
        BigDecimal grandTotal = subtotal.subtract(discount).add(tax).max(BigDecimal.ZERO);
        if (paid.compareTo(grandTotal) > 0) {
            throw new CustomException("Paid amount cannot exceed grand total", HttpStatus.BAD_REQUEST);
        }
        BigDecimal balance = grandTotal.subtract(paid).max(BigDecimal.ZERO);

        Clinic clinic = null;
        if (request.getClinicUuid() != null && !request.getClinicUuid().isBlank()) {
            clinic = requireClinic(request.getClinicUuid());
            requireDoctorAffiliated(clinic, doctor);
        }

        User owner = null;
        if (request.getOwnerUserUuid() != null && !request.getOwnerUserUuid().isBlank()) {
            owner = userRepository.findByUuid(request.getOwnerUserUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", request.getOwnerUserUuid()));
            if (clinic != null
                    && (clinic.getOwner() == null || !clinic.getOwner().getId().equals(doctor.getId()))
                    && !clinicPetOwnerRepository.existsByClinic_IdAndLinkedUser_IdAndIsActiveTrue(clinic.getId(),
                            owner.getId())) {
                throw new CustomException("Owner is not a client of this clinic.", HttpStatus.BAD_REQUEST);
            }
        }

        String petUuid = blankToNull(request.getPetUuid());
        if (petUuid != null && clinic != null) {
            Pet pet = requireInvoiceClinicPet(clinic, doctor, petUuid);
            if (owner != null && pet.getClinicOwner() != null && pet.getClinicOwner().getLinkedUser() != null
                    && !pet.getClinicOwner().getLinkedUser().getId().equals(owner.getId())) {
                throw new CustomException("Pet does not belong to the specified owner at this clinic.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        String invoiceNumber = nextInvoiceNumber();
        String visitUuid = blankToNull(request.getVisitUuid());

        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .uuid(alphanumericIdService.allocateUnique(ConsultationInvoice.class))
                .invoiceNumber(invoiceNumber)
                .doctor(doctor)
                .clinic(clinic)
                .petUuid(petUuid)
                .owner(owner)
                .visitUuid(visitUuid)
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

        if (visitUuid != null) {
            Visit visit = visitDao.findByUuid(visitUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("visit", "uuid", visitUuid));
            if (visit.getDoctor() == null || visit.getDoctor().getUser() == null
                    || !visit.getDoctor().getUser().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Visit is not owned by this doctor");
            }
            visit.setInvoiceUuid(invoice.getUuid());
            visitDao.save(visit);
        }

        if (Boolean.TRUE.equals(request.getGeneratePdf()) || Boolean.TRUE.equals(request.getSendWhatsApp())) {
            invoice = generateAndAttachPdf(invoice);
        }
        return invoice;
    }

    /**
     * Persists invoice (+ PDF when requested), then optionally sends WhatsApp.
     * WhatsApp failure does not fail the create — returns invoice with whatsappError.
     * When sendWhatsApp and visitUuid already has an invoice, reuses that row.
     */
    public CreateInvoiceResultDto createAndOptionallySendWhatsApp(User doctor, CreateConsultationInvoiceDto request) {
        boolean sendWa = Boolean.TRUE.equals(request.getSendWhatsApp());
        ConsultationInvoice invoice = resolveOrCreateDoctorInvoice(doctor, request, sendWa);
        if (!sendWa) {
            return CreateInvoiceResultDto.of(invoice);
        }
        try {
            invoice = sendInvoiceWhatsApp(invoice, null, senderFor(invoice, doctor));
            return CreateInvoiceResultDto.sent(invoice);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "WhatsApp send failed";
            log.warn("Invoice {} saved but WhatsApp send failed: {}", invoice.getUuid(), msg);
            return CreateInvoiceResultDto.sendFailed(invoice, msg);
        }
    }

    private ConsultationInvoice resolveOrCreateDoctorInvoice(
            User doctor, CreateConsultationInvoiceDto request, boolean sendWa) {
        String visitUuid = blankToNull(request.getVisitUuid());
        if (sendWa && visitUuid != null) {
            Optional<ConsultationInvoice> existing = invoiceRepository
                    .findFirstByVisitUuidAndDoctor_IdAndIsActiveTrueOrderByCreatedAtDesc(visitUuid, doctor.getId());
            if (existing.isPresent()) {
                ConsultationInvoice inv = existing.get();
                if (sameBillingScope(inv, blankToNull(request.getClinicUuid()))) {
                    if (Boolean.TRUE.equals(request.getGeneratePdf())
                            && (inv.getPdfUrl() == null || inv.getPdfUrl().isBlank())) {
                        inv = generateAndAttachPdf(inv);
                    }
                    return inv;
                }
            }
        }
        return create(doctor, request);
    }

    @Transactional
    public ConsultationInvoice createForClinic(Clinic clinic, User actor, CreateConsultationInvoiceDto request) {
        if (request.getClinicUuid() == null || request.getClinicUuid().isBlank()) {
            request.setClinicUuid(clinic.getUuid());
        } else if (!clinic.getUuid().equals(request.getClinicUuid())) {
            throw new CustomException("clinicUuid does not match path", HttpStatus.BAD_REQUEST);
        }
        User billingDoctor = resolveClinicBillingDoctor(clinic, request);
        return createAsClinicBilling(billingDoctor, clinic, request);
    }

    public CreateInvoiceResultDto createForClinicAndOptionallySend(
            Clinic clinic, User actor, CreateConsultationInvoiceDto request) {
        boolean sendWa = Boolean.TRUE.equals(request.getSendWhatsApp());
        ConsultationInvoice invoice = resolveOrCreateClinicInvoice(clinic, actor, request, sendWa);
        if (!sendWa) {
            return CreateInvoiceResultDto.of(invoice);
        }
        try {
            invoice = sendInvoiceWhatsApp(invoice, null, clinicSender(clinic));
            return CreateInvoiceResultDto.sent(invoice);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "WhatsApp send failed";
            log.warn("Clinic invoice {} saved but WhatsApp send failed: {}", invoice.getUuid(), msg);
            return CreateInvoiceResultDto.sendFailed(invoice, msg);
        }
    }

    private ConsultationInvoice resolveOrCreateClinicInvoice(
            Clinic clinic, User actor, CreateConsultationInvoiceDto request, boolean sendWa) {
        String visitUuid = blankToNull(request.getVisitUuid());
        if (sendWa && visitUuid != null) {
            Optional<ConsultationInvoice> existing = invoiceRepository
                    .findFirstByVisitUuidAndClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(visitUuid, clinic.getId());
            if (existing.isPresent()) {
                ConsultationInvoice inv = existing.get();
                if (Boolean.TRUE.equals(request.getGeneratePdf())
                        && (inv.getPdfUrl() == null || inv.getPdfUrl().isBlank())) {
                    inv = generateAndAttachPdf(inv);
                }
                return inv;
            }
        }
        return createForClinic(clinic, actor, request);
    }

    @Transactional
    public ConsultationInvoice sendInvoiceWhatsApp(
            ConsultationInvoice invoice, String overridePhone, WhatsAppSenderCredentials sender) {
        outboundMessageService.requireSenderReady(sender, senderOwnerLabel(invoice));
        ConsultationInvoice managed = invoice.getUuid() != null
                ? invoiceRepository.findByUuid(invoice.getUuid()).orElse(invoice)
                : invoice;
        if (managed.getPdfUrl() == null || managed.getPdfUrl().isBlank()) {
            managed = generateAndAttachPdf(managed);
        }
        String phone = resolveOwnerPhone(managed, overridePhone);
        String objectKey = InvoicePdfFileNamer.resolveObjectKey(managed.getPdfUrl(), managed.getUuid());
        byte[] pdf = s3StorageService.downloadTreatmentInvoice(objectKey);
        String filename = InvoicePdfFileNamer.baseName(objectKey);
        Map<String, Object> pet = readMap(managed.getPetSnapshot());
        Map<String, Object> ownerSnap = readMap(managed.getOwnerSnapshot());
        String ownerName = stringVal(ownerSnap.get("ownerName"), "Pet parent");
        String clinicName = managed.getClinic() != null && managed.getClinic().getName() != null
                ? managed.getClinic().getName()
                : "KittyP Clinic";
        String petName = stringVal(pet.get("petName"), "your pet");
        String invoiceNo = managed.getInvoiceNumber() != null ? managed.getInvoiceNumber() : managed.getUuid();
        String amount = managed.getAmount() != null
                ? managed.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "0.00";

        Pet petEntity = null;
        if (managed.getPetUuid() != null) {
            petEntity = petsRepository.findByUuid(managed.getPetUuid());
        }

        outboundMessageService.sendInvoicePdfWhatsApp(
                sender,
                phone,
                pdf,
                filename,
                List.of(ownerName, clinicName, petName, invoiceNo, amount),
                managed.getOwner(),
                petEntity);
        return managed;
    }

    /** Doctor send: clinic WhatsApp for affiliated-clinic invoices, else doctor credentials. */
    public ConsultationInvoice sendInvoiceWhatsApp(ConsultationInvoice invoice, String overridePhone) {
        return sendInvoiceWhatsApp(invoice, overridePhone, senderFor(invoice, invoice.getDoctor()));
    }

    public List<ConsultationInvoice> listForDoctor(User doctor, String clinicUuid) {
        if (clinicUuid == null || clinicUuid.isBlank()) {
            return invoiceRepository.findAllByDoctor_IdOrderByCreatedAtDesc(doctor.getId()).stream()
                    .filter(inv -> isPersonalScope(inv, doctor))
                    .toList();
        }
        Clinic clinic = requireClinic(clinicUuid);
        requireDoctorAffiliated(clinic, doctor);
        List<ConsultationInvoice> atClinic = invoiceRepository
                .findAllByDoctor_IdAndClinic_IdOrderByCreatedAtDesc(doctor.getId(), clinic.getId());
        if (!isOwnedByDoctor(clinic, doctor)) {
            return atClinic;
        }
        List<ConsultationInvoice> unscoped = invoiceRepository.findAllByDoctor_IdOrderByCreatedAtDesc(doctor.getId())
                .stream()
                .filter(inv -> inv.getClinic() == null)
                .toList();
        if (unscoped.isEmpty()) {
            return atClinic;
        }
        java.util.LinkedHashMap<String, ConsultationInvoice> merged = new java.util.LinkedHashMap<>();
        atClinic.forEach(inv -> merged.put(inv.getUuid(), inv));
        unscoped.forEach(inv -> merged.putIfAbsent(inv.getUuid(), inv));
        return List.copyOf(merged.values());
    }

    public List<ConsultationInvoice> listForClinic(Clinic clinic) {
        return invoiceRepository.findAllByClinic_IdOrderByCreatedAtDesc(clinic.getId());
    }

    public PaginationModel<ConsultationInvoice> pageForDoctor(User doctor, String clinicUuid, Integer pageNumber,
            Integer pageSize) {
        Pageable pageable = PaginationSupport.pageable(pageNumber, pageSize, "createdAt");
        if (clinicUuid == null || clinicUuid.isBlank()) {
            return PaginationSupport.fromPage(
                    invoiceRepository.findPersonalPracticeForDoctor(doctor.getId(), pageable));
        }
        Clinic clinic = requireClinic(clinicUuid);
        requireDoctorAffiliated(clinic, doctor);
        if (!isOwnedByDoctor(clinic, doctor)) {
            return PaginationSupport.fromPage(
                    invoiceRepository.findAllByDoctor_IdAndClinic_IdOrderByCreatedAtDesc(
                            doctor.getId(), clinic.getId(), pageable));
        }
        return PaginationSupport.fromPage(
                invoiceRepository.findByDoctorAndClinicOrUnscoped(doctor.getId(), clinic.getId(), pageable));
    }

    public PaginationModel<ConsultationInvoice> pageForClinic(Clinic clinic, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PaginationSupport.pageable(pageNumber, pageSize, "createdAt");
        return PaginationSupport.fromPage(
                invoiceRepository.findAllByClinic_IdOrderByCreatedAtDesc(clinic.getId(), pageable));
    }

    @Transactional(readOnly = true)
    public PaginationModel<OwnerInvoiceModel> pageForPetOwner(String petUuid, String email, Integer pageNumber,
            Integer pageSize) {
        User user = requireUser(email);
        petAccessGuard.requireOwner(user, petUuid);
        Pageable pageable = PaginationSupport.pageable(pageNumber, pageSize, "createdAt");
        return PaginationSupport.fromPage(
                invoiceRepository.findAllByPetUuidOrderByCreatedAtDesc(petUuid, pageable).map(this::toOwnerModel));
    }

    @Transactional(readOnly = true)
    public List<OwnerInvoiceModel> listForPetOwner(String petUuid, String email) {
        User user = requireUser(email);
        petAccessGuard.requireOwner(user, petUuid);
        LinkedHashMap<String, ConsultationInvoice> merged = new LinkedHashMap<>();
        invoiceRepository.findAllByPetUuidOrderByCreatedAtDesc(petUuid)
                .forEach(inv -> merged.put(inv.getUuid(), inv));
        invoiceRepository.findAllByOwner_IdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(inv -> invoiceBelongsToPet(inv, petUuid))
                .forEach(inv -> merged.putIfAbsent(inv.getUuid(), inv));
        return merged.values().stream().map(this::toOwnerModel).toList();
    }

    @Transactional(readOnly = true)
    public String pdfUrlForPetOwner(String petUuid, String invoiceUuid, String email) {
        User user = requireUser(email);
        petAccessGuard.requireOwner(user, petUuid);
        ConsultationInvoice invoice = invoiceRepository.findByUuid(invoiceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation invoice", "uuid", invoiceUuid));
        if (!invoiceBelongsToPet(invoice, petUuid)) {
            throw new ResourceNotFoundException("Consultation invoice", "uuid", invoiceUuid);
        }
        return getPresignedPdfUrl(invoice);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private boolean invoiceBelongsToPet(ConsultationInvoice invoice, String petUuid) {
        if (invoice == null || petUuid == null || petUuid.isBlank()) {
            return false;
        }
        if (petUuid.equals(invoice.getPetUuid())) {
            return true;
        }
        if (invoice.getVisitUuid() == null || invoice.getVisitUuid().isBlank()) {
            return false;
        }
        return visitDao.findByUuid(invoice.getVisitUuid())
                .map(Visit::getPet)
                .map(Pet::getUuid)
                .filter(petUuid::equals)
                .isPresent();
    }

    private OwnerInvoiceModel toOwnerModel(ConsultationInvoice invoice) {
        User doctor = invoice.getDoctor();
        Clinic clinic = invoice.getClinic();
        String doctorName = null;
        if (doctor != null) {
            String name = ((doctor.getFirstName() == null ? "" : doctor.getFirstName()) + " "
                    + (doctor.getLastName() == null ? "" : doctor.getLastName())).trim();
            doctorName = name.isBlank() ? null : name;
        }
        boolean pdfAvailable = invoice.getPdfUrl() != null && !invoice.getPdfUrl().isBlank();
        return new OwnerInvoiceModel(
                invoice.getUuid(),
                invoice.getInvoiceNumber(),
                invoice.getVisitUuid(),
                invoice.getPetUuid(),
                clinic == null ? null : clinic.getName(),
                doctorName,
                invoice.getStatus() == null ? null : invoice.getStatus().name(),
                invoice.getPaymentStatus(),
                invoice.getAmount(),
                invoice.getPaidAmount(),
                invoice.getCurrency(),
                invoice.getDiagnosis(),
                invoice.getReason(),
                invoice.getDoctorNotes(),
                invoice.getNextVisitNotes(),
                pdfAvailable,
                invoice.getConsultationDate(),
                invoice.getCreatedAt());
    }

    public ConsultationInvoice requireClinicInvoice(Clinic clinic, String invoiceUuid) {
        return invoiceRepository.findByUuidAndClinic_Id(invoiceUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation invoice", "uuid", invoiceUuid));
    }

    public BigDecimal remainingBalance(ConsultationInvoice invoice) {
        if (invoice.getBalance() != null) {
            return invoice.getBalance();
        }
        BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
        BigDecimal paid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        return amount.subtract(paid);
    }

    public boolean isRazorpayPaid(ConsultationInvoice invoice) {
        if (invoice.getStatus() == ConsultationInvoiceStatus.PAID) {
            return true;
        }
        if ("PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
            return true;
        }
        return remainingBalance(invoice).compareTo(BigDecimal.ZERO) <= 0;
    }

    @Transactional
    public ConsultationInvoice completeRazorpayCapture(String razorpayOrderId, String paymentId) {
        ConsultationInvoice invoice = invoiceRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new CustomException("Order not found", HttpStatus.NOT_FOUND));
        if (isRazorpayPaid(invoice)) {
            if (paymentId != null && (invoice.getTransactionId() == null || invoice.getTransactionId().isBlank())) {
                invoice.setTransactionId(paymentId);
                invoice = invoiceRepository.save(invoice);
                completeLinkedVisitAfterPayment(invoice);
                return refreshPdfQuietly(invoice);
            }
            completeLinkedVisitAfterPayment(invoice);
            return invoice;
        }
        BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
        invoice.setPaidAmount(amount);
        invoice.setBalance(BigDecimal.ZERO);
        invoice.setPaymentStatus("PAID");
        invoice.setPaymentMode("RAZORPAY");
        if (paymentId != null && !paymentId.isBlank()) {
            invoice.setTransactionId(paymentId);
        }
        invoice.setStatus(ConsultationInvoiceStatus.PAID);
        invoice = invoiceRepository.save(invoice);
        completeLinkedVisitAfterPayment(invoice);
        return refreshPdfQuietly(invoice);
    }

    @Transactional
    public ConsultationInvoice markPaid(ConsultationInvoice invoice, String paymentMode, String transactionId) {
        if (isRazorpayPaid(invoice)) {
            throw new CustomException("Invoice is already paid", HttpStatus.CONFLICT);
        }
        BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
        invoice.setPaidAmount(amount);
        invoice.setBalance(BigDecimal.ZERO);
        invoice.setPaymentStatus("PAID");
        invoice.setPaymentMode(normalizeRecordedPaymentMode(paymentMode));
        invoice.setTransactionId(blankToNull(transactionId));
        invoice.setStatus(ConsultationInvoiceStatus.PAID);
        invoice = invoiceRepository.save(invoice);
        completeLinkedVisitAfterPayment(invoice);
        return invoice;
    }

    /** Payment closes the clinical visit so it shows as checked out. */
    private void completeLinkedVisitAfterPayment(ConsultationInvoice invoice) {
        String visitUuid = blankToNull(invoice.getVisitUuid());
        if (visitUuid == null) {
            return;
        }
        visitDao.findByUuid(visitUuid).ifPresent(visit -> {
            VisitStatus status = visit.getStatus();
            if (status == VisitStatus.COMPLETED || status == VisitStatus.CANCELLED
                    || status == VisitStatus.NO_SHOW) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            if (visit.getCheckingOutAt() == null) {
                visit.setCheckingOutAt(now);
            }
            visit.setStatus(VisitStatus.COMPLETED);
            visit.setCompletedAt(now);
            visitDao.save(visit);
        });
    }

    static String normalizeRecordedPaymentMode(String paymentMode) {
        if (paymentMode == null || paymentMode.isBlank()) {
            throw new CustomException("Payment mode is required", HttpStatus.BAD_REQUEST);
        }
        String mode = paymentMode.trim().toUpperCase();
        return switch (mode) {
            case "UPI", "CASH", "CARD", "NEFT", "OTHER", "RAZORPAY" -> mode;
            default -> throw new CustomException("Unsupported payment mode", HttpStatus.BAD_REQUEST);
        };
    }

    public WhatsAppSenderCredentials doctorSender(User doctor) {
        if (doctor == null) {
            return WhatsAppSenderCredentials.of(null, null);
        }
        DoctorProfile profile = doctorProfileRepository.findByUser_Id(doctor.getId());
        if (profile == null) {
            return WhatsAppSenderCredentials.of(null, null);
        }
        return WhatsAppSenderCredentials.of(profile.getWhatsappToken(), profile.getWhatsappPhoneNumberId());
    }

    public WhatsAppSenderCredentials clinicSender(Clinic clinic) {
        if (clinic == null) {
            return WhatsAppSenderCredentials.of(null, null);
        }
        return WhatsAppSenderCredentials.of(clinic.getWhatsappToken(), clinic.getWhatsappPhoneNumberId());
    }

    private String senderOwnerLabel(ConsultationInvoice invoice) {
        if (invoice.getClinic() != null && invoice.getClinic().getOwner() != null
                && invoice.getDoctor() != null
                && !invoice.getClinic().getOwner().getId().equals(invoice.getDoctor().getId())) {
            return "clinic";
        }
        return "doctor";
    }

    private WhatsAppSenderCredentials senderFor(ConsultationInvoice invoice, User doctor) {
        if (invoice.getClinic() != null && !isOwnedByDoctor(invoice.getClinic(), doctor)) {
            return clinicSender(invoice.getClinic());
        }
        return doctorSender(doctor);
    }

    private static boolean sameBillingScope(ConsultationInvoice invoice, String requestedClinicUuid) {
        String invClinic = invoice.getClinic() != null ? invoice.getClinic().getUuid() : null;
        if (requestedClinicUuid == null) {
            return invClinic == null || isOwnedByDoctor(invoice.getClinic(), invoice.getDoctor());
        }
        return requestedClinicUuid.equals(invClinic);
    }

    private static boolean isPersonalScope(ConsultationInvoice invoice, User doctor) {
        return invoice.getClinic() == null || isOwnedByDoctor(invoice.getClinic(), doctor);
    }

    private static boolean isOwnedByDoctor(Clinic clinic, User doctor) {
        return clinic != null && doctor != null && clinic.getOwner() != null
                && clinic.getOwner().getId().equals(doctor.getId());
    }

    private User resolveClinicBillingDoctor(Clinic clinic, CreateConsultationInvoiceDto request) {
        String visitUuid = blankToNull(request.getVisitUuid());
        if (visitUuid != null) {
            Visit visit = visitDao.findByUuid(visitUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("visit", "uuid", visitUuid));
            if (visit.getClinic() == null || !visit.getClinic().getId().equals(clinic.getId())) {
                throw new AccessDeniedException("Visit does not belong to this clinic");
            }
            if (visit.getDoctor() != null && visit.getDoctor().getUser() != null) {
                return visit.getDoctor().getUser();
            }
        }
        if (clinic.getOwner() != null) {
            return clinic.getOwner();
        }
        throw new CustomException("Assign a doctor to the visit before creating an invoice", HttpStatus.BAD_REQUEST);
    }

    @Transactional
    protected ConsultationInvoice createAsClinicBilling(
            User doctor, Clinic clinic, CreateConsultationInvoiceDto request) {
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
        BigDecimal paid = nz(request.getPaidAmount());
        // Always derive money server-side — never trust client amount/balance/subtotal/tax totals.
        BigDecimal subtotal = computed;
        rejectNegativeMoney(discount, cgst, sgst, igst, paid);
        if (discount.compareTo(subtotal) > 0) {
            throw new CustomException("Discount cannot exceed subtotal", HttpStatus.BAD_REQUEST);
        }
        BigDecimal tax = cgst.add(sgst).add(igst);
        BigDecimal grandTotal = subtotal.subtract(discount).add(tax).max(BigDecimal.ZERO);
        if (paid.compareTo(grandTotal) > 0) {
            throw new CustomException("Paid amount cannot exceed grand total", HttpStatus.BAD_REQUEST);
        }
        BigDecimal balance = grandTotal.subtract(paid).max(BigDecimal.ZERO);

        User owner = null;
        if (request.getOwnerUserUuid() != null && !request.getOwnerUserUuid().isBlank()) {
            owner = userRepository.findByUuid(request.getOwnerUserUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", request.getOwnerUserUuid()));
            if (!clinicPetOwnerRepository.existsByClinic_IdAndLinkedUser_IdAndIsActiveTrue(clinic.getId(),
                    owner.getId())) {
                throw new CustomException("Owner is not a client of this clinic.", HttpStatus.BAD_REQUEST);
            }
        }

        String petUuid = blankToNull(request.getPetUuid());
        if (petUuid != null) {
            Pet pet = requireInvoiceClinicPet(clinic, doctor, petUuid);
            if (owner != null && pet.getClinicOwner() != null && pet.getClinicOwner().getLinkedUser() != null
                    && !pet.getClinicOwner().getLinkedUser().getId().equals(owner.getId())) {
                throw new CustomException("Pet does not belong to the specified owner at this clinic.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        String invoiceNumber = nextInvoiceNumber();
        String visitUuid = blankToNull(request.getVisitUuid());

        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .uuid(alphanumericIdService.allocateUnique(ConsultationInvoice.class))
                .invoiceNumber(invoiceNumber)
                .doctor(doctor)
                .clinic(clinic)
                .petUuid(petUuid)
                .owner(owner)
                .visitUuid(visitUuid)
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

        if (visitUuid != null) {
            Visit visit = visitDao.findByUuid(visitUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("visit", "uuid", visitUuid));
            visit.setInvoiceUuid(invoice.getUuid());
            visitDao.save(visit);
        }

        if (Boolean.TRUE.equals(request.getGeneratePdf()) || Boolean.TRUE.equals(request.getSendWhatsApp())) {
            invoice = generateAndAttachPdf(invoice);
        }
        return invoice;
    }

    /**
     * WhatsApp destination must be the invoice owner phone (snapshot or linked user).
     * A client-supplied override is allowed only when it normalizes to the same E.164 digits
     * (formatting differences), never an arbitrary third-party number.
     */
    private String resolveOwnerPhone(ConsultationInvoice invoice, String overridePhone) {
        String canonical = canonicalOwnerPhone(invoice);
        if (overridePhone == null || overridePhone.isBlank()) {
            return canonical;
        }
        String overrideDigits = WhatsAppPhones.toE164Digits(overridePhone, "91");
        String canonicalDigits = WhatsAppPhones.toE164Digits(canonical, "91");
        if (overrideDigits == null || canonicalDigits == null || !overrideDigits.equals(canonicalDigits)) {
            throw new CustomException(
                    "WhatsApp destination must match the invoice owner phone",
                    HttpStatus.BAD_REQUEST);
        }
        return canonical;
    }

    private String canonicalOwnerPhone(ConsultationInvoice invoice) {
        Map<String, Object> ownerSnap = readMap(invoice.getOwnerSnapshot());
        String fromSnap = stringVal(ownerSnap.get("ownerPhone"), null);
        if (fromSnap != null && !fromSnap.isBlank()) {
            return fromSnap;
        }
        if (invoice.getOwner() != null && invoice.getOwner().getPhoneNumber() != null
                && !invoice.getOwner().getPhoneNumber().isBlank()) {
            return invoice.getOwner().getPhoneNumber();
        }
        throw new CustomException("Owner phone is required to send invoice on WhatsApp", HttpStatus.BAD_REQUEST);
    }

    private static void rejectNegativeMoney(BigDecimal... amounts) {
        for (BigDecimal amount : amounts) {
            if (amount != null && amount.signum() < 0) {
                throw new CustomException("Money fields cannot be negative", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private static String stringVal(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? fallback : s;
    }

    @Transactional
    public ConsultationInvoice generateAndAttachPdf(ConsultationInvoice invoice) {
        TreatmentInvoiceData data = toPdfData(invoice);
        byte[] pdf = pdfGenerator.generateTreatmentInvoicePdf(data);
        String key;
        if (invoice.getPdfUrl() != null && !invoice.getPdfUrl().isBlank()) {
            key = InvoicePdfFileNamer.resolveObjectKey(invoice.getPdfUrl(), invoice.getUuid());
        } else {
            key = InvoicePdfFileNamer.objectKey(invoice.getInvoiceNumber(), invoice.getUuid());
        }
        s3StorageService.uploadTreatmentInvoice(key, pdf);
        invoice.setPdfUrl(key);
        if (invoice.getStatus() == ConsultationInvoiceStatus.DRAFT) {
            invoice.setStatus(ConsultationInvoiceStatus.ISSUED);
            invoice.setIssuedAt(LocalDateTime.now());
        }
        return invoiceRepository.save(invoice);
    }

    /**
     * Rebuild S3 PDF after payment. Call through the Spring proxy after markPaid
     * commits so a PDF/S3 failure cannot roll back paid status.
     */
    @Transactional
    public ConsultationInvoice refreshPdfQuietly(ConsultationInvoice invoice) {
        try {
            ConsultationInvoice managed = invoice.getUuid() != null
                    ? invoiceRepository.findByUuid(invoice.getUuid()).orElse(invoice)
                    : invoice;
            return generateAndAttachPdf(managed);
        } catch (Exception e) {
            log.warn("Invoice {} paid but PDF refresh failed: {}", invoice.getUuid(), e.getMessage());
            return invoice;
        }
    }

    public String getPresignedPdfUrl(ConsultationInvoice invoice) {
        if (invoice.getPdfUrl() == null || invoice.getPdfUrl().isBlank()) {
            throw new CustomException("PDF not generated for this invoice yet", HttpStatus.NOT_FOUND);
        }
        String key = InvoicePdfFileNamer.resolveObjectKey(invoice.getPdfUrl(), invoice.getUuid());
        return s3StorageService.presignedTreatmentInvoiceUrl(key, java.time.Duration.ofMinutes(15))
                .toString();
    }

    public TreatmentInvoiceData toPdfData(ConsultationInvoice invoice) {
        User doctor = invoice.getDoctor();
        Clinic clinic = invoice.getClinic();
        Map<String, Object> pet = readMap(invoice.getPetSnapshot());
        Map<String, Object> ownerSnap = readMap(invoice.getOwnerSnapshot());
        List<TreatmentLineItemDto> items = readItems(invoice.getLineItems());
        String paymentStatus = derivedPaymentStatus(invoice);
        boolean paid = "PAID".equals(paymentStatus);
        boolean partial = "PARTIAL".equals(paymentStatus);
        BigDecimal balanceDue = nz(invoice.getBalance());
        String paidAt = paid && invoice.getUpdatedAt() != null
                ? invoice.getUpdatedAt().format(DATE_FMT)
                : null;

        TreatmentInvoiceData.TreatmentInvoiceDataBuilder builder = TreatmentInvoiceData.builder()
                .title("TAX INVOICE / MEDICAL INVOICE")
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getConsultationDate() != null
                        ? invoice.getConsultationDate().format(DATE_FMT)
                        : invoice.getCreatedAt() != null
                                ? invoice.getCreatedAt().toLocalDate().format(DATE_FMT)
                                : LocalDate.now().format(DATE_FMT))
                .paymentStatus(paymentStatus)
                .paymentStatusLabel(paymentStatusLabel(paymentStatus))
                .paymentSummary(paymentSummary(paymentStatus, balanceDue))
                .paymentMode(invoice.getPaymentMode())
                .paymentModeLabel(paymentModeLabel(invoice.getPaymentMode()))
                .transactionId(blankToNull(invoice.getTransactionId()))
                .razorpayOrderId(blankToNull(invoice.getRazorpayOrderId()))
                .paidAt(paidAt)
                .paid(paid)
                .partial(partial)
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
                .patientId(friendlyPatientId(str(pet, "patientId", invoice.getPetUuid())))
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

    String derivedPaymentStatus(ConsultationInvoice invoice) {
        if (isRazorpayPaid(invoice)) {
            return "PAID";
        }
        if (nz(invoice.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIAL";
        }
        return "UNPAID";
    }

    static String paymentStatusLabel(String status) {
        if ("PAID".equalsIgnoreCase(status)) {
            return "Paid";
        }
        if ("PARTIAL".equalsIgnoreCase(status)) {
            return "Partially paid";
        }
        return "Unpaid";
    }

    static String paymentModeLabel(String mode) {
        if (mode == null || mode.isBlank()) {
            return null;
        }
        return switch (mode.trim().toUpperCase()) {
            case "RAZORPAY" -> "Online (Razorpay)";
            case "UPI" -> "UPI";
            case "CASH" -> "Cash";
            case "CARD" -> "Card";
            case "NEFT" -> "Bank transfer";
            default -> mode.trim();
        };
    }

    static String paymentSummary(String status, BigDecimal balance) {
        if ("PAID".equalsIgnoreCase(status)) {
            return "This invoice is paid in full. Thank you.";
        }
        String due = "₹" + nzStatic(balance).setScale(2, RoundingMode.HALF_UP).toPlainString();
        if ("PARTIAL".equalsIgnoreCase(status)) {
            return due + " is still due after a partial payment.";
        }
        return due + " is due. Pay at the clinic or collect online.";
    }

    static String friendlyPatientId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String trimmed = id.trim();
        if (trimmed.length() == 36 && trimmed.chars().filter(ch -> ch == '-').count() == 4) {
            return null;
        }
        return trimmed;
    }

    private static BigDecimal nzStatic(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
        map.put("ownerPhone", resolveSnapshotPhone(request, owner));
        map.put("ownerEmail", request.getOwnerEmail() != null ? request.getOwnerEmail()
                : owner != null ? owner.getEmail() : null);
        map.put("ownerAddress", request.getOwnerAddress());
        return map;
    }

    /**
     * When a linked platform user exists, prefer their phone and reject a mismatched client value
     * so create+send cannot retarget WhatsApp to an arbitrary number.
     */
    private String resolveSnapshotPhone(CreateConsultationInvoiceDto request, User owner) {
        String fromOwner = owner != null ? blankToNull(owner.getPhoneNumber()) : null;
        String fromReq = blankToNull(request.getOwnerPhone());
        if (fromOwner != null && fromReq != null) {
            String ownerDigits = WhatsAppPhones.toE164Digits(fromOwner, "91");
            String reqDigits = WhatsAppPhones.toE164Digits(fromReq, "91");
            if (!ownerDigits.equals(reqDigits)) {
                throw new CustomException(
                        "Owner phone must match the linked patient phone",
                        HttpStatus.BAD_REQUEST);
            }
            return fromOwner;
        }
        return fromOwner != null ? fromOwner : fromReq;
    }

    /** Always derive line total from qty × rate − discount — never trust client {@code total}. */
    private BigDecimal lineTotal(TreatmentLineItemDto item) {
        BigDecimal qty = nz(item.getQuantity(), BigDecimal.ONE);
        if (qty.signum() <= 0) {
            throw new CustomException("Line item quantity must be positive", HttpStatus.BAD_REQUEST);
        }
        BigDecimal rate = nz(item.getUnitPrice());
        if (rate.signum() < 0) {
            throw new CustomException("Line item unit price cannot be negative", HttpStatus.BAD_REQUEST);
        }
        BigDecimal disc = nz(item.getDiscount());
        if (disc.signum() < 0) {
            throw new CustomException("Line item discount cannot be negative", HttpStatus.BAD_REQUEST);
        }
        BigDecimal total = qty.multiply(rate).subtract(disc).setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
        item.setTotal(total);
        return total;
    }

    private Clinic requireClinic(String uuid) {
        Clinic clinic = clinicRepository.findByUuid(uuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("Clinic", "uuid", uuid);
        }
        return clinic;
    }

    /**
     * Clinic patient if registered ({@code clinic_id}), multi-clinic enrolled, or
     * (personal practice only) on the doctor's patient roster.
     */
    private Pet requireInvoiceClinicPet(Clinic clinic, User doctorUser, String petUuid) {
        String key = petUuid == null ? "" : petUuid.trim();
        Pet resolved = petsRepository.findByUuidIgnoreCase(key).orElse(null);
        if (resolved == null) {
            resolved = petsRepository.findFirstByClinic_IdAndPatientNumberIgnoreCase(clinic.getId(), key).orElse(null);
        }
        String id = resolved != null ? resolved.getUuid() : key;
        return petsRepository.findByUuidAndClinic_IdAndIsActiveTrue(id, clinic.getId())
                .or(() -> clinicPetEnrollmentRepository
                        .findByClinic_IdAndPet_UuidAndIsActiveTrue(clinic.getId(), id)
                        .map(ClinicPetEnrollment::getPet)
                        .filter(p -> Boolean.TRUE.equals(p.getIsActive())))
                .or(() -> {
                    boolean personal = clinic.getOwner() != null && doctorUser != null
                            && clinic.getOwner().getId().equals(doctorUser.getId());
                    if (!personal) {
                        return java.util.Optional.empty();
                    }
                    DoctorProfile profile = doctorProfileRepository.findByUser_Id(doctorUser.getId());
                    if (profile == null) {
                        return java.util.Optional.empty();
                    }
                    if (!doctorPatientEnrollmentRepository.existsByDoctor_IdAndPet_UuidAndIsActiveTrue(profile.getId(),
                            id)) {
                        return java.util.Optional.empty();
                    }
                    return petsRepository.findOptionalByUuid(id)
                            .filter(p -> Boolean.TRUE.equals(p.getIsActive()));
                })
                .orElseThrow(() -> new CustomException("Pet is not a patient of this clinic.", HttpStatus.NOT_FOUND));
    }

    private void requireDoctorAffiliated(Clinic clinic, User doctor) {
        boolean owner = clinic.getOwner() != null && clinic.getOwner().getId().equals(doctor.getId());
        boolean affiliated = clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(),
                doctor.getId());
        if (!owner && !affiliated) {
            throw new AccessDeniedException("You are not affiliated with this clinic.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
