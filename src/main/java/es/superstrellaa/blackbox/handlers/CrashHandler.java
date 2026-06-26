package es.superstrellaa.blackbox.handlers;

import es.superstrellaa.blackbox.BlackBox;
import es.superstrellaa.blackbox.data.SessionData;
import es.superstrellaa.blackbox.network.WebhookSender;

public class CrashHandler {

    public static void register() {
        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            BlackBox.LOGGER.error("BlackBox: uncaught exception on thread {}", thread.getName());
            try {
                WebhookSender.sendErrorReport(
                        throwable,
                        "Uncaught exception on thread: " + thread.getName(),
                        SessionData.snapshot("error")
                );
                WebhookSender.awaitFlush(5000);
            } catch (Exception e) {
                BlackBox.LOGGER.error("BlackBox: failed to report uncaught exception", e);
            }
            if (previousHandler != null) previousHandler.uncaughtException(thread, throwable);
        });
    }
}