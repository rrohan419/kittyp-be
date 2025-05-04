package com.kittyp.payment.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItemModel {

    private Integer lineItemId;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("product_type")
    private String productType;

    @JsonProperty("hsn_or_sac")
    private String hsnOrSac;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("item_order")
    private Integer itemOrder;

    @JsonProperty("rate")
    private BigDecimal rate;

    @JsonProperty("quantity")
    private Double quantity;

    @JsonProperty("discount_amount")
    private Double discountAmount;

    @JsonProperty("discount")
    private Double discount;

    @JsonProperty("tax_id")
    private String taxId;

    @JsonProperty("tax_name")
    private String taxName;

    @JsonProperty("tax_type")
    private String taxType;

    @JsonProperty("tax_percentage")
    private Double taxPercentage;

    // @JsonProperty("is_repeated")
    private Boolean isRepeated = false;

    private Integer platformResourceId;

}
