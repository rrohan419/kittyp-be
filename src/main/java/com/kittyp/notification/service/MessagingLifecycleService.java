package com.kittyp.notification.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.*;
import com.kittyp.notification.enums.*;
import com.kittyp.notification.model.MessagingDtos.*;
import com.kittyp.notification.repository.*;
import com.kittyp.payment.constants.RazorPayConstant;
import com.kittyp.payment.model.CreateOrderModel;
import com.kittyp.payment.service.RazorpayGateway;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingLifecycleService {
    private final PracticeMessageRepository messages;
    private final MessageSettingsRepository settingsRepository;
    private final MessageWalletRepository walletRepository;
    private final MessageLedgerRepository ledgerRepository;
    private final MessagePurchaseRepository purchaseRepository;
    private final MessageAuditRepository auditRepository;
    private final MessagingRateCardService rateCards;
    private final MessagingTemplateCatalogService templateCatalog;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final RazorpayGateway razorpayGateway;

    @Transactional
    public MessageModel create(MessageTenantType type,Long tenantId,CreateMessageRequest request,String actor) {
        MessageChannel channel=request.channel();
        if(channel==null && request.effectiveChannels().size()==1) channel=request.effectiveChannels().get(0);
        if(channel==null) throw new CustomException("Message channel is required",HttpStatus.BAD_REQUEST);
        String templateKey=request.templateFor(channel);
        String emailBody=request.effectiveEmailBody();
        Map<String,Object> variables=request.effectiveVariables();
        LocalDateTime requestedSchedule=request.effectiveSchedule();
        String recipient=request.effectiveRecipient(channel);
        validateRecipient(channel,recipient);
        validateVariables(variables);
        validateTemplateRequest(channel,templateKey,emailBody);
        if(channel==MessageChannel.EMAIL&&StringUtils.hasText(emailBody)&&!StringUtils.hasText(request.subject()))
            throw new CustomException("Email subject is required when sending custom email content",HttpStatus.BAD_REQUEST);
        if(StringUtils.hasText(request.idempotencyKey())) {
            String idempotency=request.idempotencyKey();
            if(request.effectiveChannels().size()>1) idempotency += ":"+channel.name();
            var old=messages.findByTenantTypeAndTenantIdAndIdempotencyKey(type,tenantId,idempotency);
            if(old.isPresent()) return model(old.get());
        }
        LocalDateTime now=LocalDateTime.now();
        PracticeMessage m=new PracticeMessage();
        m.setTenantType(type);m.setTenantId(tenantId);m.setChannel(channel);m.setRecipient(recipient.trim());
        m.setRecipientName(request.recipientName());
        m.setPetName(request.petName());
        m.setTemplateKey(templateKey);m.setSubject(request.subject());m.setEmailBody(emailBody);
        m.setScheduledAt(requestedSchedule==null?now:requestedSchedule);
        m.setStatus(m.getScheduledAt().isAfter(now)?MessageStatus.SCHEDULED:MessageStatus.QUEUED);
        m.setIdempotencyKey(request.idempotencyKey()==null?null:
                request.idempotencyKey()+(request.effectiveChannels().size()>1?":"+channel.name():""));
        m.setTemplateVariables(toJson(variables));
        m.setMessageBodySnapshot(snapshotContent(m,variables));
        m.setAttempts(0);m.setReservedCredits(0);m.setFreeAllowanceReserved(false);
        m=messages.saveAndFlush(m);
        reserve(m, variables);
        audit(type,tenantId,m.getUuid(),actor,"CREATE",m.getStatus().name());
        return model(m);
    }

    @Transactional
    public List<MessageModel> createBatch(MessageTenantType type,Long tenantId,CreateMessageRequest request,String actor) {
        List<MessageChannel> channels=request.effectiveChannels().stream().distinct().toList();
        if(channels.isEmpty()) throw new CustomException("At least one message channel is required",HttpStatus.BAD_REQUEST);
        if(channels.size()>MessageChannel.values().length||channels.stream().anyMatch(java.util.Objects::isNull))
            throw new CustomException("Invalid message channels",HttpStatus.BAD_REQUEST);
        if(channels.size()>1&&request.idempotencyKey()!=null&&request.idempotencyKey().length()>90)
            throw new CustomException("Idempotency key is too long for a multi-channel request",HttpStatus.BAD_REQUEST);
        List<MessageModel> result=new ArrayList<>();
        for(MessageChannel channel:channels) result.add(create(type,tenantId,requestWithChannel(request,channel),actor));
        return result;
    }

    private CreateMessageRequest requestWithChannel(CreateMessageRequest r,MessageChannel channel) {
        String key=r.idempotencyKey();
        if(key!=null&&r.effectiveChannels().stream().distinct().count()>1) key += ":"+channel.name();
        boolean email=channel==MessageChannel.EMAIL;
        return new CreateMessageRequest(channel,r.effectiveRecipient(channel),r.templateFor(channel),r.effectiveVariables(),
                email?r.subject():null,email?r.effectiveEmailBody():null,r.effectiveSchedule(),key,r.petName(),List.of(channel),
                r.templateIds(),r.templateVariables(),email?r.emailContent():null,r.schedule(),email?r.content():null,
                r.recipientName(),r.recipientEmail(),r.recipientPhone());
    }

    @Transactional(readOnly=true)
    public List<MessageModel> list(MessageTenantType type,Long tenantId,String view) {
        List<PracticeMessage> found;
        if("upcoming".equalsIgnoreCase(view)) found=messages.findTop200ByTenantTypeAndTenantIdAndStatusInOrderByScheduledAtAsc(
                type,tenantId,List.of(MessageStatus.SCHEDULED,MessageStatus.QUEUED,MessageStatus.PAUSED));
        else found=messages.findTop200ByTenantTypeAndTenantIdOrderByScheduledAtDesc(type,tenantId);
        return found.stream().map(this::model).toList();
    }
    @Transactional(readOnly=true)
    public MessageModel get(MessageTenantType type,Long tenantId,String uuid) { return model(require(type,tenantId,uuid)); }

    @Transactional
    public MessageModel patch(MessageTenantType type,Long tenantId,String uuid,PatchMessageRequest patch,String actor) {
        PracticeMessage m=requireForUpdate(type,tenantId,uuid);
        if(m.getStatus()!=MessageStatus.SCHEDULED&&m.getStatus()!=MessageStatus.PAUSED)
            throw new CustomException("Only scheduled or paused messages can be edited",HttpStatus.CONFLICT);
        Map<String,Object> varsBefore=readVars(m);
        boolean keepFreeAllowance=Boolean.TRUE.equals(m.getFreeAllowanceReserved());
        release(m,"EDIT_RELEASE:"+uuid+":"+UUID.randomUUID());
        if(patch.effectiveTemplate()!=null) m.setTemplateKey(patch.effectiveTemplate());
        if(patch.effectiveVariables()!=null) {
            validateVariables(patch.effectiveVariables());
            m.setTemplateVariables(toJson(patch.effectiveVariables()));
        }
        if(patch.subject()!=null) m.setSubject(patch.subject());
        if(patch.effectiveEmailBody()!=null) m.setEmailBody(patch.effectiveEmailBody());
        if(patch.effectiveRecipient(m.getChannel())!=null) {
            validateRecipient(m.getChannel(),patch.effectiveRecipient(m.getChannel()));
            m.setRecipient(patch.effectiveRecipient(m.getChannel()).trim());
        }
        if(patch.recipientName()!=null) m.setRecipientName(patch.recipientName());
        if(patch.effectiveSchedule()!=null) {
            m.setScheduledAt(patch.effectiveSchedule());
            if(m.getStatus()!=MessageStatus.PAUSED) m.setStatus(patch.effectiveSchedule().isAfter(LocalDateTime.now())
                    ?MessageStatus.SCHEDULED:MessageStatus.QUEUED);
        }
        validateTemplateRequest(m.getChannel(),m.getTemplateKey(),m.getEmailBody());
        m.setMessageBodySnapshot(snapshotContent(m,patch.effectiveVariables()==null?varsBefore:patch.effectiveVariables()));
        m.setFreeAllowanceReserved(keepFreeAllowance);
        reserve(m,patch.effectiveVariables()==null?varsBefore:patch.effectiveVariables());
        audit(type,tenantId,uuid,actor,"EDIT","Message updated");
        return model(messages.save(m));
    }

    @Transactional
    public MessageModel action(MessageTenantType type,Long tenantId,String uuid,String action,String actor) {
        PracticeMessage m=requireForUpdate(type,tenantId,uuid);
        switch(action) {
            case "pause" -> {
                if(m.getStatus()!=MessageStatus.QUEUED&&m.getStatus()!=MessageStatus.SCHEDULED)
                    throw new CustomException("Only queued or scheduled messages can be paused",HttpStatus.CONFLICT);
                m.setStatus(MessageStatus.PAUSED);
            }
            case "resume" -> {
                if(m.getStatus()!=MessageStatus.PAUSED) throw new CustomException("Message is not paused",HttpStatus.CONFLICT);
                m.setStatus(m.getScheduledAt().isAfter(LocalDateTime.now())?MessageStatus.SCHEDULED:MessageStatus.QUEUED);
            }
            case "cancel" -> {
                if(m.getStatus()==MessageStatus.SENDING||isTerminal(m.getStatus()))
                    throw new CustomException("Message can no longer be cancelled",HttpStatus.CONFLICT);
                release(m,"CANCEL:"+uuid);
                m.setStatus(MessageStatus.CANCELLED);
            }
            case "retry" -> {
                if(m.getStatus()!=MessageStatus.FAILED)
                    throw new CustomException("Only failed messages can be retried",HttpStatus.CONFLICT);
                reserve(m,readVars(m));m.setFailureCode(null);m.setScheduledAt(LocalDateTime.now());m.setStatus(MessageStatus.QUEUED);
            }
            default -> throw new CustomException("Unsupported message action",HttpStatus.BAD_REQUEST);
        }
        audit(type,tenantId,uuid,actor,action.toUpperCase(),m.getStatus().name());
        return model(messages.save(m));
    }

    @Transactional
    public SettingsModel setPause(MessageTenantType type,Long tenantId,boolean paused,String actor) {
        MessageSettings s=settings(type,tenantId);
        s.setPaused(paused);settingsRepository.save(s);
        for(PracticeMessage message:messages.findTop200ByTenantTypeAndTenantIdOrderByScheduledAtDesc(type,tenantId)) {
            if(paused && (message.getStatus()==MessageStatus.SCHEDULED||message.getStatus()==MessageStatus.QUEUED))
                message.setStatus(MessageStatus.PAUSED);
            else if(!paused && message.getStatus()==MessageStatus.PAUSED
                    && message.getAutomationId()!=null) {
                if(message.getScheduledAt()!=null && message.getScheduledAt().isAfter(LocalDateTime.now()))
                    message.setStatus(MessageStatus.SCHEDULED);
                else message.setStatus(MessageStatus.QUEUED);
            }
            messages.save(message);
        }
        audit(type,tenantId,null,actor,paused?"PAUSE_ALL":"RESUME_ALL","Messaging dispatch setting changed");
        return new SettingsModel(paused,paused);
    }
    @Transactional(readOnly=true)
    public SettingsModel getSettings(MessageTenantType type,Long tenantId) {
        boolean paused=settingsRepository.findByTenantTypeAndTenantId(type,tenantId)
                .map(MessageSettings::getPaused).orElse(false);
        return new SettingsModel(paused,paused);
    }
    @Transactional
    public AutomationModel setAutomations(MessageTenantType type,Long tenantId,Map<String,Boolean> enabled,String actor) {
        if(enabled.size()>100||enabled.keySet().stream().anyMatch(k->k==null||!k.matches("[A-Za-z0-9_.-]{1,100}")))
            throw new CustomException("Invalid automation keys",HttpStatus.BAD_REQUEST);
        MessageSettings s=settings(type,tenantId);
        s.setAutomationsJson(toJson(enabled));settingsRepository.save(s);
        for(Map.Entry<String,Boolean> entry:enabled.entrySet()) {
            for(PracticeMessage message:messages.findByTenantTypeAndTenantIdAndAutomationId(type,tenantId,entry.getKey())) {
                if(Boolean.TRUE.equals(entry.getValue())) {
                    if(message.getStatus()==MessageStatus.PAUSED
                            && message.getScheduledAt()!=null && message.getScheduledAt().isAfter(LocalDateTime.now()))
                        message.setStatus(MessageStatus.SCHEDULED);
                    else if(message.getStatus()==MessageStatus.PAUSED) message.setStatus(MessageStatus.QUEUED);
                } else if(message.getStatus()==MessageStatus.SCHEDULED||message.getStatus()==MessageStatus.QUEUED) {
                    message.setStatus(MessageStatus.PAUSED);
                }
                messages.save(message);
            }
        }
        audit(type,tenantId,null,actor,"AUTOMATIONS_UPDATED","Automation preferences updated");
        return new AutomationModel(Map.copyOf(enabled));
    }
    @Transactional(readOnly=true)
    public AutomationModel getAutomations(MessageTenantType type,Long tenantId) {
        Map<String,Boolean> enabled=readAutomations(settingsRepository.findByTenantTypeAndTenantId(type,tenantId)
                .map(MessageSettings::getAutomationsJson).orElse("{}"));
        java.util.LinkedHashMap<String,Boolean> result=new java.util.LinkedHashMap<>();
        result.put("PET_REMINDER",enabled.getOrDefault("PET_REMINDER",true));
        result.put("VACCINE_REMINDER",enabled.getOrDefault("VACCINE_REMINDER",true));
        result.put("APPOINTMENT_REMINDER",enabled.getOrDefault("APPOINTMENT_REMINDER",true));
        result.put("APPOINTMENT_CONFIRMATION",enabled.getOrDefault("APPOINTMENT_CONFIRMATION",true));
        result.put("INVOICE_NOTIFICATION",enabled.getOrDefault("INVOICE_NOTIFICATION",true));
        result.put("FOLLOW_UP",enabled.getOrDefault("FOLLOW_UP",true));
        result.putAll(enabled);
        return new AutomationModel(result);
    }

    @Transactional
    public CreditsModel credits(MessageTenantType type,Long tenantId) {
        List<WalletModel> wallets=new ArrayList<>();
        for(MessageChannel ch:MessageChannel.values()) {
            MessageWallet wallet=getOrCreateWallet(type,tenantId,ch);
            MessageRateCard rate=rateCards.get(ch);
            wallets.add(new WalletModel(ch,wallet.getAvailableCredits(),wallet.getReservedCredits(),rate.getUnitRate(),
                    rate.getIncludedMonthly(),rate.getPackCredits(),rate.getPackPriceExTax(),rate.getTaxPercent()));
        }

        List<LedgerModel> ledger=ledgerRepository.findTop100ByTenantTypeAndTenantIdOrderByCreatedAtDesc(type,tenantId)
                .stream().map(l->new LedgerModel(l.getChannel().name(),l.getEntryType(),l.getCreditDelta(),
                        l.getReferenceId(),l.getAmountExTax(),l.getTaxAmount(),l.getCreatedAt())).toList();
        java.util.Map<String,Long> balance=new java.util.LinkedHashMap<>();
        java.util.Map<String,Long> used=new java.util.LinkedHashMap<>();
        List<PurchaseOption> options=new ArrayList<>();
        LocalDateTime monthStart=LocalDate.now().withDayOfMonth(1).atStartOfDay();
        for(MessageChannel channel:MessageChannel.values()) {
            MessageWallet wallet=walletRepository.findByTenantTypeAndTenantIdAndChannel(type,tenantId,channel).orElse(null);
            balance.put(channel.name(),wallet==null?0L:wallet.getAvailableCredits());
            long usedCount=messages.countByTenantTypeAndTenantIdAndChannelAndCreatedAtGreaterThanEqualAndStatusIn(
                    type,tenantId,channel,monthStart,List.of(MessageStatus.SENT,MessageStatus.DELIVERED,MessageStatus.READ));
            used.put(channel.name(),usedCount);
            MessageRateCard card=rateCards.get(channel);
            BigDecimal ex=card.getPackPriceExTax().setScale(2,RoundingMode.HALF_UP);
            BigDecimal tax=ex.multiply(card.getTaxPercent()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
            options.add(new PurchaseOption(channel.name()+"_"+card.getPackCredits(),channel,card.getPackCredits(),
                    ex,tax,ex.add(tax).setScale(2,RoundingMode.HALF_UP),"INR"));
        }
        return new CreditsModel(balance,used,ledger,options,wallets,ledger);
    }

    @Transactional(readOnly=true)
    public List<com.kittyp.notification.model.MessagingDtos.AuditModel> auditEvents(
            MessageTenantType type,Long tenantId) {
        return auditRepository.findTop100ByTenantTypeAndTenantIdOrderByCreatedAtDesc(type,tenantId).stream()
                .map(e->new com.kittyp.notification.model.MessagingDtos.AuditModel(
                        e.getMessageUuid(),e.getActorEmail(),e.getAction(),e.getSafeDetail(),e.getCreatedAt()))
                .toList();
    }

    public PurchaseModel createPurchase(MessageTenantType type,Long tenantId,MessageChannel channel) {
        return createPurchase(type,tenantId,channel,null);
    }

    @Transactional
    public PurchaseModel createPurchase(MessageTenantType type,Long tenantId,MessageChannel channel,String bundleId) {
        if(channel==null) throw new CustomException("A valid credit bundle is required",HttpStatus.BAD_REQUEST);
        String razorpayKeyId=environment.getProperty(RazorPayConstant.KEY_ID);
        String razorpayKeySecret=environment.getProperty(RazorPayConstant.KEY_SECRET);
        if(!StringUtils.hasText(razorpayKeyId)||!StringUtils.hasText(razorpayKeySecret))
            throw new CustomException("Razorpay is not configured for messaging purchases",
                    HttpStatus.SERVICE_UNAVAILABLE);
        MessageRateCard card=rateCards.get(channel);
        String activeBundle=channel.name()+"_"+card.getPackCredits();
        if(StringUtils.hasText(bundleId)&&!activeBundle.equalsIgnoreCase(bundleId))
            throw new CustomException("Credit bundle is no longer active; refresh purchase options",HttpStatus.CONFLICT);
        BigDecimal ex=card.getPackPriceExTax().setScale(2,RoundingMode.HALF_UP);
        BigDecimal tax=ex.multiply(card.getTaxPercent()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        BigDecimal total=ex.add(tax).setScale(2,RoundingMode.HALF_UP);
        String purchaseUuid=UUID.randomUUID().toString();
        String receipt=("MSG"+purchaseUuid.replace("-",""));
        JSONObject notes=new JSONObject(); notes.put("type","MESSAGING_CREDIT_PURCHASE");
        notes.put("purchase_uuid",purchaseUuid);notes.put("tenant_type",type.name());notes.put("tenant_id",tenantId);
        JSONObject order=new JSONObject();order.put(RazorPayConstant.AMOUNT,total.multiply(new BigDecimal("100")).intValueExact());
        order.put(RazorPayConstant.CURRENCY,"INR");order.put(RazorPayConstant.RECEIPT,receipt);order.put(RazorPayConstant.NOTES,notes);
        CreateOrderModel orderModel=razorpayGateway.createOrder(order);
        MessagePurchase purchase=new MessagePurchase();
        purchase.setUuid(purchaseUuid);purchase.setTenantType(type);purchase.setTenantId(tenantId);purchase.setChannel(channel);
        purchase.setCredits(card.getPackCredits());purchase.setAmountExTax(ex);purchase.setTaxAmount(tax);
        purchase.setTotalAmount(total);purchase.setRazorpayOrderId(orderModel.getId());purchase.setStatus("CREATED");
        purchaseRepository.save(purchase);
        return new PurchaseModel(purchaseUuid,channel.name()+"_"+card.getPackCredits(),orderModel.getId(),
                card.getPackCredits(),ex,tax,total,"INR","CREATED",razorpayKeyId);
    }

    @Transactional
    public PurchaseModel verifyPurchase(MessageTenantType type,Long tenantId,VerifyPurchaseRequest request,String actor) {
        if(!StringUtils.hasText(request.effectiveOrderId())||!StringUtils.hasText(request.effectivePaymentId())
                ||!StringUtils.hasText(request.effectiveSignature()))
            throw new CustomException("Razorpay order, payment and signature are required",HttpStatus.BAD_REQUEST);
        MessagePurchase p=purchaseRepository.lockByRazorpayOrderId(request.effectiveOrderId()).orElseThrow(()->
                new CustomException("Messaging purchase not found",HttpStatus.NOT_FOUND));
        if(p.getTenantType()!=type||!p.getTenantId().equals(tenantId))
            throw new CustomException("Messaging purchase does not belong to this account",HttpStatus.FORBIDDEN);
        if("PAID".equals(p.getStatus())) return purchaseModel(p);
        if(purchaseRepository.findByRazorpayPaymentId(request.effectivePaymentId()).isPresent())
            throw new CustomException("Payment already applied",HttpStatus.CONFLICT);
        try {
            JSONObject options=new JSONObject();options.put("razorpay_order_id",request.effectiveOrderId());
            options.put("razorpay_payment_id",request.effectivePaymentId());options.put("razorpay_signature",request.effectiveSignature());
            if(!Utils.verifyPaymentSignature(options,environment.getProperty(RazorPayConstant.KEY_SECRET)))
                throw new CustomException("Payment signature is invalid",HttpStatus.BAD_REQUEST);
        } catch(CustomException e) { throw e; }
        catch(Exception e) { throw new CustomException("Payment verification failed",HttpStatus.BAD_REQUEST); }
        MessageWallet wallet=lockedWallet(type,tenantId,p.getChannel());
        wallet.setAvailableCredits(wallet.getAvailableCredits()+p.getCredits());walletRepository.save(wallet);
        p.setRazorpayPaymentId(request.effectivePaymentId());p.setStatus("PAID");purchaseRepository.save(p);
        MessageLedgerEntry ledger=new MessageLedgerEntry();ledger.setTenantType(type);ledger.setTenantId(tenantId);
        ledger.setChannel(p.getChannel());ledger.setEntryType("PURCHASE");ledger.setCreditDelta((long)p.getCredits());
        ledger.setReferenceId(p.getRazorpayPaymentId());ledger.setIdempotencyKey("PURCHASE:"+p.getUuid());
        ledger.setAmountExTax(p.getAmountExTax());ledger.setTaxAmount(p.getTaxAmount());ledgerRepository.save(ledger);
        audit(type,tenantId,null,actor,"CREDIT_PURCHASE_VERIFIED","Verified purchase");
        return purchaseModel(p);
    }

    @Transactional
    public boolean creditVerifiedWebhook(String orderId,String paymentId) {
        return creditVerifiedWebhook(orderId,paymentId,null,null);
    }

    @Transactional
    public boolean creditVerifiedWebhook(String orderId,String paymentId,Integer amountPaise,String currency) {
        MessagePurchase p=purchaseRepository.lockByRazorpayOrderId(orderId).orElse(null);
        if(p==null) return false;
        if("PAID".equals(p.getStatus())) return true;
        if(amountPaise!=null&&amountPaise.intValue()!=p.getTotalAmount().multiply(new BigDecimal("100")).intValueExact())
            throw new CustomException("Messaging payment amount does not match the purchase",HttpStatus.BAD_REQUEST);
        if(StringUtils.hasText(currency)&&!"INR".equalsIgnoreCase(currency))
            throw new CustomException("Messaging payment currency does not match the purchase",HttpStatus.BAD_REQUEST);
        if(purchaseRepository.findByRazorpayPaymentId(paymentId).isPresent())
            throw new CustomException("Payment is already associated with another messaging purchase",HttpStatus.CONFLICT);
        MessageWallet wallet=lockedWallet(p.getTenantType(),p.getTenantId(),p.getChannel());
        wallet.setAvailableCredits(wallet.getAvailableCredits()+p.getCredits());walletRepository.save(wallet);
        p.setRazorpayPaymentId(paymentId);p.setStatus("PAID");purchaseRepository.save(p);
        MessageLedgerEntry ledger=new MessageLedgerEntry();ledger.setTenantType(p.getTenantType());
        ledger.setTenantId(p.getTenantId());ledger.setChannel(p.getChannel());ledger.setEntryType("PURCHASE");
        ledger.setCreditDelta((long)p.getCredits());ledger.setReferenceId(paymentId);
        ledger.setIdempotencyKey("PURCHASE:"+p.getUuid());ledger.setAmountExTax(p.getAmountExTax());
        ledger.setTaxAmount(p.getTaxAmount());ledgerRepository.save(ledger);
        audit(p.getTenantType(),p.getTenantId(),null,null,"CREDIT_PURCHASE_WEBHOOK","Verified Razorpay webhook");
        return true;
    }

    @Transactional
    public void markSent(Long id,String providerId) {
        PracticeMessage m=messages.findById(id).orElse(null);if(m==null||m.getStatus()!=MessageStatus.SENDING)return;
        settle(m);        m.setProviderMessageId(providerId);m.setSentAt(LocalDateTime.now());m.setStatus(MessageStatus.SENT);messages.save(m);
        audit(m.getTenantType(),m.getTenantId(),m.getUuid(),null,"SENT","Provider accepted message");
    }
    @Transactional
    public void markFailed(Long id,String safeCode) {
        PracticeMessage m=messages.findById(id).orElse(null);if(m==null||m.getStatus()!=MessageStatus.SENDING)return;
        release(m,"FAIL:"+m.getUuid()+":"+m.getAttempts());m.setFailureCode(safeCode);m.setStatus(MessageStatus.FAILED);
        messages.save(m);audit(m.getTenantType(),m.getTenantId(),m.getUuid(),null,"FAILED",safeCode);
    }

    @Transactional
    public void reserveAutomation(PracticeMessage message) {
        PracticeMessage current = messages.lockByUuidAndTenant(
                message.getUuid(), message.getTenantType(), message.getTenantId())
                .orElseThrow(() -> new CustomException("Automation message no longer exists", HttpStatus.CONFLICT));
        if ((current.getReservedCredits() != null && current.getReservedCredits() > 0)
                || Boolean.TRUE.equals(current.getFreeAllowanceReserved())) return;
        reserve(current, readVars(current));
    }

    @Transactional
    public void settleAutomation(PracticeMessage message) {
        PracticeMessage current = messages.lockByUuidAndTenant(
                message.getUuid(), message.getTenantType(), message.getTenantId()).orElse(null);
        if (current != null) settle(current);
    }

    @Transactional
    public void releaseAutomation(PracticeMessage message, String key) {
        PracticeMessage current = messages.lockByUuidAndTenant(
                message.getUuid(), message.getTenantType(), message.getTenantId()).orElse(null);
        if (current != null) release(current, key);
    }

    @Transactional
    public void applyDeliveryStatus(String providerId,String status) {
        PracticeMessage m=messages.findByProviderMessageId(providerId).orElse(null);
        if(m==null) return;
        if("DELIVERED".equals(status) && m.getStatus()==MessageStatus.SENT) m.setStatus(MessageStatus.DELIVERED);
        else if("READ".equals(status) && (m.getStatus()==MessageStatus.SENT||m.getStatus()==MessageStatus.DELIVERED))
            m.setStatus(MessageStatus.READ);
        else if("FAILED".equals(status) && m.getStatus()==MessageStatus.SENT) m.setStatus(MessageStatus.FAILED);
        else return;
        messages.save(m);audit(m.getTenantType(),m.getTenantId(),m.getUuid(),null,"STATUS_"+status,"Provider status callback");
    }

    @Transactional(readOnly=true)
    public Overview overview(MessageTenantType type,Long tenantId) {
        java.util.Map<String,com.kittyp.notification.model.MessagingDtos.ChannelRateOverview> rates=new java.util.LinkedHashMap<>();
        for(MessageChannel channel:MessageChannel.values()) {
            MessageRateCard card=rateCards.get(channel);
            rates.put(channel.name(),new com.kittyp.notification.model.MessagingDtos.ChannelRateOverview(
                    balance(type,tenantId,channel),BigDecimal.ONE,card.getUnitRate(),card.getIncludedMonthly(),
                    card.getPackCredits(),card.getPackPriceExTax(),card.getTaxPercent()));
        }
        boolean paused=getSettings(type,tenantId).paused();
        return new Overview(paused,rates,templateCatalog.list(),messages.countByTenantTypeAndTenantIdAndStatus(type,tenantId,MessageStatus.QUEUED),
                messages.countByTenantTypeAndTenantIdAndStatus(type,tenantId,MessageStatus.SCHEDULED),
                messages.countByTenantTypeAndTenantIdAndStatus(type,tenantId,MessageStatus.SENT),
                messages.countByTenantTypeAndTenantIdAndStatus(type,tenantId,MessageStatus.DELIVERED),
                messages.countByTenantTypeAndTenantIdAndStatus(type,tenantId,MessageStatus.FAILED),
                messages.countByTenantTypeAndTenantIdAndStatus(type,tenantId,MessageStatus.CANCELLED),
                paused);
    }

    private void reserve(PracticeMessage m,Map<String,Object> vars) {
        MessageRateCard rate=rateCards.get(m.getChannel());
        MessageWallet wallet=lockedWallet(m.getTenantType(),m.getTenantId(),m.getChannel());
        int credits;
        boolean free=Boolean.TRUE.equals(m.getFreeAllowanceReserved());
        if(m.getChannel()==MessageChannel.EMAIL && rate.getIncludedMonthly()>0) {
            LocalDateTime monthStart=LocalDate.now().withDayOfMonth(1).atStartOfDay();
            long used=messages.countByTenantTypeAndTenantIdAndChannelAndCreatedAtGreaterThanEqualAndStatusIn(
                    m.getTenantType(),m.getTenantId(),m.getChannel(),monthStart,
                    List.of(MessageStatus.SCHEDULED,MessageStatus.QUEUED,MessageStatus.PAUSED,MessageStatus.SENDING,
                            MessageStatus.SENT,MessageStatus.DELIVERED,MessageStatus.READ));
            if(m.getId()!=null && used>0) used--;
            free=free||used<rate.getIncludedMonthly();
        }
        if(free) credits=0;
        else if(m.getChannel()==MessageChannel.SMS) {
            int length=renderedSmsLength(m,vars);
            credits=Math.max(1,(int)Math.ceil((double)Math.max(1,length)/Math.max(1,rate.getSegmentSize())));
        } else credits=1;
        if(credits>0) {
            if(wallet.getAvailableCredits()<credits) throw new CustomException("Insufficient "+m.getChannel()+" credits",
                    HttpStatus.PAYMENT_REQUIRED);
            wallet.setAvailableCredits(wallet.getAvailableCredits()-credits);
            wallet.setReservedCredits(wallet.getReservedCredits()+credits);walletRepository.save(wallet);
        }
        m.setReservedCredits(credits);m.setFreeAllowanceReserved(free);messages.save(m);
        ledger(m,free?"FREE_ALLOWANCE":"RESERVE",-credits,"RESERVE:"+m.getUuid()+":"+UUID.randomUUID(),null,null);
    }
    private void settle(PracticeMessage m) {
        int credits=m.getReservedCredits()==null?0:m.getReservedCredits();
        if(credits>0) {
            MessageWallet wallet=lockedWallet(m.getTenantType(),m.getTenantId(),m.getChannel());
            wallet.setReservedCredits(Math.max(0,wallet.getReservedCredits()-credits));walletRepository.save(wallet);
        }
        ledger(m,"SETTLE",0,"SETTLE:"+m.getUuid()+":"+m.getAttempts(),null,null);
        m.setReservedCredits(0);m.setFreeAllowanceReserved(false);
    }
    private void release(PracticeMessage m,String key) {
        int credits=m.getReservedCredits()==null?0:m.getReservedCredits();
        if(credits>0) {
            MessageWallet wallet=lockedWallet(m.getTenantType(),m.getTenantId(),m.getChannel());
            wallet.setReservedCredits(Math.max(0,wallet.getReservedCredits()-credits));
            wallet.setAvailableCredits(wallet.getAvailableCredits()+credits);walletRepository.save(wallet);
            ledger(m,"RELEASE",credits,key,null,null);
        } else if(Boolean.TRUE.equals(m.getFreeAllowanceReserved())) ledger(m,"FREE_ALLOWANCE_RELEASE",0,key,null,null);
        m.setReservedCredits(0);m.setFreeAllowanceReserved(false);
    }
    private void ledger(PracticeMessage m,String entry,long delta,String key,BigDecimal ex,BigDecimal tax) {
        if(ledgerRepository.findByTenantTypeAndTenantIdAndChannelAndIdempotencyKey(
                m.getTenantType(),m.getTenantId(),m.getChannel(),key).isPresent()) return;
        MessageLedgerEntry l=new MessageLedgerEntry();l.setTenantType(m.getTenantType());l.setTenantId(m.getTenantId());
        l.setChannel(m.getChannel());l.setEntryType(entry);l.setCreditDelta((long)delta);l.setMessageUuid(m.getUuid());
        l.setReferenceId(m.getUuid());l.setIdempotencyKey(key);l.setAmountExTax(ex);l.setTaxAmount(tax);ledgerRepository.save(l);
    }
    private MessageWallet getOrCreateWallet(MessageTenantType type,Long tenantId,MessageChannel channel) {
        return walletRepository.findByTenantTypeAndTenantIdAndChannel(type,tenantId,channel).orElseGet(()->{
            MessageWallet w=new MessageWallet();w.setTenantType(type);w.setTenantId(tenantId);w.setChannel(channel);
            w.setAvailableCredits(0L);w.setReservedCredits(0L);return walletRepository.save(w);
        });
    }
    private MessageWallet lockedWallet(MessageTenantType type,Long tenantId,MessageChannel channel) {
        getOrCreateWallet(type,tenantId,channel);
        return walletRepository.lockWallet(type,tenantId,channel).orElseThrow();
    }
    private MessageSettings settings(MessageTenantType type,Long tenantId) {
        return settingsRepository.findByTenantTypeAndTenantId(type,tenantId).orElseGet(()->{
            MessageSettings s=new MessageSettings();s.setTenantType(type);s.setTenantId(tenantId);
            s.setPaused(false);s.setAutomationsJson("{}");return settingsRepository.save(s);
        });
    }
    private PracticeMessage require(MessageTenantType type,Long tenantId,String uuid) {
        return messages.findByUuidAndTenantTypeAndTenantId(uuid,type,tenantId).orElseThrow(()->
                new CustomException("Message not found",HttpStatus.NOT_FOUND));
    }
    private PracticeMessage requireForUpdate(MessageTenantType type,Long tenantId,String uuid) {
        return messages.lockByUuidAndTenant(uuid,type,tenantId).orElseThrow(()->
                new CustomException("Message not found",HttpStatus.NOT_FOUND));
    }
    private void validateTemplateRequest(MessageChannel channel,String template,String emailBody) {
        if(!StringUtils.hasText(template) && !(channel==MessageChannel.EMAIL&&StringUtils.hasText(emailBody)))
            throw new CustomException("A configured template key or email content is required",HttpStatus.BAD_REQUEST);
        if(StringUtils.hasText(template)&&!template.matches("[A-Za-z0-9_-]{1,100}"))
            throw new CustomException("Invalid template key",HttpStatus.BAD_REQUEST);
        if(channel==MessageChannel.SMS && StringUtils.hasText(emailBody))
            throw new CustomException("SMS content must come from an approved MSG91 Flow template",HttpStatus.BAD_REQUEST);
        if(channel==MessageChannel.WHATSAPP && StringUtils.hasText(emailBody))
            throw new CustomException("WhatsApp content must come from an approved template",HttpStatus.BAD_REQUEST);
    }
    private void validateRecipient(MessageChannel channel,String recipient) {
        String value=recipient==null?"":recipient.trim();
        if(channel==MessageChannel.EMAIL) {
            if(!value.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"))
                throw new CustomException("Email recipient address is invalid",HttpStatus.BAD_REQUEST);
        } else {
            String digits=value.replaceAll("[^0-9]","");
            if(digits.length()<10||digits.length()>15)
                throw new CustomException("Phone recipient number is invalid",HttpStatus.BAD_REQUEST);
        }
    }
    private void validateVariables(Map<String,Object> variables) {
        if(variables==null)return;
        if(variables.size()>100) throw new CustomException("Too many template variables",HttpStatus.BAD_REQUEST);
        for(Map.Entry<String,Object> entry:variables.entrySet()) {
            if(entry.getKey()==null||!entry.getKey().matches("[A-Za-z0-9_]{1,60}"))
                throw new CustomException("Invalid template variable name",HttpStatus.BAD_REQUEST);
            if(entry.getValue()!=null&&String.valueOf(entry.getValue()).length()>1000)
                throw new CustomException("Template variable exceeds 1000 characters",HttpStatus.BAD_REQUEST);
        }
    }
    private MessageModel model(PracticeMessage m) {
        Map<String,Object> vars=readVars(m);
        Map<String,Object> contents=new java.util.LinkedHashMap<>();
        contents.put(m.getChannel().name(),m.getMessageBodySnapshot());
        return new MessageModel(m.getUuid(),m.getRecipient(),m.getRecipientName(),m.getPetName(),m.getStatus().name(),
                List.of(m.getChannel()),m.getScheduledAt(),m.getSentAt(),contents,
                m.getMessageBodySnapshot(),
                m.getChannel(),m.getStatus(),m.getSubject(),m.getTemplateKey(),vars,m.getScheduledAt(),
                m.getProviderMessageId(),m.getFailureCode(),m.getAttempts(),m.getCreatedAt(),
                m.getAutomationId(),m.getSourceType(),m.getSourceUuid());
    }
    private String snapshotContent(PracticeMessage message,Map<String,Object> variables) {
        String body=message.getEmailBody();
        if(message.getChannel()!=MessageChannel.EMAIL||!StringUtils.hasText(body)) {
            var template=templateCatalog.require(message.getChannel(),message.getTemplateKey());
            if(template.requiresDocument()) {
                throw new CustomException(
                        "This document template requires an invoice PDF and can only be sent from Billing",
                        HttpStatus.BAD_REQUEST);
            }
            if(template.variables()!=null) for(String key:template.variables()) {
                if(variables==null||!variables.containsKey(key)||variables.get(key)==null)
                    throw new CustomException("Message is missing a required approved-template variable",
                            HttpStatus.BAD_REQUEST);
            }
            body=template.body();
        }
        if(!StringUtils.hasText(body)) return "";
        String rendered=body;
        if(variables!=null) for(Map.Entry<String,Object> entry:variables.entrySet()) {
            if(entry.getValue()==null) continue;
            String value=String.valueOf(entry.getValue());
            rendered=rendered.replace("{{"+entry.getKey()+"}}",value)
                    .replace("{{ "+entry.getKey()+" }}",value);
        }
        return rendered;
    }
    private Map<String,Object> readVars(PracticeMessage m) {
        try { return StringUtils.hasText(m.getTemplateVariables())?objectMapper.readValue(m.getTemplateVariables(),
                new TypeReference<Map<String,Object>>(){}):Map.of(); } catch(Exception e) { return Map.of(); }
    }
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch(Exception e) { throw new CustomException(
                "Message variables are invalid",HttpStatus.BAD_REQUEST); }
    }
    private Map<String,Boolean> readAutomations(String value) {
        try{return objectMapper.readValue(value,new TypeReference<Map<String,Boolean>>(){});}catch(Exception e){return Map.of();}
    }
    private void audit(MessageTenantType type,Long tenantId,String uuid,String actor,String action,String detail) {
        MessageAuditEvent e=new MessageAuditEvent();e.setTenantType(type);e.setTenantId(tenantId);e.setMessageUuid(uuid);
        e.setActorEmail(actor);e.setAction(action);e.setSafeDetail(detail==null?null:detail.substring(0,Math.min(300,detail.length())));
        auditRepository.save(e);
    }
    private long balance(MessageTenantType type,Long id,MessageChannel channel) {
        return walletRepository.findByTenantTypeAndTenantIdAndChannel(type,id,channel).map(MessageWallet::getAvailableCredits).orElse(0L);
    }
    private boolean isTerminal(MessageStatus s) {
        return s==MessageStatus.SENT||s==MessageStatus.DELIVERED||s==MessageStatus.READ||s==MessageStatus.CANCELLED;
    }
    private int renderedSmsLength(PracticeMessage message,Map<String,Object> vars) {
        try {
            var template=templateCatalog.require(MessageChannel.SMS,message.getTemplateKey());
            String rendered=template.body()==null?"":template.body();
            if(vars!=null) for(Map.Entry<String,Object> entry:vars.entrySet()) {
                if(entry.getValue()==null) continue;
                rendered=rendered.replace("{{"+entry.getKey()+"}}",String.valueOf(entry.getValue()))
                        .replace("{{ "+entry.getKey()+" }}",String.valueOf(entry.getValue()));
            }
            if(!rendered.isBlank()) return rendered.codePointCount(0,rendered.length());
        } catch(CustomException ignored) {
            // The dispatch path reports missing/invalid templates. Reserve conservatively from variables meanwhile.
        }
        return vars==null?0:vars.values().stream().filter(java.util.Objects::nonNull)
                .mapToInt(v->String.valueOf(v).codePointCount(0,String.valueOf(v).length())).sum();
    }
    private PurchaseModel purchaseModel(MessagePurchase p) {
        return new PurchaseModel(p.getUuid(),p.getChannel().name()+"_"+p.getCredits(),p.getRazorpayOrderId(),
                p.getCredits(),p.getAmountExTax(),p.getTaxAmount(),p.getTotalAmount(),"INR",p.getStatus(),
                environment.getProperty(RazorPayConstant.KEY_ID));
    }
}
