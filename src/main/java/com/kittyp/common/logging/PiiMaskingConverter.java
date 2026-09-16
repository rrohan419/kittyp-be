package com.kittyp.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PiiMaskingConverter extends ClassicConverter {

	@Override
	public String convert(ILoggingEvent event) {
		if (event == null) {
			return "";
		}
		return PiiMasker.mask(event.getFormattedMessage());
	}
}
