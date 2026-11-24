package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.enums.MessageTenantType;

class Msg91FlowSmsProviderTest {
    @Test
    void postsOnlyConfiguredFlowTemplateWithDltAndVariableValues() {
        MockEnvironment env=new MockEnvironment()
                .withProperty("messaging.sms.msg91.auth-key","server-auth")
                .withProperty("messaging.sms.msg91.sender-id","KITTPY")
                .withProperty("messaging.sms.msg91.pe-id","pe-001")
                .withProperty("messaging.sms.templates.reminder.flow-id","flow-01")
                .withProperty("messaging.sms.templates.reminder.dlt-template-id","dlt-01");
        RestClient.Builder builder=RestClient.builder();
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://control.msg91.com/api/v5/flow/"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("authkey","server-auth"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {"template_id":"flow-01","sender":"KITTPY","DLT_TE_ID":"dlt-01","DLT_PE_ID":"pe-001",
                     "recipients":[{"mobiles":"919876543210","pet_name":"Milo"}]}
                    """))
                .andRespond(withSuccess("{\"request_id\":\"req-123\"}",MediaType.APPLICATION_JSON));
        Msg91FlowSmsProvider provider=new Msg91FlowSmsProvider(env,new ObjectMapper(),builder.build());
        PracticeMessage msg=new PracticeMessage();msg.setChannel(MessageChannel.SMS);
        msg.setTenantType(MessageTenantType.CLINIC);msg.setTenantId(1L);msg.setStatus(MessageStatus.QUEUED);
        msg.setTemplateKey("reminder");msg.setRecipient("+91 9876543210");

        assertEquals("req-123",provider.send(msg,Map.of("pet_name","Milo")));
        server.verify();
    }

    @Test
    void missingProviderConfigurationFailsWithoutMakingHttpRequest() {
        RestClient.Builder builder=RestClient.builder();
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        Msg91FlowSmsProvider provider=new Msg91FlowSmsProvider(new MockEnvironment(),new ObjectMapper(),builder.build());
        PracticeMessage msg=new PracticeMessage();msg.setChannel(MessageChannel.SMS);
        msg.setTemplateKey("missing");msg.setRecipient("9876543210");
        CustomException ex=assertThrows(CustomException.class,()->provider.send(msg,Map.of()));
        assertTrue(ex.getMessage().contains("MSG91"));
        server.verify();
    }
}
