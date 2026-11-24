package com.kittyp.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.PracticeModel;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.emailsender.IEmailSender;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageChannel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PracticeMessageDelivery {
    private final Msg91FlowSmsProvider smsProvider;
    private final WhatsAppService whatsAppService;
    private final IEmailSender<ZeptoMailDto, ZeptoMailResponseModel> zeptoMailSender;
    private final ObjectMapper objectMapper;
    private final MessagingTemplateCatalogService templateCatalog;
    private final KittypWhatsAppMessagingService kittypWhatsAppMessaging;
    private final DoctorProfileRepository doctorProfiles;

    public String deliver(PracticeMessage message) {
        Map<String,Object> vars;
        try {
            vars=StringUtils.hasText(message.getTemplateVariables())
                    ? objectMapper.readValue(message.getTemplateVariables(),new TypeReference<Map<String,Object>>() {})
                    : Map.of();
        } catch(Exception e) { throw new CustomException("Message template variables are invalid",HttpStatus.BAD_REQUEST); }
        if(!StringUtils.hasText(message.getTemplateKey())
                && !(message.getChannel()==MessageChannel.EMAIL&&StringUtils.hasText(message.getEmailBody())))
            throw new CustomException("A configured approved template is required",HttpStatus.SERVICE_UNAVAILABLE);
        var template=message.getChannel()==MessageChannel.EMAIL&&StringUtils.hasText(message.getEmailBody())
                ? null : templateCatalog.require(message.getChannel(),message.getTemplateKey());
        if(template!=null&&template.variables()!=null) for(String key:template.variables()) {
            if(vars==null||!vars.containsKey(key))
                throw new CustomException("Message is missing a required approved-template variable",
                        HttpStatus.BAD_REQUEST);
        }
        return switch(message.getChannel()) {
            case SMS -> smsProvider.send(message,templateVariables(vars,template));
            case WHATSAPP -> sendWhatsApp(message,vars,template);
            case EMAIL -> sendEmail(message,vars,template);
        };
    }

    private String sendWhatsApp(PracticeMessage message,Map<String,Object> vars,
            com.kittyp.notification.model.MessagingDtos.ApprovedTemplate template) {
        if(!whatsAppService.isDeliveryEnabled())
            throw new CustomException("WhatsApp sending is disabled on the server. Set WHATSAPP_ENABLED=true to deliver messages.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        if(template==null||template.requiresDocument())
            throw new CustomException("Document templates must be sent with an invoice PDF from Billing",
                    HttpStatus.BAD_REQUEST);
        WhatsAppSenderCredentials sender=kittypWhatsAppMessaging.requireSender();
        if(!whatsAppService.isConfigured(sender))
            throw new CustomException("WhatsApp delivery unavailable: Kittyp sender credentials are not configured",
                    HttpStatus.SERVICE_UNAVAILABLE);
        String name=template==null?"":template.name();
        String language=template==null?"":template.language();
        if(!StringUtils.hasText(name)||!StringUtils.hasText(language))
            throw new CustomException("WhatsApp delivery unavailable: approved template is not configured",
                    HttpStatus.SERVICE_UNAVAILABLE);
        String recipient=whatsAppService.toE164Digits(message.getRecipient());
        List<String> params=template.variables()==null?new ArrayList<>():
                template.variables().stream().map(key->vars.get(key)==null?"":String.valueOf(vars.get(key))).toList();
        String providerId=whatsAppService.sendTextTemplateWithId(
                sender,recipient,name,language,params);
        return providerId;
    }

    private String sendEmail(PracticeMessage message,Map<String,Object> vars,
            com.kittyp.notification.model.MessagingDtos.ApprovedTemplate approvedTemplate) {
        String template=approvedTemplate==null?"":approvedTemplate.name();
        if(!StringUtils.hasText(message.getEmailBody())&&!StringUtils.hasText(template))
            throw new CustomException("Email delivery unavailable: approved ZeptoMail template is not configured",
                    HttpStatus.SERVICE_UNAVAILABLE);
        ZeptoMailDto dto=new ZeptoMailDto();
        dto.setTemplateKey(StringUtils.hasText(message.getEmailBody())?null:template);
        dto.setRecipientEmail(message.getRecipient());
        dto.setRecipientName(message.getRecipientName());
        dto.setMergeInfo(vars);
        dto.setMessage(message.getEmailBody());
        dto.setSubject(message.getSubject());
        if (message.getTenantType() == com.kittyp.notification.enums.MessageTenantType.CLINIC) {
            dto.setClinicAccountId(message.getTenantId());
        } else {
            doctorProfiles.findWithClinicById(message.getTenantId()).ifPresent(profile -> {
                Clinic clinic = profile.getClinic();
                if (clinic != null && clinic.getPracticeModel() == PracticeModel.PERSONAL_PRACTICE
                        && clinic.getOwner() != null
                        && clinic.getOwner().getId().equals(profile.getUser().getId())) {
                    dto.setClinicAccountId(clinic.getId());
                }
            });
        }
        ZeptoMailResponseModel response=zeptoMailSender.sendEmail(dto);
        return response==null ? null : response.getRequestId();
    }

    private Map<String,Object> templateVariables(Map<String,Object> vars,
            com.kittyp.notification.model.MessagingDtos.ApprovedTemplate template) {
        if(template==null||template.variables()==null)return vars==null?Map.of():vars;
        java.util.LinkedHashMap<String,Object> filtered=new java.util.LinkedHashMap<>();
        for(String key:template.variables()) if(vars.containsKey(key)) filtered.put(key,vars.get(key));
        return filtered;
    }
}
