package com.kittyp.common.health;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "dashboard")
public class HealthDashboardEndpoint {

	@ReadOperation(produces = MediaType.TEXT_HTML_VALUE)
	public String dashboard() throws IOException {
		try (InputStream in = HealthDashboardEndpoint.class.getResourceAsStream("/health-dashboard.html")) {
			if (in == null) {
				return "<html><body>health dashboard missing</body></html>";
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
