package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kittyp.clinic.dto.ClinicInventoryDtos;
import com.kittyp.clinic.dto.ClinicInventoryDtos.AlertModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.ConsumptionRow;
import com.kittyp.clinic.dto.ClinicInventoryDtos.DashboardModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryLotModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.MovementModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.MovementRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.ScanRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.ScanResult;
import com.kittyp.clinic.dto.ClinicInventoryDtos.WeeklyReportModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryLot;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.InventoryExpiryStatus;
import com.kittyp.clinic.enums.InventoryMovementType;
import com.kittyp.clinic.enums.InventoryStockStatus;
import com.kittyp.clinic.repository.ClinicInventoryAlertStateRepository;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryLotRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.clinic.service.barcode.BarcodeProductLookup;
import com.kittyp.clinic.util.Gs1BarcodeParser;
import com.kittyp.clinic.util.Gs1BarcodeParser.ParsedBarcode;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicInventoryService {

    private final ClinicService clinicService;
    private final ClinicRepository clinicRepository;
    private final ClinicInventoryItemRepository inventoryItemRepository;
    private final ClinicInventoryLotRepository lotRepository;
    private final ClinicInventoryMovementRepository movementRepository;
    private final ClinicInventoryAlertStateRepository alertStateRepository;
    private final ClinicInventoryStockService stockService;
    private final ClinicInventoryAlertService alertService;
    private final BarcodeProductLookup barcodeProductLookup;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<InventoryItemModel> list(
            String clinicUuid, String q, String stockFilter, String expiryFilter, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        LocalDate today = LocalDate.now();
        List<ClinicInventoryItem> items;
        if (StringUtils.hasText(q)) {
            String query = q.trim().replace("%", "").replace("_", "");
            items = query.isBlank()
                    ? inventoryItemRepository.findByClinic_IdAndIsActiveTrueOrderByNameAsc(clinic.getId())
                    : inventoryItemRepository.searchActive(clinic.getId(), query);
        } else {
            items = inventoryItemRepository.findByClinic_IdAndIsActiveTrueOrderByNameAsc(clinic.getId());
        }
        return items.stream()
                .map(i -> toModel(i, clinic, today))
                .filter(m -> matchesStockFilter(m, stockFilter))
                .filter(m -> matchesExpiryFilter(m, expiryFilter))
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryItemModel create(String clinicUuid, InventoryItemRequest request, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        User actor = userRepository.findByEmailIgnoreCase(email).orElse(null);
        validateRequest(request);
        BigDecimal initialStock = nz(request.stock());
        ClinicInventoryItem item = ClinicInventoryItem.builder()
                .clinic(clinic)
                .name(request.name().trim())
                .category(request.category().trim().toLowerCase())
                .stock(initialStock)
                .unit("pcs")
                .minStock(request.minStock() == null ? 0 : Math.max(0, request.minStock()))
                .barcode(trimToNull(request.barcode()))
                .gtin(trimToNull(request.gtin()))
                .sku(trimToNull(request.sku()))
                .manufacturer(trimToNull(request.manufacturer()))
                .trackStock(request.trackStock() == null || request.trackStock())
                .price(normalizePrice(request.price()))
                .purchasePrice(request.purchasePrice() == null ? null : normalizePrice(request.purchasePrice()))
                .build();
        item.setIsActive(true);
        item = inventoryItemRepository.save(item);

        if (initialStock.compareTo(BigDecimal.ZERO) > 0 || StringUtils.hasText(request.lotNumber())
                || request.expiresOn() != null) {
            ClinicInventoryLot lot = ClinicInventoryLot.builder()
                    .clinic(clinic)
                    .inventoryItem(item)
                    .lotNumber(blankTo(request.lotNumber(), "DEFAULT"))
                    .manufacturer(trimToNull(request.manufacturer()))
                    .manufacturedOn(request.manufacturedOn())
                    .expiresOn(request.expiresOn())
                    .quantity(initialStock)
                    .build();
            lot.setIsActive(true);
            lotRepository.save(lot);
            if (initialStock.compareTo(BigDecimal.ZERO) > 0) {
                stockService.syncItemStockFromLots(item);
                ClinicInventoryMovement mov = ClinicInventoryMovement.builder()
                        .clinic(clinic)
                        .inventoryItem(item)
                        .lot(lot)
                        .type(InventoryMovementType.STOCK_IN)
                        .quantity(initialStock)
                        .previousQty(BigDecimal.ZERO)
                        .newQty(initialStock)
                        .notes("Initial stock")
                        .actor(actor)
                        .build();
                mov.setIsActive(true);
                movementRepository.save(mov);
            }
        } else {
            stockService.ensureDefaultLot(item);
        }
        item = inventoryItemRepository.findById(item.getId()).orElse(item);
        alertService.evaluateItem(clinic, item);
        return toModel(item, clinic, LocalDate.now());
    }

    @Transactional
    public InventoryItemModel update(String clinicUuid, String itemUuid, InventoryItemRequest request, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        validateRequest(request);
        ClinicInventoryItem item = inventoryItemRepository
                .findByUuidAndClinic_IdAndIsActiveTrue(itemUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "uuid", itemUuid));
        item.setName(request.name().trim());
        item.setCategory(request.category().trim().toLowerCase());
        item.setUnit(blankTo(request.unit(), item.getUnit() == null ? "pcs" : item.getUnit()));
        item.setMinStock(request.minStock() == null ? item.getMinStock() : Math.max(0, request.minStock()));
        item.setBarcode(trimToNull(request.barcode()));
        item.setGtin(trimToNull(request.gtin()));
        item.setSku(trimToNull(request.sku()));
        item.setManufacturer(trimToNull(request.manufacturer()));
        if (request.trackStock() != null) {
            item.setTrackStock(request.trackStock());
        }
        item.setPrice(normalizePrice(request.price()));
        if (request.purchasePrice() != null) {
            item.setPurchasePrice(normalizePrice(request.purchasePrice()));
        }
        // Do not overwrite stock from update payload when lots exist — use movements instead.
        // Backward compat: if client sends stock and only DEFAULT lot, sync lot quantity.
        if (request.stock() != null) {
            List<ClinicInventoryLot> lots =
                    lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId());
            if (lots.size() <= 1) {
                ClinicInventoryLot lot = lots.isEmpty() ? stockService.ensureDefaultLot(item) : lots.get(0);
                lot.setQuantity(nz(request.stock()));
                if (request.expiresOn() != null) {
                    lot.setExpiresOn(request.expiresOn());
                }
                if (request.manufacturedOn() != null) {
                    lot.setManufacturedOn(request.manufacturedOn());
                }
                if (StringUtils.hasText(request.lotNumber())) {
                    lot.setLotNumber(request.lotNumber().trim());
                }
                lotRepository.save(lot);
                stockService.syncItemStockFromLots(item);
            }
        }
        item = inventoryItemRepository.save(item);
        alertService.evaluateItem(clinic, item);
        return toModel(item, clinic, LocalDate.now());
    }

    @Transactional
    public void delete(String clinicUuid, String itemUuid, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        ClinicInventoryItem item = inventoryItemRepository
                .findByUuidAndClinic_IdAndIsActiveTrue(itemUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "uuid", itemUuid));
        item.setIsActive(false);
        inventoryItemRepository.save(item);
        for (ClinicInventoryLot lot :
                lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId())) {
            lot.setIsActive(false);
            lotRepository.save(lot);
        }
    }

    @Transactional
    public MovementModel recordMovement(String clinicUuid, MovementRequest request, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        User actor = userRepository.findByEmailIgnoreCase(email).orElse(null);
        ClinicInventoryItem item = inventoryItemRepository
                .findByUuidAndClinic_IdAndIsActiveTrue(request.itemUuid(), clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "uuid", request.itemUuid()));
        ClinicInventoryMovement mov = stockService.applyManual(
                clinic,
                item,
                request.lotUuid(),
                request.type(),
                request.quantity(),
                request.notes(),
                request.lotNumber(),
                request.manufacturedOn(),
                request.expiresOn(),
                request.manufacturer(),
                actor);
        item = inventoryItemRepository.findById(item.getId()).orElse(item);
        alertService.evaluateItem(clinic, item);
        return toMovementModel(mov);
    }

    @Transactional(readOnly = true)
    public Page<MovementModel> listMovements(String clinicUuid, int page, int size, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        return movementRepository
                .findByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(
                        clinic.getId(), PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))))
                .map(this::toMovementModel);
    }

    @Transactional(readOnly = true)
    public ScanResult scan(String clinicUuid, ScanRequest request, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        ParsedBarcode parsed = Gs1BarcodeParser.parse(request.rawCode());
        var local = barcodeProductLookup.findLocal(clinic.getId(), parsed);
        InventoryItemModel model = local.map(i -> toModel(i, clinic, LocalDate.now())).orElse(null);
        return new ScanResult(
                model != null,
                parsed.raw(),
                parsed.gtin(),
                parsed.lot(),
                parsed.expiry(),
                parsed.serial(),
                model,
                null);
    }

    @Transactional(readOnly = true)
    public DashboardModel dashboard(String clinicUuid, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        int soonDays = InventoryStatusSupport.expiringSoonDays(clinic);

        List<InventoryItemModel> all = inventoryItemRepository
                .findByClinic_IdAndIsActiveTrueOrderByNameAsc(clinic.getId())
                .stream()
                .map(i -> toModel(i, clinic, today))
                .toList();

        List<InventoryItemModel> low = all.stream()
                .filter(m -> m.stockStatus() == InventoryStockStatus.LOW_STOCK)
                .limit(20)
                .toList();
        List<InventoryItemModel> out = all.stream()
                .filter(m -> m.stockStatus() == InventoryStockStatus.OUT_OF_STOCK)
                .toList();

        List<InventoryLotModel> expiring = lotRepository
                .findExpiringSoonWithStock(clinic.getId(), today, today.plusDays(soonDays))
                .stream()
                .map(l -> toLotModel(l, clinic, today))
                .limit(20)
                .toList();
        List<InventoryLotModel> expired = lotRepository.findExpiredWithStock(clinic.getId(), today).stream()
                .map(l -> toLotModel(l, clinic, today))
                .limit(20)
                .toList();

        BigDecimal stockValue = all.stream()
                .map(m -> {
                    BigDecimal unitCost = m.purchasePrice() != null ? m.purchasePrice() : BigDecimal.ZERO;
                    return nz(m.stock()).multiply(unitCost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<ConsumptionRow> most = consumptionRows(clinic.getId(), now.minusDays(30), now, true, 10);
        List<ConsumptionRow> least = consumptionRows(clinic.getId(), now.minusDays(30), now, false, 10);

        return new DashboardModel(
                all.size(),
                low.size(),
                out.size(),
                expiring.size(),
                expired.size(),
                inventoryItemRepository.sumStock(clinic.getId()),
                stockValue,
                movementRepository.sumQuantity(
                        clinic.getId(), InventoryMovementType.STOCK_OUT, now.minusDays(7), now),
                movementRepository.sumQuantity(
                        clinic.getId(), InventoryMovementType.STOCK_OUT, now.minusDays(30), now),
                movementRepository.sumQuantity(
                        clinic.getId(), InventoryMovementType.STOCK_OUT, now.minusDays(90), now),
                low,
                expiring,
                expired,
                most,
                least,
                inventoryItemRepository.findTop10ByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(clinic.getId()).stream()
                        .map(i -> toModel(i, clinic, today))
                        .toList(),
                inventoryItemRepository.findTop10ByClinic_IdAndIsActiveTrueOrderByUpdatedAtDesc(clinic.getId()).stream()
                        .map(i -> toModel(i, clinic, today))
                        .toList(),
                movementRepository.findTop20ByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(clinic.getId()).stream()
                        .map(this::toMovementModel)
                        .toList());
    }

    @Transactional(readOnly = true)
    public List<ConsumptionRow> consumption(String clinicUuid, int days, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        LocalDateTime now = LocalDateTime.now();
        int d = days <= 0 ? 30 : Math.min(days, 365);
        return consumptionRows(clinic.getId(), now.minusDays(d), now, true, 50);
    }

    @Transactional(readOnly = true)
    public WeeklyReportModel weeklyReport(String clinicUuid, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        return buildWeeklyReport(clinic);
    }

    @Transactional(readOnly = true)
    public WeeklyReportModel buildWeeklyReport(Clinic clinic) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        int soonDays = InventoryStatusSupport.expiringSoonDays(clinic);

        List<InventoryItemModel> all = inventoryItemRepository
                .findByClinic_IdAndIsActiveTrueOrderByNameAsc(clinic.getId())
                .stream()
                .map(i -> toModel(i, clinic, today))
                .toList();

        List<MovementModel> significant = movementRepository
                .findTop20ByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(clinic.getId())
                .stream()
                .filter(m -> m.getCreatedAt() != null && !m.getCreatedAt().isBefore(from))
                .map(this::toMovementModel)
                .toList();

        return new WeeklyReportModel(
                clinic.getUuid(),
                clinic.getName(),
                weekStart,
                today,
                all.stream().filter(m -> m.stockStatus() == InventoryStockStatus.LOW_STOCK).toList(),
                all.stream().filter(m -> m.stockStatus() == InventoryStockStatus.OUT_OF_STOCK).toList(),
                lotRepository
                        .findExpiringSoonWithStock(clinic.getId(), today, today.plusDays(soonDays))
                        .stream()
                        .map(l -> toLotModel(l, clinic, today))
                        .toList(),
                lotRepository.findExpiredWithStock(clinic.getId(), today).stream()
                        .map(l -> toLotModel(l, clinic, today))
                        .toList(),
                consumptionRows(clinic.getId(), from, to, true, 10),
                consumptionRows(clinic.getId(), from, to, false, 10),
                movementRepository.sumQuantity(clinic.getId(), InventoryMovementType.STOCK_IN, from, to),
                movementRepository.sumQuantity(clinic.getId(), InventoryMovementType.STOCK_OUT, from, to),
                significant);
    }

    @Transactional(readOnly = true)
    public List<AlertModel> listAlerts(String clinicUuid, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        return alertStateRepository
                .findByClinic_IdAndResolvedAtIsNullAndIsActiveTrueOrderByLastNotifiedAtDesc(clinic.getId())
                .stream()
                .map(a -> new AlertModel(
                        a.getAlertType().name(),
                        a.getInventoryItem() == null ? null : a.getInventoryItem().getUuid(),
                        a.getInventoryItem() == null ? null : a.getInventoryItem().getName(),
                        a.getMessage(),
                        a.getLastNotifiedAt(),
                        a.getResolvedAt() != null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryLotModel> listLots(String clinicUuid, String itemUuid, String email) {
        Clinic clinic = requireClinic(clinicUuid, email);
        ClinicInventoryItem item = inventoryItemRepository
                .findByUuidAndClinic_IdAndIsActiveTrue(itemUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "uuid", itemUuid));
        LocalDate today = LocalDate.now();
        return lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId()).stream()
                .map(l -> toLotModel(l, clinic, today))
                .toList();
    }

    public InventoryItemModel toModel(ClinicInventoryItem item, Clinic clinic, LocalDate today) {
        List<ClinicInventoryLot> lots =
                lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId());
        return new InventoryItemModel(
                item.getUuid(),
                item.getName(),
                item.getCategory(),
                nz(item.getStock()),
                item.getUnit() == null ? "pcs" : item.getUnit(),
                item.getMinStock() == null ? 0 : item.getMinStock(),
                item.getBarcode(),
                item.getGtin(),
                item.getSku(),
                item.getManufacturer(),
                item.getTrackStock() == null || item.getTrackStock(),
                item.getPrice(),
                item.getPurchasePrice(),
                InventoryStatusSupport.stockStatus(item),
                InventoryStatusSupport.itemExpiryStatus(lots, clinic, today),
                InventoryStatusSupport.earliestExpiry(lots),
                lots.stream().map(l -> toLotModel(l, clinic, today)).toList());
    }

    public InventoryLotModel toLotModel(ClinicInventoryLot lot, Clinic clinic, LocalDate today) {
        ClinicInventoryItem item = lot.getInventoryItem();
        return new InventoryLotModel(
                lot.getUuid(),
                item == null ? null : item.getUuid(),
                item == null ? null : item.getName(),
                lot.getLotNumber(),
                lot.getManufacturer(),
                lot.getManufacturedOn(),
                lot.getExpiresOn(),
                nz(lot.getQuantity()),
                InventoryStatusSupport.lotExpiryStatus(lot, clinic, today));
    }

    public MovementModel toMovementModel(ClinicInventoryMovement m) {
        return new MovementModel(
                m.getUuid(),
                m.getInventoryItem() == null ? null : m.getInventoryItem().getUuid(),
                m.getInventoryItem() == null ? null : m.getInventoryItem().getName(),
                m.getLot() == null ? null : m.getLot().getUuid(),
                m.getLot() == null ? null : m.getLot().getLotNumber(),
                m.getType(),
                m.getQuantity(),
                m.getPreviousQty(),
                m.getNewQty(),
                m.getInvoiceUuid(),
                m.getNotes(),
                m.getCreatedAt());
    }

    private List<ConsumptionRow> consumptionRows(
            Long clinicId, LocalDateTime from, LocalDateTime to, boolean most, int limit) {
        List<Object[]> rows = movementRepository.sumQuantityByItem(
                clinicId, InventoryMovementType.STOCK_OUT, from, to);
        List<ConsumptionRow> list = new ArrayList<>();
        for (Object[] row : rows) {
            list.add(new ConsumptionRow(
                    (String) row[0],
                    (String) row[1],
                    row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]));
        }
        if (!most) {
            list.sort(Comparator.comparing(ConsumptionRow::quantity));
        }
        return list.stream().limit(limit).toList();
    }

    private static boolean matchesStockFilter(InventoryItemModel m, String filter) {
        if (!StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        return switch (filter.trim().toUpperCase(Locale.ROOT)) {
            case "LOW_STOCK", "LOW" -> m.stockStatus() == InventoryStockStatus.LOW_STOCK;
            case "OUT_OF_STOCK", "OUT" -> m.stockStatus() == InventoryStockStatus.OUT_OF_STOCK;
            case "IN_STOCK", "IN" -> m.stockStatus() == InventoryStockStatus.IN_STOCK;
            default -> true;
        };
    }

    private static boolean matchesExpiryFilter(InventoryItemModel m, String filter) {
        if (!StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter) || "normal".equalsIgnoreCase(filter)
                && m.expiryStatus() == InventoryExpiryStatus.NORMAL) {
            if ("normal".equalsIgnoreCase(filter)) {
                return m.expiryStatus() == InventoryExpiryStatus.NORMAL;
            }
            if (!StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter)) {
                return true;
            }
        }
        return switch (filter.trim().toUpperCase(Locale.ROOT)) {
            case "EXPIRING_SOON", "EXPIRING" -> m.expiryStatus() == InventoryExpiryStatus.EXPIRING_SOON;
            case "EXPIRED" -> m.expiryStatus() == InventoryExpiryStatus.EXPIRED;
            case "NORMAL" -> m.expiryStatus() == InventoryExpiryStatus.NORMAL;
            default -> true;
        };
    }

    Clinic requireClinic(String clinicUuid, String email) {
        clinicService.get(clinicUuid, email);
        Clinic clinic = clinicRepository.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("Clinic", "uuid", clinicUuid);
        }
        return clinic;
    }

    private void validateRequest(InventoryItemRequest request) {
        String category = request.category() == null ? "" : request.category().trim().toLowerCase();
        if (!ClinicInventoryDtos.CATEGORIES.contains(category)) {
            throw new CustomException(
                    "Category must be one of: medication, supply, equipment, food",
                    HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.name())) {
            throw new CustomException("Item name is required", HttpStatus.BAD_REQUEST);
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String blankTo(String v, String fallback) {
        return StringUtils.hasText(v) ? v.trim() : fallback;
    }

    private static String trimToNull(String v) {
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}
