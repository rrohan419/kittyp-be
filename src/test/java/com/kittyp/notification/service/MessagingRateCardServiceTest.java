package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import com.kittyp.notification.entity.MessageRateCard;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.repository.MessageRateCardRepository;

class MessagingRateCardServiceTest {
    @Test
    void seedsEditableInitialRatesOnceAndDoesNotOverwriteAdminValues() throws Exception {
        MessageRateCardRepository repository=mock(MessageRateCardRepository.class);
        when(repository.findByChannel(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation->invocation.getArgument(0));
        MessagingRateCardService service=new MessagingRateCardService(repository);
        service.run(mock(ApplicationArguments.class));
        verify(repository,times(3)).save(any(MessageRateCard.class));
        verify(repository).save(argThat(card -> card.getChannel()==MessageChannel.SMS
                && card.getUnitRate().toPlainString().equals("0.35")
                && card.getPackCredits()==1000&&card.getPackPriceExTax().toPlainString().equals("350.00")));
        reset(repository);
        when(repository.findByChannel(any())).thenReturn(Optional.of(new MessageRateCard()));
        service.run(mock(ApplicationArguments.class));
        verify(repository,never()).save(any());
    }
}
