package es.superstrellaa.blackbox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import es.superstrellaa.blackbox.config.BlackBoxConfig;
import es.superstrellaa.blackbox.network.WebhookSender;
import es.superstrellaa.blackbox.data.SessionData;
import es.superstrellaa.blackbox.data.SessionSnapshot;

public class BlackBoxClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlackBox.LOGGER.info("BlackBox Client initialized!");

        if (!BlackBoxConfig.get().enabled) {
            BlackBox.LOGGER.warn("BlackBox is disabled in config, no telemetry will be sent.");
            return;
        }

        WebhookSender.start();
        SessionData.startSession();

        Thread.UncaughtExceptionHandler previousHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            BlackBox.LOGGER.error("BlackBox: uncaught exception detected on thread {}", thread.getName());

            try {
                WebhookSender.sendErrorReport(
                        throwable,
                        "Uncaught exception on thread: " + thread.getName(),
                        SessionData.snapshot("error")
                );
            } catch (Exception e) {
                BlackBox.LOGGER.error("BlackBox: failed to report uncaught exception", e);
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            BlackBox.LOGGER.info("BlackBox: joined a server, starting server session tracker");
            SessionData.startServerSession();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BlackBox.LOGGER.info("BlackBox: disconnect detected, sending session report");
            SessionSnapshot snapshot = SessionData.snapshot("disconnect");
            SessionData.stopServerSession();
            WebhookSender.sendSessionReport(snapshot);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            BlackBox.LOGGER.info("BlackBox: game stopping, sending final report");
            WebhookSender.sendSessionReport(SessionData.snapshot("game_close"));
            WebhookSender.shutdown();
        });
    }
}