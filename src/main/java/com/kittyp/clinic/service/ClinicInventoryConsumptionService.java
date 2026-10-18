package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryMovementSource;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dto.TreatmentLineItemDto;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.TreatmentInvoiceItemType;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicInventoryConsumptionService {

    private final ClinicInventoryItemRepository inventoryRepository;
    private final ClinicInventoryMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** Bind stocked lines to this clinic's catalog. Does not change quantities. */
    public void prepareLines(Clinic clinic, List<TreatmentLineItemDto> items) {
        if (items == null) {
            return;
        }
        for (TreatmentLineItemDto line : items) {
            if (clinic == null || !isStocked(line.getItemType())) {
                line.setInventoryItemUuid(null);
                line.setAllowShortStock(null);
                continue;
            }
            String itemUuid = blankToNull(line.getInventoryItemUuid());
            if (itemUuid == null) {
                throw new CustomException("Choose an inventory item for " + line.getItemType().name().toLowerCase()
                        + " lines", HttpStatus.BAD_REQUEST);
            }
            ClinicInventoryItem item = inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(itemUuid, clinic.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("clinic inventory item", "uuid", itemUuid));
            requireCategory(line.getItemType(), item);
            requireWholeUnits(line.getQuantity(), item.getName());
            line.setInventoryItemUuid(item.getUuid());
            line.setDescription(item.getName());
            line.setUnit(item.getUnit());
        }
    }

    /** Decrement stock for linked lines. Caller must invoke this only once, while the invoice is still a draft. */
    @Transactional
    public void consumeForIssue(ConsultationInvoice invoice) {
        if (invoice.getClinic() == null) {
            return;
        }
        List<TreatmentLineItemDto> lines = readItems(invoice.getLineItems());
        User actor = currentActor();
        for (TreatmentLineItemDto line : lines) {
            if (!isStocked(line.getItemType()) || blankToNull(line.getInventoryItemUuid()) == null) {
                continue;
            }
            ClinicInventoryItem item = inventoryRepository
                    .findActiveForUpdate(line.getInventoryItemUuid(), invoice.getClinic().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("clinic inventory item", "uuid",
                            line.getInventoryItemUuid()));
            requireCategory(line.getItemType(), item);
            int units = requireWholeUnits(line.getQuantity(), item.getName());
            int onHand = item.getQuantity() == null ? 0 : item.getQuantity();
            if (units > onHand && !Boolean.TRUE.equals(line.getAllowShortStock())) {
                throw new CustomException(
                        "Only " + onHand + " " + item.getName() + " on hand. Confirm billing " + units + " to continue.",
                        HttpStatus.CONFLICT);
            }
            int after = onHand - units;
            item.setQuantity(after);
            inventoryRepository.save(item);
            String note = units > onHand ? "Billed below stock" : "Invoice consumption";
            record(invoice, item, actor, -units, after, note);
        }
    }

    static boolean isStocked(TreatmentInvoiceItemType itemType) {
        return itemType == TreatmentInvoiceItemType.MEDICINE
                || itemType == TreatmentInvoiceItemType.CONSUMABLE
                || itemType == TreatmentInvoiceItemType.VACCINATION
                || itemType == TreatmentInvoiceItemType.FOOD
                || itemType == TreatmentInvoiceItemType.EQUIPMENT;
    }

    static String expectedCategory(TreatmentInvoiceItemType itemType) {
        if (itemType == TreatmentInvoiceItemType.CONSUMABLE) {
            return KeyConstant.SUPPLY;
        }
        if (itemType == TreatmentInvoiceItemType.MEDICINE || itemType == TreatmentInvoiceItemType.VACCINATION) {
            return KeyConstant.MEDICATION;
        }
        if (itemType == TreatmentInvoiceItemType.FOOD) {
            return KeyConstant.FOOD;
        }
        if (itemType == TreatmentInvoiceItemType.EQUIPMENT) {
            return KeyConstant.EQUIPMENT;
        }
        return null;
    }

    private static void requireCategory(TreatmentInvoiceItemType itemType, ClinicInventoryItem item) {
        String expected = expectedCategory(itemType);
        String actual = item.getCategory() == null ? "" : item.getCategory().trim();
        if (expected == null || !expected.equalsIgnoreCase(actual)) {
            throw new CustomException(item.getName() + " is not in the " + expected + " catalog",
                    HttpStatus.BAD_REQUEST);
        }
    }

    static int requireWholeUnits(BigDecimal quantity, String itemName) {
        if (quantity == null) {
            throw new CustomException(itemName + " quantity is required", HttpStatus.BAD_REQUEST);
        }
        BigDecimal units = quantity.stripTrailingZeros();
        if (units.scale() > 0) {
            throw new CustomException(itemName + " must be billed in whole units", HttpStatus.BAD_REQUEST);
        }
        try {
            return units.intValueExact();
        } catch (ArithmeticException ex) {
            throw new CustomException(itemName + " quantity is too large", HttpStatus.BAD_REQUEST);
        }
    }

    private void record(ConsultationInvoice invoice, ClinicInventoryItem item, User actor, int delta, int after,
            String note) {
        ClinicInventoryMovement movement = new ClinicInventoryMovement();
        movement.setClinic(invoice.getClinic());
        movement.setItemUuid(item.getUuid());
        movement.setItemName(item.getName());
        movement.setActor(actor);
        movement.setActorName(ClinicInventoryServiceImpl.displayName(actor));
        movement.setAction(InventoryMovementAction.CONSUME);
        movement.setQuantityDelta(delta);
        movement.setQuantityAfter(after);
        movement.setNote(note);
        movement.setSource(InventoryMovementSource.INVOICE);
        movement.setInvoiceUuid(invoice.getUuid());
        movement.setInvoiceNumber(invoice.getInvoiceNumber());
        movement.setIsActive(true);
        movementRepository.save(movement);
    }

    private User currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }

    private List<TreatmentLineItemDto> readItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<TreatmentLineItemDto>>() {
            });
        } catch (Exception e) {
            throw new CustomException("Invoice line items could not be read", HttpStatus.BAD_REQUEST);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
