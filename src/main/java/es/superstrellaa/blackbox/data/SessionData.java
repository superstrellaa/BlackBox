package es.superstrellaa.blackbox.data;

import com.sun.management.OperatingSystemMXBean;
import es.superstrellaa.blackbox.config.BlackBoxConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import es.superstrellaa.blackbox.BlackBox;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionData {

    private static String sessionId;
    private static boolean started = false;

    private static volatile String cachedGpuRenderer = "unknown";
    private static volatile boolean gpuRendererCaptured = false;

    private static final FpsTracker gameTracker = new FpsTracker();
    private static final FpsTracker serverTracker = new FpsTracker();

    public static void startSession() {
        if (started) return;
        started = true;

        sessionId = UUID.randomUUID().toString();
        gameTracker.start();

        WorldRenderEvents.END.register(context -> {
            if (!gpuRendererCaptured) {
                try {
                    cachedGpuRenderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
                } catch (Exception e) {
                    cachedGpuRenderer = "unknown";
                }
                gpuRendererCaptured = true;
            }

            gameTracker.onFrame();
            serverTracker.onFrame();
        });

        BlackBox.LOGGER.info("BlackBox game session started: {}", sessionId);
    }

    public static void startServerSession() {
        serverTracker.start();
        BlackBox.LOGGER.info("BlackBox server session started");
    }

    public static void stopServerSession() {
        serverTracker.stop();
        BlackBox.LOGGER.info("BlackBox server session stopped");
    }

    public static SessionSnapshot snapshot(String reason) {
        SessionSnapshot s = new SessionSnapshot();
        MinecraftClient client = MinecraftClient.getInstance();

        FpsTracker tracker = "game_close".equals(reason)
                ? gameTracker
                : (serverTracker.everStarted() ? serverTracker : gameTracker);

        s.sessionId = sessionId;
        s.reason = reason;
        s.sessionDurationSeconds = tracker.getDurationSeconds();
        s.playerName = BlackBoxConfig.get().anonymous
                ? "Anonymous"
                : (client.getSession() != null ? client.getSession().getUsername() : "unknown");

        s.fpsAvg = tracker.getAvg();
        s.fpsMin = tracker.getMin();
        s.fpsMax = tracker.getMax();

        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        s.memoryUsedMb = usedBytes / (1024 * 1024);
        s.memoryAllocatedMb = runtime.totalMemory() / (1024 * 1024);
        s.memoryMaxMb = runtime.maxMemory() / (1024 * 1024);

        s.osName = System.getProperty("os.name");
        s.osVersion = System.getProperty("os.version");
        s.javaVersion = System.getProperty("java.version");

        try {
            OperatingSystemMXBean osBean =
                    (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            s.cpuCores = osBean.getAvailableProcessors();
            s.totalSystemRamMb = osBean.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) {
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

    private static class FpsTracker {

        private long startMillis;
        private boolean active = false;
        private boolean everStarted = false;

        private int frameCountThisSecond = 0;
        private long lastSampleMillis;

        private int fpsMin = Integer.MAX_VALUE;
        private int fpsMax = 0;
        private long fpsSampleSum = 0;
        private int fpsSampleCount = 0;

        synchronized void start() {
            startMillis = System.currentTimeMillis();
            lastSampleMillis = startMillis;
            frameCountThisSecond = 0;
            fpsMin = Integer.MAX_VALUE;
            fpsMax = 0;
            fpsSampleSum = 0;
            fpsSampleCount = 0;
            active = true;
            everStarted = true;
        }

        synchronized void stop() {
            active = false;
        }

        synchronized void onFrame() {
            if (!active) return;

            frameCountThisSecond++;

            long now = System.currentTimeMillis();
            if (now - lastSampleMillis >= 1000) {
                int fps = frameCountThisSecond;
                fpsMin = Math.min(fpsMin, fps);
                fpsMax = Math.max(fpsMax, fps);
                fpsSampleSum += fps;
                fpsSampleCount++;

                frameCountThisSecond = 0;
                lastSampleMillis = now;
            }
        }

        synchronized int getAvg() {
            return fpsSampleCount > 0 ? (int) (fpsSampleSum / fpsSampleCount) : 0;
        }

        synchronized int getMin() {
            return fpsMin == Integer.MAX_VALUE ? 0 : fpsMin;
        }

        synchronized int getMax() {
            return fpsMax;
        }

        synchronized long getDurationSeconds() {
            return (System.currentTimeMillis() - startMillis) / 1000;
        }

        synchronized boolean everStarted() {
            return everStarted;
        }
    }
}