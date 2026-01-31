package com.zenith.pearlplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zenith.pearlplus.model.ApiErrorResponse;
import com.zenith.pearlplus.model.PearlLoadRequest;
import com.zenith.pearlplus.model.PearlLoadResponse;
import com.zenith.pearlplus.model.PearlStatusRequest;
import com.zenith.pearlplus.model.PearlStatusResponse;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class PearlPlusApi {
    public static final PearlPlusApi INSTANCE = new PearlPlusApi();

    private final Gson gson = new GsonBuilder().create();

    public PearlStatusResponse fetchPearls(String playerName, String baseUrl, String token)
        throws IOException, InterruptedException, PearlPlusApiException {
        PearlStatusRequest request = new PearlStatusRequest(playerName);
        String requestJson = gson.toJson(request);
        var url = normalizeBaseUrl(baseUrl) + "/pearlplus/status";
        try (var client = buildHttpClient()) {
            HttpRequest httpRequest = buildBaseRequest(url, token)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.body(), PearlStatusResponse.class);
        }
    }

    public PearlLoadResponse loadPearl(String playerName, String pearlId, String baseUrl, String token)
        throws IOException, InterruptedException, PearlPlusApiException {
        PearlLoadRequest request = new PearlLoadRequest(playerName, pearlId);
        String requestJson = gson.toJson(request);
        var url = normalizeBaseUrl(baseUrl) + "/pearlplus/load";
        try (var client = buildHttpClient()) {
            HttpRequest httpRequest = buildBaseRequest(url, token)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.body(), PearlLoadResponse.class);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            return baseUrl;
        }
        return "http://" + baseUrl;
    }

    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    }

    private HttpRequest.Builder buildBaseRequest(final String uri, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("User-Agent", "PearlPlusMod/" + FabricLoader.getInstance().getModContainer("pearlplus")
                .get().getMetadata().getVersion().getFriendlyString())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15));
        String normalizedToken = normalizeAuthToken(token);
        if (!normalizedToken.isBlank()) {
            builder.header("Authorization", normalizedToken);
        }
        return builder;
    }

    private String normalizeAuthToken(String token) {
        if (token == null) {
            return "";
        }
        String trimmed = token.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private <T> T parseResponse(String bodyJson, Class<T> type) throws PearlPlusApiException {
        JsonObject root = JsonParser.parseString(bodyJson).getAsJsonObject();
        if (root.has("error")) {
            ApiErrorResponse error = gson.fromJson(root, ApiErrorResponse.class);
            throw new PearlPlusApiException(error.error());
        }
        return gson.fromJson(root, type);
    }
}
