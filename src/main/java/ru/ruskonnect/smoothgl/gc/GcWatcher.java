package ru.ruskonnect.smoothgl.gc;

import com.sun.management.GarbageCollectionNotificationInfo;
import ru.ruskonnect.smoothgl.SmoothGL;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Subscribes to JVM GC notifications and logs every pause longer than
 * {@link #LONG_PAUSE_MS}. This does not "fix" GC pauses — that is a JVM
 * tuning concern (see README for flags) — but it tells the truth about
 * which GCs are causing micro-freezes, so the user can verify whether
 * their JVM flags are actually working.
 */
public final class GcWatcher {

    private static final long LONG_PAUSE_MS = 20L;

    private final List<NotificationEmitter> registered = new ArrayList<>();
    private final AtomicLong totalPauseMs = new AtomicLong();
    private final AtomicLong longPauseCount = new AtomicLong();

    public void install() {
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (!(bean instanceof NotificationEmitter emitter)) continue;
            emitter.addNotificationListener(this::onNotification, null, null);
            registered.add(emitter);
        }
        SmoothGL.LOGGER.info("GC watcher installed on {} collector(s)", registered.size());
    }

    private void onNotification(Notification notification, Object handback) {
        if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
                .equals(notification.getType())) {
            return;
        }
        GarbageCollectionNotificationInfo info =
            GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        long pauseMs = info.getGcInfo().getDuration();
        totalPauseMs.addAndGet(pauseMs);
        if (pauseMs >= LONG_PAUSE_MS) {
            longPauseCount.incrementAndGet();
            SmoothGL.LOGGER.warn("[gc] {} ({}) paused {}ms — cause: {}",
                info.getGcName(), info.getGcAction(), pauseMs, info.getGcCause());
        }
    }

    public long totalPauseMs() { return totalPauseMs.get(); }
    public long longPauseCount() { return longPauseCount.get(); }
}
