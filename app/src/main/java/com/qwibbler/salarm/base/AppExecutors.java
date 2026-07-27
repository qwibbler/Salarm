// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.base;

import android.os.Handler;
import android.os.Looper;

import com.qwibbler.salarm.utils.LogUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global executors for the application to handle multi-threading.
 *
 * <p>This class provides a centralized way to execute background tasks and UI operations,
 * preventing memory leaks and excessive thread creation.</p>
 */
public class AppExecutors {

    /**
     * Single-thread asynchronous executor.
     * Ensures that all background tasks are executed sequentially (one after another)
     * in the order they are submitted.
     */
    private static final ExecutorService diskIO = Executors.newSingleThreadExecutor();

    /**
     * Multi-thread asynchronous executor for network-bound or blocking operations
     * that don't need to be sequential.
     */
    private static final ExecutorService networkIO = Executors.newFixedThreadPool(4);

    /**
     * Handler attached to the application's main thread (UI Thread).
     * Safely sends tasks to the user interface from a background thread.
     */
    private static final Handler mainThread = new Handler(Looper.getMainLooper());

    /**
     * Returns the executor dedicated to heavy or blocking background operations.
     *
     * <p>Ideal for:
     * <ul>
     *   <li>Database queries or deletions (e.g., in AlarmClockFragment)</li>
     *   <li>Any long-running task that shouldn't block the UI Thread</li>
     * </ul>
     *
     * @return {@link ExecutorService} configured on a single background thread.
     */
    public static ExecutorService getDiskIO() {
        return diskIO;
    }

    /**
     * Returns the executor dedicated to network-bound or blocking operations.
     */
    public static ExecutorService getNetworkIO() {
        return networkIO;
    }

    /**
     * Returns the global Handler for the Main Thread.
     * Replaces the repetitive creation of {@code new Handler(Looper.getMainLooper())} in the codebase.
     *
     * <p>Ideal for:
     * <ul>
     *   <li>Updating views or restarting Loaders (e.g., {@code restartLoader})</li>
     *   <li>Displaying Toasts or Snackbars</li>
     *   <li>Executing UI-related delayed tasks (animations, activity recreation)</li>
     * </ul>
     *
     * @return {@link Handler} attached to the Main Looper.
     */
    public static Handler getMainThread() {
        return mainThread;
    }

    /**
     * Executes a task on the main thread and returns its result, blocking the current background thread.
     * Useful for fetching AOSP settings that are protected by enforceMainLooper().
     */
    public static <T> T getOnMainThread(Callable<T> callable) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException("Error executing task on main thread", e);
            }
        }

        final AtomicReference<T> result = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        mainThread.post(() -> {
            try {
                result.set(callable.call());
            } catch (Exception e) {
                error.set(e);
                LogUtils.e("Error on main thread", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            LogUtils.e("Interrupted while waiting for main thread", e);
            Thread.currentThread().interrupt();
        }

        if (error.get() != null) {
            throw new RuntimeException("Failed to fetch data from main thread", error.get());
        }

        return result.get();
    }
}
