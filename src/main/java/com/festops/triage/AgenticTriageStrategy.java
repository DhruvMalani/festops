package com.festops.triage;

import com.festops.model.IncidentType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * LLM-backed triage strategy. Calls the Anthropic Messages API (raw HTTP via
 * Java's {@link HttpClient}) to classify an SOS report into an
 * {@link IncidentType} and severity, parsing the model's JSON reply with Gson.
 *
 * <p>The whole call is wrapped in a {@link CompletableFuture} with a 5-second
 * timeout; on timeout, network/API failure, a missing API key, or an
 * unparseable reply it falls back to {@link RuleBasedTriageStrategy} so triage
 * never blocks the dispatch pipeline.</p>
 *
 * <p>The API key is read from the {@code ANTHROPIC_API_KEY} environment
 * variable.</p>
 */
public class AgenticTriageStrategy implements TriageStrategy {

    private static final Logger log = LoggerFactory.getLogger(AgenticTriageStrategy.class);

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-opus-4-8";
    private static final int MAX_TOKENS = 256;
    private static final long TIMEOUT_SECONDS = 5;

    private static final String SYSTEM_PROMPT = """
            You are an emergency-dispatch triage classifier for a large cultural festival.
            Classify the incoming SOS report into exactly one incident type and one severity.

            Respond with ONLY a single JSON object — no prose, no explanation, no markdown
            code fences — in exactly this shape:
            {"type": "<TYPE>", "severity": "<SEVERITY>"}

            <TYPE> must be one of: MEDICAL, SECURITY, FIRE, LOGISTICS
            <SEVERITY> must be one of: CRITICAL, HIGH, MEDIUM, LOW
            """;

    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final TriageStrategy fallback;

    public AgenticTriageStrategy() {
        this(new RuleBasedTriageStrategy());
    }

    public AgenticTriageStrategy(TriageStrategy fallback) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.gson = new Gson();
        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
        this.fallback = fallback;
    }

    @Override
    public IncidentType classify(String sosText) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY not set — using rule-based triage");
            return fallback.classify(sosText);
        }
        if (sosText == null || sosText.isBlank()) {
            return fallback.classify(sosText);
        }

        try {
            return CompletableFuture
                    .supplyAsync(() -> callApi(sosText))
                    .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (RuntimeException e) {
            log.warn("Agentic triage failed ({}) — falling back to rule-based",
                    e.getCause() != null ? e.getCause() : e);
            return fallback.classify(sosText);
        }
    }

    /** Performs the HTTP call and parses the classification. Throws on any failure. */
    private IncidentType callApi(String sosText) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(sosText)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException(
                        "Anthropic API HTTP " + response.statusCode() + ": " + response.body());
            }
            return parseClassification(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // checked exceptions (IOException, InterruptedException) -> unchecked for the future
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Anthropic API call failed", e);
        }
    }

    private String buildRequestBody(String sosText) {
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", sosText);

        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        JsonObject root = new JsonObject();
        root.addProperty("model", MODEL);
        root.addProperty("max_tokens", MAX_TOKENS);
        root.addProperty("system", SYSTEM_PROMPT);
        root.add("messages", messages);
        return gson.toJson(root);
    }

    /** Extracts the model's text block, parses its JSON, and maps to an IncidentType. */
    private IncidentType parseClassification(String responseBody) {
        JsonObject root = gson.fromJson(responseBody, JsonObject.class);
        JsonArray content = root.getAsJsonArray("content");
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Anthropic response had no content blocks");
        }

        String text = null;
        for (JsonElement element : content) {
            JsonObject block = element.getAsJsonObject();
            if (block.has("type") && "text".equals(block.get("type").getAsString())) {
                text = block.get("text").getAsString();
                break;
            }
        }
        if (text == null) {
            throw new RuntimeException("Anthropic response had no text block");
        }

        TriageResult result = gson.fromJson(extractJsonObject(text), TriageResult.class);
        if (result == null || result.type == null) {
            throw new RuntimeException("Could not parse classification JSON: " + text);
        }

        IncidentType type = IncidentType.valueOf(result.type.trim().toUpperCase());
        log.info("Agentic triage -> {} (severity {})", type, result.severity);
        return type;
    }

    /** Defensive: strip any prose/markdown fences and return the first {...} object. */
    private static String extractJsonObject(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /** Gson target for the model's JSON reply. */
    private static final class TriageResult {
        String type;
        String severity;
    }
}
