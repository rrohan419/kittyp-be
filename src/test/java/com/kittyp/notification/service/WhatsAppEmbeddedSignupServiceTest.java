package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class WhatsAppEmbeddedSignupServiceTest {

    private MockWebServer server;
    private DoctorProfileDao doctorProfileDao;
    private ClinicRepository clinicRepository;
    private WhatsAppEmbeddedSignupService service;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        ObjectMapper mapper = new ObjectMapper();
        String base = server.url("/").toString().replaceAll("/$", "");
        WhatsAppCredentialsVerifier verifier = new WhatsAppCredentialsVerifier(mapper, "v21.0", base);
        doctorProfileDao = mock(DoctorProfileDao.class);
        clinicRepository = mock(ClinicRepository.class);
        when(doctorProfileDao.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clinicRepository.save(any(Clinic.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new WhatsAppEmbeddedSignupService(
                mapper, verifier, doctorProfileDao, clinicRepository, "v21.0", base, "app-id", "app-secret");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void complete_exchangesCodeWithoutRedirectUri_thenSubscribesAndSaves() throws Exception {
        enqueueToken("short-token");
        enqueueToken("long-token");
        enqueueJson(200, "{\"success\":true}");
        enqueueJson(200, "{\"id\":\"phone-1\",\"status\":\"CONNECTED\"}");
        enqueueJson(200, "{\"id\":\"waba-1\"}");

        DoctorProfile profile = new DoctorProfile();
        profile.setUuid("doc-1");
        Map<String, Object> view = service.complete(profile, "auth-code", "waba-1", "phone-1");

        assertEquals(Boolean.TRUE, view.get("whatsappConfigured"));
        assertEquals("phone-1", view.get("phoneNumberId"));
        assertEquals("waba-1", view.get("businessAccountId"));
        assertEquals("long-token", profile.getWhatsappToken());
        verify(doctorProfileDao).save(profile);

        RecordedRequest codeExchange = server.takeRequest();
        assertTrue(codeExchange.getPath().contains("/v21.0/oauth/access_token"));
        assertTrue(codeExchange.getPath().contains("code=auth-code"));
        assertFalse(codeExchange.getPath().contains("redirect_uri"));

        RecordedRequest longLived = server.takeRequest();
        assertTrue(longLived.getPath().contains("grant_type=fb_exchange_token"));

        RecordedRequest subscribe = server.takeRequest();
        assertEquals("POST", subscribe.getMethod());
        assertTrue(subscribe.getPath().contains("/v21.0/waba-1/subscribed_apps"));
        assertEquals("Bearer long-token", subscribe.getHeader("Authorization"));
    }

    @Test
    void complete_retriesCodeExchangeWithLocalRedirectUri() throws Exception {
        enqueueJson(400, "{\"error\":{\"message\":\"Missing redirect_uri\",\"type\":\"OAuthException\"}}");
        enqueueToken("short-token");
        enqueueToken("long-token");
        enqueueJson(200, "{\"success\":true}");
        enqueueJson(200, "{\"id\":\"phone-1\",\"status\":\"CONNECTED\"}");
        enqueueJson(200, "{\"id\":\"waba-1\"}");

        DoctorProfile profile = new DoctorProfile();
        profile.setUuid("doc-1");
        service.complete(profile, "auth-code", "waba-1", "phone-1");

        RecordedRequest first = server.takeRequest();
        assertFalse(first.getPath().contains("redirect_uri"));
        RecordedRequest retry = server.takeRequest();
        assertTrue(retry.getPath().contains("redirect_uri="));
        assertTrue(retry.getPath().contains("localhost") && retry.getPath().contains("8080"));
    }

    @Test
    void completeClinic_savesOnClinic() throws Exception {
        enqueueToken("short-token");
        enqueueToken("long-token");
        enqueueJson(200, "{\"success\":true}");
        enqueueJson(200, "{\"id\":\"phone-1\",\"status\":\"CONNECTED\"}");
        enqueueJson(200, "{\"id\":\"waba-1\"}");

        Clinic clinic = new Clinic();
        clinic.setUuid("clinic-1");
        Map<String, Object> view = service.complete(clinic, "auth-code", "waba-1", "phone-1");

        assertEquals(Boolean.TRUE, view.get("whatsappConfigured"));
        assertEquals("phone-1", view.get("phoneNumberId"));
        assertEquals("long-token", clinic.getWhatsappToken());
        verify(clinicRepository).save(clinic);
    }

    private void enqueueToken(String token) {
        enqueueJson(200, "{\"access_token\":\"" + token + "\"}");
    }

    private void enqueueJson(int status, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }
}
