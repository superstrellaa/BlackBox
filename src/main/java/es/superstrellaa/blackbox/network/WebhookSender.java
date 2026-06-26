package es.superstrellaa.blackbox.network;

import es.superstrellaa.blackbox.BlackBox;
import es.superstrellaa.blackbox.config.BlackBoxConfig;
import es.superstrellaa.blackbox.data.SessionSnapshot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WebhookSender {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final BlockingQueue<String> QUEUE = new LinkedBlockingQueue<>();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicInteger PENDING = new AtomicInteger(0);
    private static Thread workerThread;
    private static final long MIN_INTERVAL_MILLIS = 1500;

    public static synchronized void start() {
        if (RUNNING.get()) return;
        RUNNING.set(true);
        workerThread = new Thread(WebhookSender::workerLoop, "BlackBox-WebhookSender");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static void sendSessionReport(SessionSnapshot snapshot) {
        enqueue(PayloadBuilder.buildSessionReport(snapshot));
    }

    public static void sendErrorReport(Throwable throwable, String context, SessionSnapshot snapshot) {
        snapshot.errorStacktrace = stackTraceToString(throwable);
        snapshot.errorContext = context;
        enqueue(PayloadBuilder.buildSessionReport(snapshot));
    }

    private static void enqueue(String jsonPayload) {
        if (!BlackBoxConfig.get().enabled) return;
        String url = BlackBoxConfig.get().webhookUrl;
        if (url == null || url.isBlank()) return;
        if (!RUNNING.get()) start();
        PENDING.incrementAndGet();
        QUEUE.offer(jsonPayload);
    }

    private static void workerLoop() {
        long lastSendMillis = 0;
        while (RUNNING.get() || !QUEUE.isEmpty()) {
            try {
                String payload = QUEUE.poll(2, java.util.concurrent.TimeUnit.SECONDS);
                if (payload == null) continue;

                long waitTime = MIN_INTERVAL_MILLIS - (System.currentTimeMillis() - lastSendMillis);
                if (waitTime > 0) Thread.sleep(waitTime);

                lastSendMillis = System.currentTimeMillis();
                try {
                    sendWithRetry(payload, 3);
                } finally {
                    PENDING.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void sendWithRetry(String payload, int attemptsLeft) {
        if (attemptsLeft <= 0) return;
        try {
            String url = BlackBoxConfig.get().webhookUrl;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                double retryAfter = Double.parseDouble(response.headers().firstValue("Retry-After").orElse("1"));
                Thread.sleep((long) (retryAfter * 1000));
                sendWithRetry(payload, attemptsLeft - 1);
                return;
            }
            if (response.statusCode() >= 400) {
                BlackBox.LOGGER.error("BlackBox webhook error {} -> {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            sendWithRetry(payload, attemptsLeft - 1);
        }
    }

    private static String stackTraceToString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    public static void awaitFlush(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (PENDING.get() > 0 && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    public static void shutdown() {
        RUNNING.set(false);
        if (workerThread != null) {
            try { workerThread.join(Duration.ofSeconds(15).toMillis()); } catch (InterruptedException ignored) {}
        }
    }
}