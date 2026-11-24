package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.notification.entity.*;
import com.kittyp.notification.enums.*;
import com.kittyp.notification.model.MessagingDtos.CreateMessageRequest;
import com.kittyp.notification.repository.*;

@ExtendWith(MockitoExtension.class)
class MessagingLifecycleServiceTest {
    @Mock PracticeMessageRepository messages;
    @Mock MessageSettingsRepository settings;
    @Mock MessageWalletRepository wallets;
    @Mock MessageLedgerRepository ledger;
    @Mock MessageRateCardRepository rateRepository;
    @Mock MessagePurchaseRepository purchases;
    @Mock MessageAuditRepository audits;
    @Mock MessagingRateCardService rateCards;
    @Mock MessagingTemplateCatalogService templateCatalog;
    @Mock ObjectMapper objectMapper;
    @Mock org.springframework.core.env.Environment environment;
    @Mock com.kittyp.payment.service.RazorpayGateway razorpay;
    @InjectMocks MessagingLifecycleService service;
    MessageWallet wallet;
    MessageRateCard smsRate;
    PracticeMessage lastMessage;

    @BeforeEach void setUp() {
        org.mockito.Mockito.lenient().when(messages.save(any())).thenAnswer(i->i.getArgument(0));
        org.mockito.Mockito.lenient().when(wallets.save(any())).thenAnswer(i->i.getArgument(0));
        org.mockito.Mockito.lenient().when(ledger.save(any())).thenAnswer(i->i.getArgument(0));
        org.mockito.Mockito.lenient().when(audits.save(any())).thenAnswer(i->i.getArgument(0));
        wallet=new MessageWallet();wallet.setTenantType(MessageTenantType.DOCTOR);wallet.setTenantId(22L);
        wallet.setChannel(MessageChannel.SMS);wallet.setAvailableCredits(5L);wallet.setReservedCredits(0L);
        smsRate=new MessageRateCard();smsRate.setChannel(MessageChannel.SMS);
        smsRate.setUnitRate(new BigDecimal("0.35"));smsRate.setIncludedMonthly(0);
        smsRate.setSegmentSize(160);smsRate.setPackCredits(1000);smsRate.setPackPriceExTax(new BigDecimal("350"));
        org.mockito.Mockito.lenient().when(rateCards.get(MessageChannel.SMS)).thenReturn(smsRate);
        org.mockito.Mockito.lenient().when(templateCatalog.require(eq(MessageChannel.SMS),anyString()))
                .thenReturn(new com.kittyp.notification.model.MessagingDtos.ApprovedTemplate(
                        "approved_sms","Approved SMS","SMS","test","{{code}}",java.util.List.of("code"),"en"));
        org.mockito.Mockito.lenient().when(wallets.findByTenantTypeAndTenantIdAndChannel(MessageTenantType.DOCTOR,22L,MessageChannel.SMS))
                .thenAnswer(i->Optional.of(wallet));
        org.mockito.Mockito.lenient().when(wallets.lockWallet(MessageTenantType.DOCTOR,22L,MessageChannel.SMS)).thenAnswer(i->Optional.of(wallet));
        org.mockito.Mockito.lenient().when(ledger.findByTenantTypeAndTenantIdAndChannelAndIdempotencyKey(any(),anyLong(),any(),anyString()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(purchases.findByRazorpayPaymentId(anyString())).thenReturn(Optional.empty());
    }

    @Test void reserveThenSettleConsumesSegmentCreditsAndReleasesReservation() {
        PracticeMessage msg=createWithLength(161);
        assertEquals(2,msg.getReservedCredits());
        assertEquals(3L,wallet.getAvailableCredits());
        assertEquals(2L,wallet.getReservedCredits());

        msg.setStatus(MessageStatus.SENDING);
        when(messages.findById(101L)).thenReturn(Optional.of(msg));
        service.markSent(101L,"provider-1");

        assertEquals(MessageStatus.SENT,msg.getStatus());
        assertEquals(3L,wallet.getAvailableCredits());
        assertEquals(0L,wallet.getReservedCredits());
        verify(ledger).save(argThat(entry->"SETTLE".equals(entry.getEntryType())));
    }

    @Test void cancellingQueuedMessageReleasesItsCreditsAndCannotBeDispatchedAsQueued() {
        PracticeMessage msg=createWithLength(2);
        when(messages.lockByUuidAndTenant("public-1",MessageTenantType.DOCTOR,22L))
                .thenReturn(Optional.of(msg));
        var result=service.action(MessageTenantType.DOCTOR,22L,"public-1","cancel","doctor@example.test");
        assertEquals(MessageStatus.CANCELLED.name(),result.status());
        assertEquals(5L,wallet.getAvailableCredits());
        assertEquals(0L,wallet.getReservedCredits());
        verify(ledger).save(argThat(entry->"RELEASE".equals(entry.getEntryType())&&entry.getCreditDelta()==1));
    }

    @Test void verifiedPaymentWebhookCreditsOnlyOnceForRepeatedPayment() {
        MessagePurchase purchase=new MessagePurchase();purchase.setUuid("purchase-1");
        purchase.setTenantType(MessageTenantType.DOCTOR);purchase.setTenantId(22L);
        purchase.setChannel(MessageChannel.SMS);purchase.setCredits(1000);
        purchase.setAmountExTax(new BigDecimal("350.00"));purchase.setTaxAmount(new BigDecimal("63.00"));
        when(purchases.lockByRazorpayOrderId("order-1")).thenReturn(Optional.of(purchase));
        assertTrue(service.creditVerifiedWebhook("order-1","pay-1"));
        assertTrue(service.creditVerifiedWebhook("order-1","pay-1"));
        assertEquals(1005L,wallet.getAvailableCredits());
        verify(ledger,times(1)).save(argThat((MessageLedgerEntry entry)->"PURCHASE".equals(entry.getEntryType())));
    }

    @Test void deliveryStatusesAdvanceOnlyFromValidPriorStates() {
        PracticeMessage msg=new PracticeMessage();msg.setUuid("m-2");msg.setTenantType(MessageTenantType.DOCTOR);
        msg.setTenantId(22L);msg.setStatus(MessageStatus.QUEUED);
        when(messages.findByProviderMessageId("wamid-2")).thenReturn(Optional.of(msg));
        service.applyDeliveryStatus("wamid-2","DELIVERED");
        assertEquals(MessageStatus.QUEUED,msg.getStatus());
        msg.setStatus(MessageStatus.SENT);
        service.applyDeliveryStatus("wamid-2","DELIVERED");
        assertEquals(MessageStatus.DELIVERED,msg.getStatus());
        service.applyDeliveryStatus("wamid-2","READ");
        assertEquals(MessageStatus.READ,msg.getStatus());
    }

    @Test void messageLookupIsScopedToTenantTypeAndTenantId() {
        when(messages.findByUuidAndTenantTypeAndTenantId("other-tenant-message",
                MessageTenantType.CLINIC,999L)).thenReturn(Optional.empty());
        assertThrows(com.kittyp.common.exception.CustomException.class,()->service.get(
                MessageTenantType.CLINIC,999L,"other-tenant-message"));
        verify(messages).findByUuidAndTenantTypeAndTenantId("other-tenant-message",
                MessageTenantType.CLINIC,999L);
    }

    @Test void batchUsesChannelSpecificRecipientsAndStoresRenderedTemplateSnapshot() {
        MessageWallet emailWallet=new MessageWallet();
        emailWallet.setTenantType(MessageTenantType.DOCTOR);emailWallet.setTenantId(22L);
        emailWallet.setChannel(MessageChannel.EMAIL);emailWallet.setAvailableCredits(5L);emailWallet.setReservedCredits(0L);
        MessageRateCard emailRate=new MessageRateCard();emailRate.setChannel(MessageChannel.EMAIL);
        emailRate.setIncludedMonthly(0);emailRate.setSegmentSize(1);emailRate.setPackCredits(1000);
        emailRate.setPackPriceExTax(new BigDecimal("50"));
        when(rateCards.get(MessageChannel.EMAIL)).thenReturn(emailRate);
        when(wallets.findByTenantTypeAndTenantIdAndChannel(MessageTenantType.DOCTOR,22L,MessageChannel.EMAIL))
                .thenReturn(Optional.of(emailWallet));
        when(wallets.lockWallet(MessageTenantType.DOCTOR,22L,MessageChannel.EMAIL)).thenReturn(Optional.of(emailWallet));
        int[] nextId={200};
        when(messages.saveAndFlush(any())).thenAnswer(invocation->{
            PracticeMessage message=invocation.getArgument(0);
            message.setId((long)nextId[0]);message.setUuid("batch-"+nextId[0]++);
            return message;
        });

        var request=new CreateMessageRequest(null,"owner@example.test","approved_sms",Map.of("code","42"),
                "Appointment reminder","Email copy",null,null,"Milo",
                java.util.List.of(MessageChannel.EMAIL,MessageChannel.SMS),
                Map.of("SMS","approved_sms"),Map.of("code","42"),"Email copy",null,null,
                "Rahul","owner@example.test","+919876543210");
        var result=service.createBatch(MessageTenantType.DOCTOR,22L,request,"doctor@example.test");

        var email=result.stream().filter(message->message.channel()==MessageChannel.EMAIL).findFirst().orElseThrow();
        var sms=result.stream().filter(message->message.channel()==MessageChannel.SMS).findFirst().orElseThrow();
        assertEquals("owner@example.test",email.recipient());
        assertEquals("Rahul",email.recipientName());
        assertEquals("+919876543210",sms.recipient());
        assertEquals("Rahul",sms.recipientName());
        assertEquals("42",sms.content());
        assertEquals("Email copy",email.content());
    }

    private PracticeMessage createWithLength(int length) {
        when(messages.saveAndFlush(any())).thenAnswer(i->{
            PracticeMessage m=i.getArgument(0);m.setId(101L);m.setUuid("public-1");
            lastMessage=m;
            return m;
        });
        service.create(MessageTenantType.DOCTOR,22L,new CreateMessageRequest(MessageChannel.SMS,
                "9876543210","approved_sms",Map.of("code","x".repeat(length)),null,null,null,null),
                "doctor@example.test");
        return lastMessage;
    }
}
