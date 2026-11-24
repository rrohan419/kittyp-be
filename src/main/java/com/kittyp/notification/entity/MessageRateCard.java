package com.kittyp.notification.entity;

import java.math.BigDecimal;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.notification.enums.MessageChannel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messaging_rate_cards")
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageRateCard extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 16)
    private MessageChannel channel;
    @Column(name = "unit_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitRate;
    @Column(name = "included_monthly", nullable = false)
    private Integer includedMonthly = 0;
    @Column(name = "segment_size", nullable = false)
    private Integer segmentSize = 1;
    @Column(name = "pack_credits", nullable = false)
    private Integer packCredits;
    @Column(name = "pack_price_ex_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal packPriceExTax;
    @Column(name = "tax_percent", nullable = false, precision = 6, scale = 3)
    private BigDecimal taxPercent;
}
