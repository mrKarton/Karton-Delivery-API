package com.karton.delivery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    public static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Path configPath = Paths.get(
            "config",
            "karton-delivery"
    );

    protected static File mainConfigFile = new File(
            configPath.toFile(),
            "main.json"
    );

    private static MainConfig cachedMainConfig;

    public static void init() throws IOException {
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath);
        }

        cachedMainConfig = loadMainConfig();
    }

    public static MainConfig getMainConfig() {
        if (cachedMainConfig != null) {
            return cachedMainConfig;
        }

        throw new RuntimeException("Main config file is not loaded");
    }

    private static void initializeMainConfig() {
        MainConfig mainConfig = new MainConfig();

        try (
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(mainConfigFile),
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            String jsonString = GSON.toJson(mainConfig);

            writer.write(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MainConfig loadMainConfig () {
        if (!mainConfigFile.exists()) {
            initializeMainConfig();
            return new MainConfig();
        }

        try (
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(
                                mainConfigFile
                        ),
                        StandardCharsets.UTF_8
                )
        ) {
            String jsonString = reader.readAllAsString();

            return GSON.fromJson(
                    jsonString,
                    MainConfig.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
