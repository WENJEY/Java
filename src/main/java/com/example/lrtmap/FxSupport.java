package com.example.lrtmap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts the JavaFX toolkit once so Map and Search windows can both open
 * from the console Main menu without calling Application.launch twice.
 */
public final class FxSupport extends Application {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final CountDownLatch READY = new CountDownLatch(1);

    /** Required by JavaFX Application.launch — must be public no-arg. */
    public FxSupport() {
    }

    public static void run(Runnable action) {
        ensureStarted();
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static void ensureStarted() {
        if (STARTED.compareAndSet(false, true)) {
            Thread fxThread = new Thread(() -> Application.launch(FxSupport.class), "javafx-launcher");
            fxThread.setDaemon(true);
            fxThread.start();
        }
        try {
            READY.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        primaryStage.hide();
        READY.countDown();
    }
}
