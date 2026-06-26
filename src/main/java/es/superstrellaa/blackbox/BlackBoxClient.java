package es.superstrellaa.blackbox;

import net.fabricmc.api.ClientModInitializer;
import es.superstrellaa.blackbox.config.BlackBoxConfig;
import es.superstrellaa.blackbox.network.WebhookSender;
import es.superstrellaa.blackbox.data.SessionData;
import es.superstrellaa.blackbox.handlers.CrashHandler;
import es.superstrellaa.blackbox.handlers.ConnectionEventHandler;
import es.superstrellaa.blackbox.handlers.LifecycleEventHandler;

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

        CrashHandler.register();
        ConnectionEventHandler.register();
        LifecycleEventHandler.register();
    }
}