package com.zenith.config;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.zenith.PearlPlusMod.LOG;

public class Config {
    public static final class PearlPlusEndpoint {
        public String name = "Default";
        public String apiUrl = "http://localhost:8080";
        public String authToken = "";
    }

    public ArrayList<PearlPlusEndpoint> endpoints = new ArrayList<>();
    public int activeEndpointIndex = 0;

    @Deprecated
    public String apiUrl = "";
    @Deprecated
    public String authToken = "";

    public static Path configPath = FabricLoader.getInstance().getConfigDir().resolve("pearlplus.json");
    static Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .setLenient()
        .create();

    public static Config loadConfig() {
        if (!configPath.toFile().exists()) {
            var config = new Config();
            config.ensureDefaultEndpoint();
            config.save();
            return config;
        }
        try (var reader = new FileReader(configPath.toFile())) {
            var config = gson.fromJson(reader, Config.class);
            config.ensureDefaultEndpoint();
            return config;
        } catch (Exception e) {
            LOG.error("Failed to load config", e);
            var config = new Config();
            config.ensureDefaultEndpoint();
            return config;
        }
    }

    public PearlPlusEndpoint getActiveEndpoint() {
        ensureDefaultEndpoint();
        int index = Math.min(Math.max(activeEndpointIndex, 0), endpoints.size() - 1);
        return endpoints.get(index);
    }

    public void ensureDefaultEndpoint() {
        if (endpoints == null) {
            endpoints = new ArrayList<>();
        }
        if (endpoints.isEmpty()) {
            PearlPlusEndpoint endpoint = new PearlPlusEndpoint();
            if (apiUrl != null && !apiUrl.isBlank()) {
                endpoint.apiUrl = apiUrl;
            }
            if (authToken != null && !authToken.isBlank()) {
                endpoint.authToken = authToken;
            }
            endpoints.add(endpoint);
        }
        if (activeEndpointIndex < 0 || activeEndpointIndex >= endpoints.size()) {
            activeEndpointIndex = 0;
        }
    }

    public void save() {
        try {
            File tempFile = File.createTempFile("pearlplus-mod", null);
            try (var writer = new FileWriter(tempFile)) {
                gson.toJson(this, writer);
            }
            Files.move(tempFile, configPath.toFile());
        } catch (Exception e) {
            LOG.error("Failed to write config", e);
        }
    }
}
