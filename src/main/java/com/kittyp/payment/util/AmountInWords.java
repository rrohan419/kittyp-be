package com.kittyp.payment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indian-style amount in words for invoice PDFs (rupees and paise).
 */
public final class AmountInWords {

	private static final String[] UNITS = {
			"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
			"Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
			"Eighteen", "Nineteen"
	};
	private static final String[] TENS = {
			"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
	};

	private AmountInWords() {
	}

	public static String ofInr(BigDecimal amount) {
		if (amount == null) {
			return "Rupees Zero Only";
		}
		BigDecimal scaled = amount.abs().setScale(2, RoundingMode.HALF_UP);
		long rupees = scaled.longValue();
		int paise = scaled.remainder(BigDecimal.ONE).movePointRight(2).intValue();
		StringBuilder out = new StringBuilder("Rupees ");
		out.append(rupees == 0 ? "Zero" : convert(rupees));
		if (paise > 0) {
			out.append(" and ").append(convert(paise)).append(" Paise");
		}
		out.append(" Only");
		return out.toString();
	}

	private static String convert(long n) {
		if (n < 20) {
			return UNITS[(int) n];
		}
		if (n < 100) {
			return TENS[(int) (n / 10)] + (n % 10 == 0 ? "" : " " + UNITS[(int) (n % 10)]);
		}
		if (n < 1000) {
			return UNITS[(int) (n / 100)] + " Hundred" + (n % 100 == 0 ? "" : " " + convert(n % 100));
		}
		if (n < 100_000) {
			return convert(n / 1000) + " Thousand" + (n % 1000 == 0 ? "" : " " + convert(n % 1000));
		}
		if (n < 10_000_000) {
			return convert(n / 100_000) + " Lakh" + (n % 100_000 == 0 ? "" : " " + convert(n % 100_000));
		}
		return convert(n / 10_000_000) + " Crore" + (n % 10_000_000 == 0 ? "" : " " + convert(n % 10_000_000));
	}
}
