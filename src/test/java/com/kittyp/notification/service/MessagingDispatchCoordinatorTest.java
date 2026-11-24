package com.kittyp.notification.service;

import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.repository.PracticeMessageRepository;

@ExtendWith(MockitoExtension.class)
class MessagingDispatchCoordinatorTest {
    @Mock PracticeMessageRepository messages;
    @Mock MessagingDispatchClaimService claimService;
    @Mock MessagingLifecycleService lifecycle;
    @Mock PracticeMessageDelivery delivery;
    @InjectMocks MessagingDispatchCoordinator coordinator;

    @Test void atomicClaimLoserNeverCallsProvider() {
        when(claimService.claim(anyLong())).thenReturn(false);
        coordinator.dispatch(8L);
        verifyNoInteractions(delivery,lifecycle);
        verify(messages,never()).findById(anyLong());
    }

    @Test void emailCreditExhaustionIsRecordedWithSpecificFailureCode() {
        when(claimService.claim(8L)).thenReturn(true);
        when(messages.findById(8L)).thenReturn(Optional.of(new PracticeMessage()));
        when(delivery.deliver(any())).thenThrow(
                new CustomException("Monthly email credits exhausted", HttpStatus.TOO_MANY_REQUESTS));

        coordinator.dispatch(8L);

        verify(lifecycle).markFailed(8L, "EMAIL_CREDITS_EXHAUSTED");
    }
}
