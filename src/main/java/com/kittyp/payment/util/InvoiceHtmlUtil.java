/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.util;

import com.kittyp.payment.constants.ReciptConstant;

/**
 * @author rrohan419@gmail.com 
 */
public class InvoiceHtmlUtil {
	private static final String TAX_HEADERS_IGST = """
            <th>IGST %</th>
            <th>IGST Amount</th>
            """;
    private static final String TAX_TOTALS_IGST = """
            <tr>
                <td colspan="6" class="total">Sub Total</td>
                <td>{{subTotal}}</td>
            </tr>
            {{discountKey}}
            <tr>
                <td colspan="6" class="total">IGST Total({{IGSTTotalKey}}%)</td>
                <td>{{igstTotal}}</td>
            </tr>
            {{adjustmentKey}}
             <tr>
                <td colspan="6" class="total total-box">Total</td>
                <td class="total-box">{{totalAmount}}</td>
            </tr>
            """;

    private static final String TAX_HEADERS_CGST_SGST = """
            <th>CGST %</th>
            <th>CGST Amount</th>
            <th>SGST %</th>
            <th>SGST Amount</th>
            """;
    private static final String TAX_TOTALS_CGST_SGST = """
             <tr>
                 <td colspan="8" class="total">Sub Total</td>
                 <td>{{subTotal}}</td>
            </tr>
                {{discountKey}}
             <tr>
                 <td colspan="8" class="total">CGST Total({{CGSTTotalkey}}%)</td>
                 <td>{{cgstTotal}}</td>
             </tr>
             <tr>
                 <td colspan="8" class="total">SGST Total({{SGSTTotalkey}}%)</td>
                 <td>{{sgstTotal}}</td>
             </tr>
            {{adjustmentKey}}
             <tr>
                 <td colspan="8" class="total total-box">Total</td>
                 <td class="total-box">{{totalAmount}}</td>
             </tr>
             """;
    public static final String DISCOUNT_IGST = """
             <tr>
                <td colspan="6" class="total">Discount</td>
                <td>{{discount}}</td>
            </tr>
            """;
    public static final String DISCOUNT_CGST_SGST = """
             <tr>
                <td colspan="8" class="total">Discount</td>
                <td>{{discount}}</td>
            </tr>
            """;

    public static final String ADJUSTMENT_IGST = """
             <tr>
                <td colspan="6" class="total">Adjustment</td>
                <td>{{adjustment}}</td>
            </tr>
            """;

    public static final String ADJUSTMENT_CGST_SGST = """
             <tr>
                <td colspan="8" class="total">Adjustment</td>
                <td>{{adjustment}}</td>
            </tr>
            """;

    public static String getHtmlTemplate(Boolean isIGST, Boolean isDiscount, Boolean isAdjustMent) {
        String taxHeaders, taxTotals;
        if (isIGST) {
            taxHeaders = TAX_HEADERS_IGST;
            taxTotals = TAX_TOTALS_IGST;

            if (Boolean.TRUE.equals(isDiscount)) {
                taxTotals = taxTotals.replace(ReciptConstant.DISCOUNT_KEY, DISCOUNT_IGST);
            } else {
                taxTotals = taxTotals.replace(ReciptConstant.DISCOUNT_KEY, " ");
            }

            if (Boolean.TRUE.equals(isAdjustMent)) {
                taxTotals = taxTotals.replace(ReciptConstant.ADJUSTMENT_KEY, ADJUSTMENT_IGST);
            } else {
                taxTotals = taxTotals.replace(ReciptConstant.ADJUSTMENT_KEY, " ");
            }
        } else {
            taxHeaders = TAX_HEADERS_CGST_SGST;
            taxTotals = TAX_TOTALS_CGST_SGST;

            if (Boolean.TRUE.equals(isDiscount)) {
                taxTotals = taxTotals.replace(ReciptConstant.DISCOUNT_KEY, DISCOUNT_CGST_SGST);
            } else {
                taxTotals = taxTotals.replace(ReciptConstant.DISCOUNT_KEY, " ");
            }

            if (Boolean.TRUE.equals(isAdjustMent)) {
                taxTotals = taxTotals.replace(ReciptConstant.ADJUSTMENT_KEY, ADJUSTMENT_CGST_SGST);
            } else {
                taxTotals = taxTotals.replace(ReciptConstant.ADJUSTMENT_KEY, " ");
            }
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Invoice</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            margin: 40px;
                            font-size: 14px;
                        }
                        .header { text-align: left; }
                        .header img { max-width: 200px; }
                        .bank-details, .terms { margin-top: 15px; }
                        .invoice-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: start;
                            margin-top: 15px;
                        }
                        .invoice-details { flex: 1; }
                        .date-details { text-align: right; }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 15px;
                        }
                        th, td {
                            border: 1px solid #000;
                            padding: 8px;
                            text-align: left;
                        }
                        th { background-color: #2D2D62; color: white; }
                        .total { text-align: right; font-weight: bold; }
                        .total-box { background-color: #E0E0E0; padding: 8px; font-weight: bold; }
                        .bank-container { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 10px; }
                        .bank-column { flex: 1; min-width: 280px; }
                        .bank-column p { margin: 5px 0; }
                        .terms { margin-top: 20px; text-align: justify; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <img src="https://metrics-sproutops-bucket.s3.ap-south-1.amazonaws.com/assets/mindbowser-logo.jpg" alt="Mindbowser Logo">
                        <h2>Mindbowser Info Solutions Pvt Ltd</h2>
                        <p>
                            7th Floor, Office No 701 & 702, Icon Tower,<br>
                            Pan Card Club Road, Baner, Pune, Maharashtra 411045, India<br>
                            GSTIN: 27AAHCM8155A1ZJ
                        </p>
                    </div>
                    <div class="bank-details">
                        <h3>Bill To:</h3>
                        <div class="bank-container">
                            <div class="bank-column">
                                <span><strong>{{projectName}}</strong></p>
                                <span><strong>{{customerName}}</strong><br>{{billingAddress}}</p>
                            </div>
                            <div class="bank-column">
                                <p><strong>Date:</strong> {{invoiceDate}}</p>
                                <p><strong>Due Date:</strong> {{dueDate}}</p>
                            </div>
                        </div>
                    </div>
                    <table>
                        <tr>
                            <th>#</th>
                            <th>Item & Description</th>
                            <th>Qty</th>
                            <th>Rate</th>
                """
                + taxHeaders + """
                                    <th class="total">Amount</th>
                                </tr>
                                {{lineItems}}

                        """ + taxTotals
                + """
                            </table>
                            <p><strong>Total in Words:</strong> {{totalInWords}}</p>
                            <div class="bank-details">
                                <h3>Bank Account Details:</h3>
                                <div class="bank-container">
                                    <div class="bank-column">
                                        <p><strong>Account Holder:</strong> Mindbowser Infosolutions Pvt Ltd</p>
                                        <p><strong>Account Number:</strong> 098505008414</p>
                                        <p><strong>Bank Name:</strong> ICICI BANK LTD.</p>
                                        <p><strong>IFSC Code:</strong> ICIC0000985</p>
                                        <p><strong>SWIFT Code:</strong> ICICINBBCTS</p>
                                        <p><strong>MICR Code:</strong> 411229015</p>
                                        <p><strong>Branch:</strong> Baner</p>
                                    </div>
                                    <div class="bank-column">
                                        <p><strong>Province:</strong> Maharashtra</p>
                                        <p><strong>City:</strong> Pune</p>
                                        <p><strong>Country:</strong> India</p>
                                        <p><strong>Bank Address:</strong> ICICI BANK LTD., GROUND FLOOR, SHOP NO. O. ATRIA, SAL PRESTIGE, BANER ROAD, PUNE. 411045</p>
                                        <p><strong>PAN:</strong> AAHCM8155A</p>
                                        <p><strong>GST:</strong> 27AAHCM8155A1ZJ</p>
                                    </div>
                                </div>
                            </div>
                            <div class="terms">
                                <h3>Terms & Conditions:</h3>
                                <p>All wire transfers should be done in the mentioned currency on the invoice (e.g., USD). INR transfers can lead to extra taxes like GST, etc. Payment made means that service has been delivered successfully as payment is post-delivery. In case of dispute, kindly notify within 7 days of delivery. There shall be no refunds after 7 days of payment. All wire fees will be borne by the customer.</p>
                            </div>
                        </body>
                        </html>
                        """;
    }
}
