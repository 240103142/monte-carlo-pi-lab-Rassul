import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Part 3: OpenMP-Style Reduction
 *
 * Each thread accumulates hits into its OWN local (register/stack) variable
 * with no locking and no shared writes during the loop. Partial sums are
 * combined once, after all threads finish (the same pattern as OpenMP's
 * `reduction(+:totalHits)`). This removes all contention on the counter,
 * so the code scales with thread count instead of collapsing under lock
 * contention like Part 2.
 *
 * Benchmarks total wall-clock time for T in {1, 2, 4, 8, 16, 32} threads
 * with 100,000,000 total iterations, and prints a
 * Threads | Runtime(ms) | Speedup | Efficiency table.
 *
 * Run with: java Part3_Reduction
 */
public class Part3_Reduction {

    static final long TOTAL_POINTS = 100_000_000L;
    static final int[] THREAD_COUNTS = {1, 2, 4, 8, 16, 32};

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Part 3: OpenMP-Style Reduction (thread-local counters)");
        System.out.println("Total points: " + TOTAL_POINTS);
        System.out.println("-------------------------------------------------------");

        long baselineMs = -1;
        System.out.println();
        System.out.printf("%-10s %-14s %-16s %-10s%n", "Threads", "Runtime(ms)", "Speedup(T1/TN)", "Efficiency");

        for (int t : THREAD_COUNTS) {
            long ms = runReduction(t);
            if (t == 1) baselineMs = ms;

            double speedup = (double) baselineMs / ms;
            double efficiency = speedup / t * 100.0;
            System.out.printf("%-10d %-14d %-16.2fx %-9.1f%%%n", t, ms, speedup, efficiency);
        }
    }

    static long runReduction(int numThreads) throws InterruptedException {
        long pointsPerThread = TOTAL_POINTS / numThreads;
        long remainder = TOTAL_POINTS % numThreads; // give leftover points to last thread

        Thread[] threads = new Thread[numThreads];
        long[] localHits = new long[numThreads]; // one slot per thread, no false-sharing-safe
                                                   // padding for simplicity; fine for a class demo

        long start = System.nanoTime();
        for (int t = 0; t < numThreads; t++) {
            final int idx = t;
            final long iterations = pointsPerThread + (t == numThreads - 1 ? remainder : 0);
            threads[t] = new Thread(() -> {
                long hits = 0; // thread-private local accumulator, no synchronization needed
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (long i = 0; i < iterations; i++) {
                    double x = rnd.nextDouble();
                    double y = rnd.nextDouble();
                    if (x * x + y * y <= 1.0) {
                        hits++;
                    }
                }
                localHits[idx] = hits; // single write at the very end
            });
            threads[t].start();
        }
        for (Thread th : threads) th.join();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Combine partial sums once, after all threads have finished.
        long totalHits = 0;
        for (long h : localHits) totalHits += h;

        double pi = 4.0 * totalHits / TOTAL_POINTS;
        System.out.printf("  [T=%-2d] pi=%.6f  time=%d ms%n", numThreads, pi, elapsedMs);
        return elapsedMs;
    }
}
