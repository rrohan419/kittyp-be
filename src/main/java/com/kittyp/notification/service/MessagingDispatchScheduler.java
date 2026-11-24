package com.kittyp.notification.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class MessagingDispatchScheduler {
    private final MessagingDispatchCoordinator coordinator;
    @Scheduled(fixedDelayString="${messaging.dispatch.delay-ms:10000}")
    public void dispatchDue() { coordinator.dispatchDue(); }
}
