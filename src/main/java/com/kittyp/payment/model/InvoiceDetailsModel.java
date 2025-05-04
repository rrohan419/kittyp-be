package com.kittyp.payment.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InvoiceDetailsModel {
    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("currency_id")
    private String currencyId;

    @JsonProperty("contact_persons")
    private List<String> contactPersons;

    // @JsonProperty("invoice_number")
    // private String invoiceNumber;

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("payment_terms")
    private String paymentTerms;

    @JsonProperty("payment_terms_label")
    private String paymentTermLabel;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("discount")
    private String discount;

    @JsonProperty("discount_type")
    private String discountType;

    @JsonProperty("is_discount_before_tax")
    private Boolean isDiscountBeforeTax;

    @JsonProperty("is_inclusive_tax")
    private Boolean isInclusiveTax;

    @JsonProperty("line_items")
    private List<LineItemModel> lineItems;

    @JsonProperty("allow_partial_payments")
    private Boolean allowPartialPayments;

    @JsonProperty("custom_body")
    private String customBody;

    @JsonProperty("custom_subject")
    private String customSubject;

    @JsonProperty("timesheet_string")
    private String timesheetString;

    private String notes;

    private String terms;

    private Double adjustment;

    @JsonProperty("adjustment_description")
    private String adjustmentDescription;

    @JsonProperty("tax_id")
    private String taxId;

}
