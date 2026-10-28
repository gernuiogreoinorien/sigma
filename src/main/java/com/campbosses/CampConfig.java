package com.campbosses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Settings saved to .minecraft/config/camp-bosses.json */
public class CampConfig {
    public static final List<String> BOSSES = List.of(
            "Desert Leader", "Ice Leader", "Nether Leader", "Void Leader", "Poseidon",
            "Sanctum Leader", "Shrine Leader", "Sanctuary Leader", "Citadel Leader",
            "Jurassic Leader", "Volcano Leader", "Rome Leader", "Medieval Leader");

    private static final Map<String, Integer> DEFAULT_INTERVALS = Map.of(
            "Desert Leader", 10, "Ice Leader", 20, "Nether Leader", 40,
            "Void Leader", 60, "Poseidon", 60);

    /** Popup + sound on/off per boss. */
    public Map<String, Boolean> alerts = new LinkedHashMap<>();
    /** Respawn time in minutes per boss (0 = unknown). */
    public Map<String, Integer> intervals = new LinkedHashMap<>();
    public boolean sound = true;
    public boolean hud = true;
    /** Chat Games timer + alert. */
    public boolean chatGames = true;
    public int chatGamesIntervalMinutes = 5;
    public int chatGamesWarnSeconds = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("camp-bosses.json");
    private static CampConfig instance;

    public static CampConfig get() {
        if (instance == null) load();
        return instance;
    }

    public boolean alertsOn(String boss) {
        return alerts.getOrDefault(boss, true);
    }

    public int interval(String boss) {
        return intervals.getOrDefault(boss, 0);
    }

    private static void load() {
        CampConfig cfg = null;
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                cfg = GSON.fromJson(r, CampConfig.class);
            } catch (Exception ignored) {
            }
        }
        if (cfg == null) cfg = new CampConfig();
        if (cfg.alerts == null) cfg.alerts = new LinkedHashMap<>();
        if (cfg.intervals == null) cfg.intervals = new LinkedHashMap<>();
        for (String b : BOSSES) {
            cfg.alerts.putIfAbsent(b, true);
            cfg.intervals.putIfAbsent(b, DEFAULT_INTERVALS.getOrDefault(b, 0));
        }
        instance = cfg;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception ignored) {
        }
    }
}
