package com.kittyp.notification.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageStatus;
import jakarta.validation.constraints.*;

public final class MessagingDtos {
    private MessagingDtos() {}
    public record CreateMessageRequest(MessageChannel channel,
            @NotBlank @Size(max=512) String recipient,
            @Size(max=120) String templateKey,
            @Size(max=100) Map<String,Object> variables,
            @Size(max=200) String subject,
            @Size(max=100000) String emailBody,
            LocalDateTime scheduledAt,
            @Size(max=100) String idempotencyKey,
            String petName,
            List<MessageChannel> channels,
            @Size(max=10) Map<String,String> templateIds,
            @Size(max=100) Map<String,Object> templateVariables,
            @Size(max=100000) String emailContent,
            LocalDateTime schedule,
            String content,
            @Size(max=160) String recipientName,
            @Email @Size(max=320) String recipientEmail,
            @Size(max=32) String recipientPhone) {
        public CreateMessageRequest(MessageChannel channel,String recipient,String templateKey,Map<String,Object> variables,
                String subject,String emailBody,LocalDateTime scheduledAt,String idempotencyKey) {
            this(channel,recipient,templateKey,variables,subject,emailBody,scheduledAt,idempotencyKey,
                    null,null,null,null,null,null,null,null,null,null);
        }
        public Map<String,Object> effectiveVariables() {
            java.util.LinkedHashMap<String,Object> result=new java.util.LinkedHashMap<>();
            if(variables!=null) result.putAll(variables);
            if(templateVariables!=null) result.putAll(templateVariables);
            if(petName!=null) result.put("petName",petName);
            return result;
        }
        public LocalDateTime effectiveSchedule() { return schedule!=null?schedule:scheduledAt; }
        public String effectiveEmailBody() {
            return emailContent!=null?emailContent:(content!=null?content:emailBody);
        }
        public List<MessageChannel> effectiveChannels() {
            if(channels!=null&&!channels.isEmpty()) return channels;
            return channel==null?List.of():List.of(channel);
        }
        public String templateFor(MessageChannel c) {
            if(templateIds!=null) {
                String exact=templateIds.get(c.name());
                if(exact!=null)return exact;
                for(Map.Entry<String,String> entry:templateIds.entrySet())
                    if(entry.getKey()!=null&&entry.getKey().equalsIgnoreCase(c.name())) return entry.getValue();
            }
            return templateKey;
        }
        public String effectiveRecipient(MessageChannel c) {
            String channelRecipient=c==MessageChannel.EMAIL?recipientEmail:recipientPhone;
            return channelRecipient==null||channelRecipient.isBlank()?recipient:channelRecipient;
        }
    }
    public record PatchMessageRequest(LocalDateTime scheduledAt, @Size(max=120) String templateKey,
            Map<String,Object> variables, @Size(max=200) String subject,
            @Size(max=100000) String emailBody,LocalDateTime schedule,
            Map<String,Object> templateVariables,String templateId,String emailContent,
            @Size(max=512) String recipient,@Size(max=160) String recipientName,
            @Email @Size(max=320) String recipientEmail,@Size(max=32) String recipientPhone) {
        public PatchMessageRequest(LocalDateTime scheduledAt,String templateKey,Map<String,Object> variables,
                String subject,String emailBody) {
            this(scheduledAt,templateKey,variables,subject,emailBody,null,null,null,null,null,null,null,null);
        }
        public LocalDateTime effectiveSchedule(){return schedule==null?scheduledAt:schedule;}
        public Map<String,Object> effectiveVariables(){
            java.util.LinkedHashMap<String,Object> values=new java.util.LinkedHashMap<>();
            if(variables!=null)values.putAll(variables);if(templateVariables!=null)values.putAll(templateVariables);
            return variables==null&&templateVariables==null?null:values;
        }
        public String effectiveTemplate(){return templateId==null?templateKey:templateId;}
        public String effectiveEmailBody(){return emailContent==null?emailBody:emailContent;}
        public String effectiveRecipient(MessageChannel channel) {
            String channelRecipient=channel==MessageChannel.EMAIL?recipientEmail:recipientPhone;
            return channelRecipient==null||channelRecipient.isBlank()?recipient:channelRecipient;
        }
    }
    public record MessageModel(String uuid,String recipient,String recipientName,String petName,String status,List<MessageChannel> channels,
            LocalDateTime schedule,LocalDateTime sendDate,Map<String,Object> channelContents,String content,
            MessageChannel channel,MessageStatus messageStatus,String subject,String templateKey,
            Map<String,Object> variables,LocalDateTime scheduledAt,String providerMessageId,String failureCode,
            int attempts,LocalDateTime createdAt,String automationId,String sourceType,String sourceUuid) {}
    public record MessageList(List<MessageModel> items) {}
    public record ApprovedTemplate(String id,String name,String channel,String purpose,String body,List<String> variables,
            String language,boolean requiresDocument) {
        public ApprovedTemplate(String id,String name,String channel,String purpose,String body,List<String> variables,
                String language) {
            this(id,name,channel,purpose,body,variables,language,false);
        }
    }
    public record ChannelRateOverview(long balance,BigDecimal creditsPerMessage,BigDecimal unitRate,
            int includedMonthly,int packCredits,BigDecimal packPriceExTax,BigDecimal taxPercent) {}
    public record Overview(boolean globallyPaused,Map<String,ChannelRateOverview> channelRates,
            List<ApprovedTemplate> approvedTemplates,long queued,long scheduled,long sent,long delivered,
            long failed,long cancelled,boolean paused) {}
    public record SettingsModel(boolean paused,boolean globallyPaused) {
        public SettingsModel(boolean paused){this(paused,paused);}
    }
    public record SettingsRequest(boolean paused,Boolean globallyPaused) {
        public SettingsRequest(boolean paused){this(paused,null);}
        public boolean effectivePaused(){return globallyPaused==null?paused:globallyPaused;}
    }
    public record AutomationItem(@NotBlank @Size(max=100) String id,boolean enabled) {}
    public record AutomationModel(List<AutomationItem> automations) {
        public AutomationModel(Map<String,Boolean> enabled) {
            this(enabled==null?List.of():enabled.entrySet().stream()
                    .map(e->new AutomationItem(e.getKey(),Boolean.TRUE.equals(e.getValue()))).toList());
        }
        public Map<String,Boolean> enabled(){return automations.stream().collect(java.util.stream.Collectors.toMap(
                AutomationItem::id,AutomationItem::enabled,(a,b)->b,java.util.LinkedHashMap::new));}
    }
    public record AutomationRequest(List<AutomationItem> automations) {
        public AutomationRequest(Map<String,Boolean> enabled) {
            this(enabled==null?List.of():enabled.entrySet().stream().map(e->new AutomationItem(e.getKey(),e.getValue())).toList());
        }
        public Map<String,Boolean> enabled(){return automations==null?Map.of():automations.stream().collect(
                java.util.stream.Collectors.toMap(AutomationItem::id,AutomationItem::enabled,(a,b)->b,java.util.LinkedHashMap::new));}
    }
    public record WalletModel(MessageChannel channel,long availableCredits,long reservedCredits,
            BigDecimal exTaxUnitRate,int includedMonthly,int packCredits,BigDecimal packPriceExTax,BigDecimal taxPercent) {}
    public record PurchaseOption(String bundleId,MessageChannel channel,int credits,BigDecimal amountExTax,
            BigDecimal taxAmount,BigDecimal totalAmount,String currency) {}
    public record CreditsModel(Map<String,Long> balance,Map<String,Long> usedThisPeriod,
            List<LedgerModel> transactions,List<PurchaseOption> purchaseOptions,List<WalletModel> wallets,
            List<LedgerModel> ledger) {}
    public record LedgerModel(String channel,String entryType,long creditDelta,String referenceId,
            BigDecimal amountExTax,BigDecimal taxAmount,LocalDateTime createdAt) {}
    public record AuditModel(String messageUuid,String actorEmail,String action,String detail,LocalDateTime createdAt) {}
    public record PurchaseRequest(String bundleId,MessageChannel channel) {
        public PurchaseRequest(MessageChannel channel){this(null,channel);}
        public MessageChannel effectiveChannel() {
            if(channel!=null) return channel;
            if(bundleId==null) return null;
            String value=bundleId.split("_",2)[0].toUpperCase(java.util.Locale.ROOT);
            try{return MessageChannel.valueOf(value);}catch(Exception e){return null;}
        }
    }
    public record PurchaseModel(String purchaseUuid,String bundleId,String razorpayOrderId,int credits,
            BigDecimal amountExTax,BigDecimal taxAmount,BigDecimal totalAmount,String currency,String status,String key) {}
    public record VerifyPurchaseRequest(String orderId,String paymentId,String signature,
            String razorpayOrderId,String razorpayPaymentId,String razorpaySignature) {
        public String effectiveOrderId(){return orderId==null?razorpayOrderId:orderId;}
        public String effectivePaymentId(){return paymentId==null?razorpayPaymentId:paymentId;}
        public String effectiveSignature(){return signature==null?razorpaySignature:signature;}
    }
    public record RateCardModel(MessageChannel channel,BigDecimal unitRate,int includedMonthly,int segmentSize,
            int packCredits,BigDecimal packPriceExTax,BigDecimal taxPercent) {}
    public record RateCardsRequest(@NotNull List<@NotNull RateCardModel> rates) {}
    public record ProviderStatusCallback(@NotBlank String providerMessageId,@NotBlank String status) {}
}
