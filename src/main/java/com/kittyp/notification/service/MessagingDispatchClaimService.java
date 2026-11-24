package com.kittyp.notification.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.kittyp.notification.repository.PracticeMessageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingDispatchClaimService {
    private final PracticeMessageRepository messages;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long id) {
        return messages.claim(id, LocalDateTime.now()) == 1;
    }
}
