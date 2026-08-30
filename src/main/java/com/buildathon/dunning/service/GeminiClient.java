package com.buildathon.dunning.service;

import com.buildathon.dunning.dto.FailureClassification;
import com.buildathon.dunning.dto.RecoveryMessageContent;
import com.buildathon.dunning.entity.enums.Channel;
import com.buildathon.dunning.entity.enums.FailureCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Calls the Gemini Interactions API (the current recommended endpoint,
 * generally available since June 2026 - generateContent still works but is
 * the legacy path now) for two things: classifying a payment failure into a
 * retry strategy, and generating the personalized recovery message for a
 * retry attempt. Both use structured JSON output so results parse reliably
 * instead of scraping free text.
 * <p>
 * Both public methods throw on any failure (missing key, network error,
 * malformed response) - {@link FailureClassificationService} and
 * {@link MessageGenerationService} are what catch that and fall back, so
 * this class stays a thin, honest wrapper around the API call itself.
 */
@Component
public class GeminiClient {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions";

    // Pins the post-May-2026 "steps" response schema explicitly. It's been
    // the default since May 26, 2026, so this header is belt-and-suspenders
    // - safe to drop later if Google retires the legacy schema opt-out
    // entirely, but harmless to leave in the meantime.
    private static final String API_REVISION = "2026-05-20";

    private final RestClient restClient = RestClient.create();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String model;

    public FailureClassification classify(FailureCode failureCode, BigDecimal amount, String currency) {
        String prompt = buildClassificationPrompt(failureCode, amount, currency);
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "retryWindowHours", Map.of("type", "integer"),
                        "channel", Map.of("type", "string", "enum", List.of("EMAIL", "SMS")),
                        "suggestAltPaymentMethod", Map.of("type", "boolean"),
                        "confidence", Map.of("type", "number"),
                        "reasoning", Map.of("type", "string")
                ),
                "required", List.of("retryWindowHours", "channel", "suggestAltPaymentMethod", "confidence", "reasoning")
        );

        JsonNode node = callInteractions(prompt, schema);
        return new FailureClassification(
                node.get("retryWindowHours").asInt(),
                Channel.valueOf(node.get("channel").asText()),
                node.get("suggestAltPaymentMethod").asBoolean(),
                node.get("confidence").asDouble(),
                node.get("reasoning").asText()
        );
    }

    public RecoveryMessageContent generateMessage(FailureCode failureCode, BigDecimal amount, String currency,
                                                    String customerName, boolean suggestAltPaymentMethod) {
        String prompt = buildMessagePrompt(failureCode, amount, currency, customerName, suggestAltPaymentMethod);
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "subject", Map.of("type", "string"),
                        "body", Map.of("type", "string")
                ),
                "required", List.of("subject", "body")
        );

        JsonNode node = callInteractions(prompt, schema);
        return new RecoveryMessageContent(node.get("subject").asText(), node.get("body").asText());
    }

    private String buildClassificationPrompt(FailureCode failureCode, BigDecimal amount, String currency) {
        return """
                You are a payment recovery (dunning) strategist for a company processing recurring
                subscription payments in India.

                A recurring payment just failed with:
                - Failure reason code: %s
                - Amount: %s %s

                Recommend the best retry strategy to recover this payment. Consider how this specific
                failure reason typically behaves in real-world payment recovery: some failures resolve
                quickly (transient/technical issues), others need a longer wait (e.g. insufficient funds,
                which often resolves near a salary/payday), and some barely recover at all without
                prompting the customer to update their payment method (e.g. an expired card).

                Respond with: the retry window in hours before the first retry, the best notification
                channel (EMAIL or SMS), whether the customer should be prompted to add an alternate
                payment method, your confidence from 0.0 to 1.0, and a short one-sentence reasoning.
                """.formatted(failureCode, amount, currency);
    }

    private String buildMessagePrompt(FailureCode failureCode, BigDecimal amount, String currency,
                                       String customerName, boolean suggestAltPaymentMethod) {
        return """
                You are writing a short payment-recovery notification email on behalf of a company
                processing recurring subscription payments in India. The tone should be warm, brief,
                and reassuring - not alarming or pushy.

                Customer name: %s
                Failed payment amount: %s %s
                Failure reason code: %s
                Should this message prompt the customer to add an alternate payment method?: %s

                Write a short email with a subject line and a body of 2-3 sentences. If prompted to
                suggest an alternate payment method, gently ask them to update their payment details.
                Otherwise, reassure them the payment will be retried automatically and no action is
                needed. Do not include a greeting salutation or sign-off - just the core message body.
                """.formatted(customerName, amount, currency, failureCode, suggestAltPaymentMethod);
    }

    // Interactions API structured output uses plain JSON Schema (lowercase
    // types), unlike the legacy generateContent endpoint's OpenAPI-style
    // uppercase Schema object (STRING/OBJECT/etc) - both callers above build
    // their schema this way.
    private JsonNode callInteractions(String prompt, Map<String, Object> responseSchema) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", prompt,
                "response_format", Map.of(
                        "type", "text",
                        "mime_type", "application/json",
                        "schema", responseSchema
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(API_URL)
                .header("x-goog-api-key", apiKey)
                .header("Api-Revision", API_REVISION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        String jsonText = extractText(response);
        try {
            return jsonMapper.readTree(jsonText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + jsonText, e);
        }
    }

    // Interactions API responses (post-May-2026 schema) return a "steps"
    // array instead of the old "candidates" shape. POST returns only the
    // output steps; we want the model_output step's text content block.
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        List<Map<String, Object>> steps = (List<Map<String, Object>>) response.get("steps");
        if (steps != null) {
            for (Map<String, Object> step : steps) {
                if (!"model_output".equals(step.get("type"))) {
                    continue;
                }
                List<Map<String, Object>> content = (List<Map<String, Object>>) step.get("content");
                if (content == null) {
                    continue;
                }
                for (Map<String, Object> block : content) {
                    if ("text".equals(block.get("type")) && block.get("text") != null) {
                        return (String) block.get("text");
                    }
                }
            }
        }
        throw new IllegalStateException("No model_output text step found in Interactions API response: " + response);
    }
}
