package es.superstrellaa.blackbox.data;

import java.util.List;

public class SessionSnapshot {

    public String sessionId;
    public String reason;
    public long sessionDurationSeconds;
    public String playerName;

    public int fpsAvg;
    public int fpsMin;
    public int fpsMax;

    public long memoryUsedMb;
    public long memoryAllocatedMb;
    public long memoryMaxMb;

    public String osName;
    public String osVersion;
    public int cpuCores;
    public long totalSystemRamMb;
    public String javaVersion;
    public String gpuRenderer;

    public int renderDistance;
    public String graphicsMode;
    public int maxFps;
    public boolean vsync;
    public String particles;
    public boolean entityShadows;
    public int mipmapLevels;
    public boolean fullscreen;
    public int guiScale;

    public String minecraftVersion;
    public String fabricLoaderVersion;
    public List<String> installedMods;

    public String errorStacktrace;
    public String errorContext;
}