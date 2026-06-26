package es.superstrellaa.blackbox.handlers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import es.superstrellaa.blackbox.data.SessionData;
import es.superstrellaa.blackbox.network.WebhookSender;

public class LifecycleEventHandler {

    public static void register() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            WebhookSender.sendSessionReport(SessionData.snapshot("game_close"));
            WebhookSender.shutdown();
        });
    }
}