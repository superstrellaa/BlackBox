package es.superstrellaa.blackbox.handlers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import es.superstrellaa.blackbox.config.BlackBoxConfig;
import es.superstrellaa.blackbox.data.SessionData;
import es.superstrellaa.blackbox.data.SessionSnapshot;
import es.superstrellaa.blackbox.network.WebhookSender;

public class ConnectionEventHandler {

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SessionData.startServerSession();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            boolean shouldSend = BlackBoxConfig.get().triggers.onDisconnect;
            SessionSnapshot snapshot = shouldSend ? SessionData.snapshot("disconnect") : null;
            SessionData.stopServerSession();
            if (shouldSend) WebhookSender.sendSessionReport(snapshot);
        });
    }
}