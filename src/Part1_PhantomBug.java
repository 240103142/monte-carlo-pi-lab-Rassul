import java.util.concurrent.ThreadLocalRandom;

/**
 * Part 1: The Phantom Bug
 *
 * 50,000,000 random (x, y) points are thrown into a 1x1 square, split across
 * 4 native Java threads. All threads increment the SAME shared, unsynchronized
 * variable `totalHits` with a plain `totalHits++`.
 *
 * `totalHits++` is NOT atomic: it is actually three separate steps
 * (read totalHits -> add 1 -> write totalHits back). When two threads
 * interleave those steps, one thread's increment can be silently lost
 * (a "lost update" / data race). With 4 threads hammering the same
 * variable millions of times, a large fraction of increments vanish,
 * so totalHits ends up far too low and the computed pi is wrong and
 * different (and non-reproducible) on every run.
 */
public class Part1_PhantomBug {

    static final long TOTAL_POINTS = 50_000_000L;
    static final int NUM_THREADS = 4;

    // Shared, UNSYNCHRONIZED counter -> data race on purpose.
    static long totalHits = 0;

    public static void main(String[] args) throws InterruptedException {
        int runs = 5;
        System.out.println("Part 1: The Phantom Bug (unsynchronized totalHits++)");
        System.out.println("Points per run: " + TOTAL_POINTS + " | Threads: " + NUM_THREADS);
        System.out.println("-------------------------------------------------------");

        for (int run = 1; run <= runs; run++) {
            totalHits = 0; // reset shared state between runs
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
                            totalHits++; // <-- RACE: read-modify-write, not atomic
                        }
                    }
                });
                threads[t].start();
            }
            for (Thread th : threads) th.join();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            double pi = 4.0 * totalHits / TOTAL_POINTS;
            System.out.printf("Run %d: totalHits=%d  pi=%.6f  time=%d ms%n",
                    run, totalHits, pi, elapsedMs);
        }
    }
}
