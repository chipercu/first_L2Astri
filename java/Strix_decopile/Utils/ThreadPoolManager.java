package Strix_decopile.Utils;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    private static final long MAX_DELAY;
    private static final ThreadPoolManager INSTANCE;
    private final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    private final ThreadPoolExecutor executor;

    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    private ThreadPoolManager() {
        this.executor = new ThreadPoolExecutor(1, 1, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        this.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ThreadPoolManager.this.executor.purge();
                ThreadPoolManager.this.scheduledExecutor.purge();
            }
        }, 600000L, 600000L);
    }

    private long validate(long delay) {
        return Math.max(0L, Math.min(MAX_DELAY, delay));
    }

    public void execute(Runnable r) {
        this.executor.execute(r);
    }

    public ScheduledFuture<?> schedule(Runnable r, long delay) {
        return this.scheduledExecutor.schedule(r, this.validate(delay), TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay) {
        return this.scheduledExecutor.scheduleAtFixedRate(r, this.validate(initial), this.validate(delay), TimeUnit.MILLISECONDS);
    }

    static {
        MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2L;
        INSTANCE = new ThreadPoolManager();
    }
}
