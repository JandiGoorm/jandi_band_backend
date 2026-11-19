package com.jandi.band_backend;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ApplicationListener;

final class StartupDiagnostics implements ApplicationListener<SpringApplicationEvent>, AutoCloseable {
    private final long started = System.nanoTime();
    private final Thread main = Thread.currentThread();
    private final BufferingApplicationStartup timeline = new BufferingApplicationStartup(2048);
    private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("startup-diagnostics").factory());

    StartupDiagnostics(SpringApplication application) {
        application.setApplicationStartup(timeline);
        application.addListeners(this);
        sampler.scheduleWithFixedDelay(this::sample, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onApplicationEvent(SpringApplicationEvent event) {
        System.out.printf("STARTUP phase=%s elapsed_ms=%d%n",
                event.getClass().getSimpleName(), elapsedMillis());
    }

    private long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private void sample() {
        String frames = Arrays.stream(main.getStackTrace()).limit(12)
                .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                .collect(Collectors.joining(" <- "));
        System.out.printf("STARTUP main_state=%s elapsed_ms=%d stack=%s%n",
                main.getState(), elapsedMillis(), frames);
        logSteps();
    }

    private synchronized void logSteps() {
        for (var event : timeline.drainBufferedTimeline().getEvents()) {
            long duration = event.getDuration().toMillis();
            if (duration < 100) {
                continue;
            }
            var step = event.getStartupStep();
            String bean = "";
            for (var tag : step.getTags()) {
                if ("beanName".equals(tag.getKey())) {
                    bean = tag.getValue();
                }
            }
            System.out.printf("STARTUP step=%s id=%d parent=%s bean=%s start=%s duration_ms=%d%n",
                    step.getName(), step.getId(), step.getParentId(), bean, event.getStartTime(), duration);
        }
    }

    @Override
    public void close() {
        sampler.shutdownNow();
        logSteps();
        timeline.addFilter(step -> false);
    }
}
