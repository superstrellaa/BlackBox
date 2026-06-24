package es.superstrellaa.blackbox.data;

import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import es.superstrellaa.blackbox.BlackBox;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionData {

    private static String sessionId;
    private static long sessionStartMillis;

    private static int frameCountThisSecond = 0;
    private static long lastFpsSampleMillis = 0;

    private static int fpsMin = Integer.MAX_VALUE;
    private static int fpsMax = 0;
    private static long fpsSampleSum = 0;
    private static int fpsSampleCount = 0;

    private static boolean started = false;

    private static volatile String cachedGpuRenderer = "unknown";
    private static volatile boolean gpuRendererCaptured = false;

    public static void startSession() {
        if (started) return;
        started = true;

        sessionId = UUID.randomUUID().toString();
        sessionStartMillis = System.currentTimeMillis();
        lastFpsSampleMillis = System.currentTimeMillis();

        WorldRenderEvents.END.register(context -> {
            if (!gpuRendererCaptured) {
                try {
                    cachedGpuRenderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
                } catch (Exception e) {
                    cachedGpuRenderer = "unknown";
                }
                gpuRendererCaptured = true;
            }

            frameCountThisSecond++;

            long now = System.currentTimeMillis();
            if (now - lastFpsSampleMillis >= 1000) {
                int fps = frameCountThisSecond;
                fpsMin = Math.min(fpsMin, fps);
                fpsMax = Math.max(fpsMax, fps);
                fpsSampleSum += fps;
                fpsSampleCount++;

                frameCountThisSecond = 0;
                lastFpsSampleMillis = now;
            }
        });

        BlackBox.LOGGER.info("BlackBox session started: {}", sessionId);
    }

    public static SessionSnapshot snapshot(String reason) {
        SessionSnapshot s = new SessionSnapshot();
        MinecraftClient client = MinecraftClient.getInstance();

        s.sessionId = sessionId;
        s.reason = reason;
        s.sessionDurationSeconds = (System.currentTimeMillis() - sessionStartMillis) / 1000;
        s.playerName = client.getSession() != null ? client.getSession().getUsername() : "unknown";

        // FPS
        s.fpsAvg = fpsSampleCount > 0 ? (int) (fpsSampleSum / fpsSampleCount) : 0;
        s.fpsMin = fpsMin == Integer.MAX_VALUE ? 0 : fpsMin;
        s.fpsMax = fpsMax;

        // Memoria
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        s.memoryUsedMb = usedBytes / (1024 * 1024);
        s.memoryAllocatedMb = runtime.totalMemory() / (1024 * 1024);
        s.memoryMaxMb = runtime.maxMemory() / (1024 * 1024);

        // Hardware
        s.osName = System.getProperty("os.name");
        s.osVersion = System.getProperty("os.version");
        s.javaVersion = System.getProperty("java.version");

        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            s.cpuCores = osBean.getAvailableProcessors();
            s.totalSystemRamMb = osBean.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) {
            // Fallback si com.sun.management no está disponible en esta JVM
            s.cpuCores = Runtime.getRuntime().availableProcessors();
            s.totalSystemRamMb = -1;
        }

        s.gpuRenderer = cachedGpuRenderer;

        var options = client.options;
        s.renderDistance = options.getViewDistance().getValue();
        s.graphicsMode = options.getGraphicsMode().getValue().toString();
        s.maxFps = options.getMaxFps().getValue();
        s.vsync = options.getEnableVsync().getValue();
        s.particles = options.getParticles().getValue().toString();
        s.entityShadows = options.getEntityShadows().getValue();
        s.mipmapLevels = options.getMipmapLevels().getValue();
        s.fullscreen = options.getFullscreen().getValue();
        s.guiScale = options.getGuiScale().getValue();

        s.minecraftVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        s.fabricLoaderVersion = FabricLoader.getInstance().getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        List<String> mods = FabricLoader.getInstance().getAllMods().stream()
                .map(c -> c.getMetadata().getId() + "@" + c.getMetadata().getVersion().getFriendlyString())
                .collect(Collectors.toList());
        s.installedMods = mods;

        return s;
    }

    public static String getSessionId() {
        return sessionId;
    }
}