package jsonic.controller;

/**
 * Fixed-timestep game loop: calls onTick() 60 times per second on its own
 * thread, using a time accumulator to catch up with extra ticks if a frame
 * takes too long, instead of drifting out of sync with real time.
 */
public class GameLoop implements Runnable {

    // callback interface
    public interface TickListener {
        void onTick(); // implemented by GameEngine
    }

    // configuration
    private static final int TARGET_FPS = 60;
    private static final double NANOS_PER_TICK = 1_000_000_000.0 / TARGET_FPS;

    // internal state
    private final TickListener listener;
    private Thread gameThread;
    private volatile boolean running = false;

    // FPS counter, updated every second
    private volatile int currentFPS = 0;

    // constructor

    public GameLoop(TickListener listener) {
        this.listener = listener;
    }

    // start / stop

    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    public void stop() {
        running = false;
        if (gameThread != null) {
            try { gameThread.join(1000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // loop

    @Override
    public void run() {
        double delta = 0.0;
        long lastTime = System.nanoTime();
        long fpsTimer = 0L;
        int tickCount = 0;

        while (running) {
            long currentTime = System.nanoTime();
            long elapsed = currentTime - lastTime;
            lastTime = currentTime;

            delta += elapsed / NANOS_PER_TICK;
            fpsTimer += elapsed;

            // Catches up on accumulated lag, capped at 5 ticks so a very
            // slow system slows down visibly instead of freezing here forever.
            int catchUpCap = 0;
            while (delta >= 1.0 && catchUpCap < 5) {
                listener.onTick();
                delta--;
                catchUpCap++;
                tickCount++;
            }

            if (fpsTimer >= 1_000_000_000L) {
                currentFPS = tickCount;
                tickCount = 0;
                fpsTimer -= 1_000_000_000L;
            }

            // Sleeps for the time left before the next tick; Thread.yield()
            // wouldn't actually block, it would just spin the CPU at 100%.
            if (delta < 1.0) {
                long remainingNanos = (long) ((1.0 - delta) * NANOS_PER_TICK);
                long remainingMillis = remainingNanos / 1_000_000L;
                if (remainingMillis > 0) {
                    try {
                        Thread.sleep(remainingMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    Thread.yield();
                }
            }
        }
    }

    // getter

    public int getCurrentFPS() { return currentFPS; }
}
