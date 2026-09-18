import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Part 2: The Synchronization Trap
 *
 * Fixes the race from Part 1 by making the increment atomic, either with
 * `synchronized` or with AtomicLong.incrementAndGet(). Both are correct
 * (pi comes out ~3.1415), but both are slow: every single hit now forces
 * a trip through the CPU's cache-coherency / memory-bus locking protocol
 * (a CAS or a monitor lock) on a cache line that all 4 threads are fighting
 * over. That "cache line ping-pong" means the cores spend most of their
 * time stalling instead of computing, so 4 threads end up slower than a
 * single thread doing the same work with a plain local variable.
 */
public class Part2_SynchronizationTrap {

    static final long TOTAL_POINTS = 50_000_000L;
    static final int NUM_THREADS = 4;

    // Shared, but now correctly synchronized.
    static final AtomicLong totalHitsAtomic = new AtomicLong(0);
    static long totalHitsSynchronized = 0;

    static synchronized void incrementSynchronized() {
        totalHitsSynchronized++;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Part 2: The Synchronization Trap");
        System.out.println("-------------------------------------------------------");

        long atomicTime = runAtomicVersion();
        long syncTime = runSynchronizedVersion();
        long singleThreadTime = runSingleThreaded();

        System.out.println();
        System.out.println("Summary (" + TOTAL_POINTS + " points):");
        System.out.printf("  Single-threaded, plain local counter : %d ms%n", singleThreadTime);
        System.out.printf("  4 threads, synchronized totalHits++  : %d ms%n", syncTime);
        System.out.printf("  4 threads, AtomicLong.incrementAndGet: %d ms%n", atomicTime);
        System.out.println("  -> both concurrent versions are slower than one thread");
        System.out.println("     because of contention on the shared counter.");
    }

    static long runAtomicVersion() throws InterruptedException {
        totalHitsAtomic.set(0);
        long pointsPerThread = TOTAL_POINTS / NUM_THREADS;

        long start = System.nanoTime();
        Thread[] threads = new Thread[NUM_THREADS];
        for (int t = 0; t < NUM_THREADS; t++) {
            threads[t] = new Thread(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (long i = 0; i < pointsPerThread; i++) {
                    double x = rnd.nextDouble();
                    double y = rnd.nextDouble();
                    if (x * x + y * y <= 1.0) {
                        totalHitsAtomic.incrementAndGet(); // atomic CAS every hit
                    }
                }
            });
            threads[t].start();
        }
        for (Thread th : threads) th.join();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        double pi = 4.0 * totalHitsAtomic.get() / TOTAL_POINTS;
        System.out.printf("AtomicLong version:      pi=%.6f  time=%d ms%n", pi, elapsedMs);
        return elapsedMs;
    }

    static long runSynchronizedVersion() throws InterruptedException {
        totalHitsSynchronized = 0;
        long pointsPerThread = TOTAL_POINTS / NUM_THREADS;

        long start = System.nanoTime();
        Thread[] threads = new Thread[NUM_THREADS];
        for (int t = 0; t < NUM_THREADS; t++) {
            threads[t] = new Thread(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (long i = 0; i < pointsPerThread; i++) {
                    double x = rnd.nextDouble();
                    double y = rnd.nextDouble();
                    if (x * x + y * y <= 1.0) {
                        incrementSynchronized(); // acquires monitor lock every hit
                    }
                }
            });
            threads[t].start();
        }
        for (Thread th : threads) th.join();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        double pi = 4.0 * totalHitsSynchronized / TOTAL_POINTS;
        System.out.printf("synchronized version:    pi=%.6f  time=%d ms%n", pi, elapsedMs);
        return elapsedMs;
    }

    static long runSingleThreaded() {
        long hits = 0; // plain local variable, no locking needed
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        long start = System.nanoTime();
        for (long i = 0; i < TOTAL_POINTS; i++) {
            double x = rnd.nextDouble();
            double y = rnd.nextDouble();
            if (x * x + y * y <= 1.0) {
                hits++;
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        double pi = 4.0 * hits / TOTAL_POINTS;
        System.out.printf("Single-threaded version: pi=%.6f  time=%d ms%n", pi, elapsedMs);
        return elapsedMs;
    }
}
