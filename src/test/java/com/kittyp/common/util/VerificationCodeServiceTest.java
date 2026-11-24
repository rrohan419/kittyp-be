package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

class VerificationCodeServiceTest {

    @Test
    void enforcesThirtySecondResendCooldown() {
        VerificationCodeService service = new VerificationCodeService();
        service.generateCode("profile:phone");

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.generateCode("profile:phone"));

        assertTrue(exception.getMessage().contains("seconds"));
        assertTrue(exception.getHttpStatus() == HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void cooldownIsIsolatedByOtpTarget() {
        VerificationCodeService service = new VerificationCodeService();
        service.generateCode("profile:phone-one");
        service.generateCode("profile:phone-two");
    }
}
