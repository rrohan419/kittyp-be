package com.kittyp.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.repository.PracticeMessageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingDispatchCoordinator {
    private final PracticeMessageRepository messages;
    private final MessagingDispatchClaimService claimService;
    private final MessagingLifecycleService lifecycle;
    private final PracticeMessageDelivery delivery;

    @Transactional(readOnly=true)
    public PracticeMessage fetchForDelivery(Long id) {
        return messages.findById(id).orElse(null);
    }
    public void dispatch(Long id) {
        if(!claimService.claim(id)) return;
        PracticeMessage message=fetchForDelivery(id);
        if(message==null) return;
        try {
            String providerId=delivery.deliver(message); // no DB transaction is open during provider HTTP call
            lifecycle.markSent(id,providerId);
        } catch(Exception e) {
            lifecycle.markFailed(id,safeFailure(e));
        }
    }
    public void dispatchDue() {
        List<PracticeMessage> due=messages
                .findTop100ByAutomationIdIsNullAndStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                List.of(MessageStatus.QUEUED,MessageStatus.SCHEDULED),LocalDateTime.now());
        for(PracticeMessage m:due) dispatch(m.getId());
    }
    private String safeFailure(Exception e) {
        // Store only a stable safe code, never provider response/body, recipient or message data.
        if(e instanceof CustomException custom && custom.getHttpStatus()==HttpStatus.TOO_MANY_REQUESTS)
            return "EMAIL_CREDITS_EXHAUSTED";
        if(e instanceof org.springframework.web.client.RestClientResponseException) return "PROVIDER_REJECTED";
        String s=e.getMessage()==null?"delivery_failed":e.getMessage().toLowerCase(java.util.Locale.ROOT);
        if(s.contains("not configured")||s.contains("must be configured")||s.contains("credentials"))
            return "CONFIGURATION_MISSING";
        if(s.contains("approved template")||s.contains("template is not configured")) return "TEMPLATE_MISSING";
        if(s.contains("recipient")) return "INVALID_RECIPIENT";
        if(s.contains("rejected")||s.contains("http ")) return "PROVIDER_REJECTED";
        return "DELIVERY_FAILED";
    }
}
