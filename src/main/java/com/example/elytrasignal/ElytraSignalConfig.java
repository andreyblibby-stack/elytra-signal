package com.example.elytrasignal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ElytraSignalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("elytrasignal.json");

    /** Fire the low-durability signal once remaining durability drops below this percent. */
    public double thresholdPercent = 20.0;

    /** Percent above the threshold required before the signal can fire again (prevents flapping). */
    public double hysteresisPercent = 5.0;

    /** How often (in ticks) to actually check durability. 10 = every half second. */
    public int checkIntervalTicks = 10;

    /**
     * If true, ALSO sends a real outgoing chat message when the signal fires,
     * so tools that only listen for chat-send loader events (e.g. a Pathmind
     * "Wait Until -> Loader Event -> fabric.client.message.send_chat" node)
     * can catch it without any extra bridging.
     *
     * WARNING: this is a real chat message sent to whatever server you are on.
     * It will be visible to the server and, unless you use a client-side-only
     * mod to hide it, to other players. Off by default.
     */
    public boolean bridgeViaChat = false;

    /** The exact text sent when bridgeViaChat is true. Keep it short and recognizable. */
    public String bridgeChatMessage = "!elytra_low";

    public static ElytraSignalConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                ElytraSignalConfig cfg = GSON.fromJson(reader, ElytraSignalConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (IOException e) {
                ElytraSignalMod.LOGGER.warn("Failed to read elytrasignal.json, using defaults", e);
            }
        }
        ElytraSignalConfig cfg = new ElytraSignalConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ElytraSignalMod.LOGGER.warn("Failed to save elytrasignal.json", e);
        }
    }
}
