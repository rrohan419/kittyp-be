package com.kittyp.email.emailsender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.service.ClinicEmailUsageService;

class ZeptoMailSenderTest {
    @Test
    void blocksProviderCallWhenPracticeHasNoEmailCreditsLeft() {
        RestClient restClient = mock(RestClient.class);
        Environment environment = mock(Environment.class);
        ClinicEmailUsageService usageService = mock(ClinicEmailUsageService.class);
        when(usageService.reserve(9L, "owner@example.com", "invoice"))
                .thenReturn(new ClinicEmailUsageService.Reservation(false, null));
        ZeptoMailSender sender = new ZeptoMailSender(restClient, environment, usageService);
        ZeptoMailDto email = new ZeptoMailDto();
        email.setClinicAccountId(9L);
        email.setRecipientEmail("owner@example.com");
        email.setType("invoice");

        CustomException exception = assertThrows(CustomException.class, () -> sender.sendEmail(email));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        verify(usageService).reserve(9L, "owner@example.com", "invoice");
        verifyNoInteractions(restClient);
    }
}
