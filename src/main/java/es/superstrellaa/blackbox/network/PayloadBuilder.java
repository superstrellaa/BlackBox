package es.superstrellaa.blackbox.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import es.superstrellaa.blackbox.config.BlackBoxConfig;
import es.superstrellaa.blackbox.data.SessionSnapshot;

import java.time.Instant;

public class PayloadBuilder {

    private static final int COLOR_OK = 0x4ade80;
    private static final int COLOR_DISCONNECT = 0xfacc15;
    private static final int COLOR_ERROR = 0xef4444;

    public static String buildSessionReport(SessionSnapshot s) {
        BlackBoxConfig.DataOptions data = BlackBoxConfig.get().data;

        JsonObject root = new JsonObject();
        root.addProperty("username", "BlackBox");

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        boolean isError = "error".equals(s.reason);
        embed.addProperty("title", isError ? "Crash Report" : "Session Report - " + s.reason);
        embed.addProperty("color", isError ? COLOR_ERROR : "disconnect".equals(s.reason) ? COLOR_DISCONNECT : COLOR_OK);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonArray fields = new JsonArray();

        addField(fields, "Player", s.playerName, true);
        addField(fields, "Session ID", shorten(s.sessionId), true);
        addField(fields, "Duration", s.sessionDurationSeconds + "s", true);

        if (data.fps) {
            addField(fields, "FPS (avg/min/max)", s.fpsAvg + " / " + s.fpsMin + " / " + s.fpsMax, true);
        }

        if (data.memory) {
            addField(fields, "Memory (used/alloc/max)",
                    s.memoryUsedMb + "MB / " + s.memoryAllocatedMb + "MB / " + s.memoryMaxMb + "MB", true);
        }

        if (data.hardware) {
            addField(fields, "OS", s.osName + " " + s.osVersion, true);
            addField(fields, "CPU cores", String.valueOf(s.cpuCores), true);
            addField(fields, "System RAM", s.totalSystemRamMb + "MB", true);
            addField(fields, "GPU", s.gpuRenderer, true);
            addField(fields, "Java", s.javaVersion, true);
        }

        if (data.graphicsSettings) {
            addField(fields, "Render Distance", String.valueOf(s.renderDistance), true);
            addField(fields, "Graphics", s.graphicsMode, true);
            addField(fields, "Max FPS setting", String.valueOf(s.maxFps), true);
            addField(fields, "VSync", String.valueOf(s.vsync), true);
            addField(fields, "Particles", s.particles, true);
            addField(fields, "Entity Shadows", String.valueOf(s.entityShadows), true);
            addField(fields, "Fullscreen", String.valueOf(s.fullscreen), true);
        }

        addField(fields, "Minecraft", s.minecraftVersion, true);
        addField(fields, "Fabric Loader", s.fabricLoaderVersion, true);

        if (data.modsList) {
            addField(fields, "Mods installed", String.valueOf(s.installedMods.size()), true);
        }

        if (isError && s.errorContext != null) {
            addField(fields, "Context", s.errorContext, false);
        }

        embed.add("fields", fields);

        if (isError && s.errorStacktrace != null) {
            String trace = s.errorStacktrace;
            if (trace.length() > 3800) trace = trace.substring(0, 3800) + "\n... (truncated)";
            embed.addProperty("description", "```\n" + trace + "\n```");
        }

        embeds.add(embed);
        root.add("embeds", embeds);
        return root.toString();
    }

    private static void addField(JsonArray fields, String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", (value == null || value.isEmpty()) ? "N/A" : value);
        field.addProperty("inline", inline);
        fields.add(field);
    }

    private static String shorten(String uuid) {
        if (uuid == null) return "N/A";
        return uuid.length() > 8 ? uuid.substring(0, 8) : uuid;
    }
}