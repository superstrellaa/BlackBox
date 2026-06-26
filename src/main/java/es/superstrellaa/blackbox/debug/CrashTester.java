package es.superstrellaa.blackbox.debug;

import net.minecraft.client.MinecraftClient;
import es.superstrellaa.blackbox.network.WebhookSender;
import es.superstrellaa.blackbox.data.SessionData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrashTester {

    public static void scheduleTestCrash() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BlackBox-CrashTester");
            t.setDaemon(true);
            return t;
        });

        scheduler.schedule(() -> {
            MinecraftClient.getInstance().execute(() -> {
                WebhookSender.sendErrorReport(
                        new RuntimeException("BlackBox simulated test crash"),
                        "Scheduled test crash after 20s",
                        SessionData.snapshot("error")
                );
                WebhookSender.awaitFlush(8000);
                throw new RuntimeException("BlackBox simulated test crash");
            });
            scheduler.shutdown();
        }, 20, TimeUnit.SECONDS);
    }
}