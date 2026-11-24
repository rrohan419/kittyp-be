package com.kittyp.notification.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.notification.model.MessagingDtos.ProviderStatusCallback;
import com.kittyp.notification.service.MessagingLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL + ApiUrl.WEBHOOK_MESSAGING_STATUS)
@RequiredArgsConstructor
public class MessagingStatusWebhookController {
    private final Environment environment;
    private final MessagingLifecycleService lifecycle;

    @PostMapping
    public void status(@RequestHeader(value="X-Kittyp-Webhook-Token",required=false) String supplied,
            @Valid @RequestBody ProviderStatusCallback callback) {
        String configured=environment.getProperty("messaging.webhook.token","");
        if(configured.isBlank()||supplied==null||!MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
            throw new CustomException("Invalid messaging webhook credential",HttpStatus.UNAUTHORIZED);
        String normalized=callback.status().trim().toUpperCase(java.util.Locale.ROOT);
        if(!java.util.Set.of("DELIVERED","READ","FAILED").contains(normalized))
            throw new CustomException("Unsupported messaging delivery status",HttpStatus.BAD_REQUEST);
        lifecycle.applyDeliveryStatus(callback.providerMessageId(),normalized);
    }
}
