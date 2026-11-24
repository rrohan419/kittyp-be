package com.kittyp.notification.service;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.model.MessagingDtos.ApprovedTemplate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingTemplateCatalogService {
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final KittypWhatsAppMessagingService kittypWhatsAppMessaging;

    public List<ApprovedTemplate> list() {
        List<ApprovedTemplate> templates=new ArrayList<>(configuredTemplates().stream()
                .filter(template->!MessageChannel.WHATSAPP.name().equalsIgnoreCase(template.channel()))
                .toList());
        templates.addAll(kittypWhatsAppMessaging.listApprovedTemplates());
        return templates.stream().filter(this::valid).toList();
    }

    private List<ApprovedTemplate> configuredTemplates() {
        String raw=environment.getProperty("messaging.templates.catalog","[]");
        try {
            ApprovedTemplate[] parsed=objectMapper.readValue(raw,ApprovedTemplate[].class);
            return Arrays.stream(parsed).filter(this::valid).toList();
        } catch(Exception e) {
            throw new CustomException("Messaging template catalog configuration is invalid",HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    public ApprovedTemplate require(MessageChannel channel,String id) {
        if(channel==MessageChannel.WHATSAPP) {
            return kittypWhatsAppMessaging.requireApprovedTemplate(id);
        }
        Optional<ApprovedTemplate> found=configuredTemplates().stream().filter(t->t.id().equals(id)
                && t.channel().equalsIgnoreCase(channel.name())).findFirst();
        return found.orElseThrow(()->new CustomException(
                "No approved "+channel+" template is configured for this template id",HttpStatus.SERVICE_UNAVAILABLE));
    }
    private boolean valid(ApprovedTemplate t) {
        if(t==null||!StringUtils.hasText(t.id())||!t.id().matches("[A-Za-z0-9_-]{1,100}")
                ||!StringUtils.hasText(t.name())||!StringUtils.hasText(t.channel())) return false;
        MessageChannel channel=Arrays.stream(MessageChannel.values())
                .filter(c->c.name().equalsIgnoreCase(t.channel())).findFirst().orElse(null);
        if(channel==null) return false;
        return (channel==MessageChannel.EMAIL||StringUtils.hasText(t.body()))
                &&(channel!=MessageChannel.WHATSAPP||StringUtils.hasText(t.language()));
    }
}
