package com.buildathon.dunning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends real email via Resend's API. Throws on any failure (missing key,
 * network error, non-2xx response) — {@link RecoveryMessageService} is what
 * decides whether a send should be attempted at all and catches failures,
 * so this stays a thin wrapper around the actual API call.
 */
@Component
public class ResendEmailService {

    private static final String API_URL = "https://api.resend.com/emails";

    private final RestClient restClient = RestClient.create();

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-address:Dunning Engine <onboarding@resend.dev>}")
    private String fromAddress;

    public void send(String toEmail, String subject, String body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }

        Map<String, Object> requestBody = Map.of(
                "from", fromAddress,
                "to", List.of(toEmail),
                "subject", subject,
                "text", body
        );

        restClient.post()
                .uri(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}
