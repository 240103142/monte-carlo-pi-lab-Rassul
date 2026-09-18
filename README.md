# Monte Carlo π — Concurrency Lab

Java source lives in `src/`. No external dependencies — just a JDK (11+).

## How to run

```bash
cd src
javac Part1_PhantomBug.java && java Part1_PhantomBug
javac Part2_SynchronizationTrap.java && java Part2_SynchronizationTrap
javac Part3_Reduction.java && java Part3_Reduction
```

> **Note:** `Part3_Reduction` should be run on *your own machine* — the runtimes
> are meant to reflect your CPU's actual core count and memory system, so the
> grid below is a template to fill in yourself, not pre-filled data.

---

## Part 1 — The Phantom Bug

5 runs, `totalHits++` unsynchronized across 4 threads. Expect π ≈ 1.8–2.4,
and a **different** wrong value each run, because the outcome depends on
how the threads happen to interleave.

| Run | π (your result) |
|-----|------------------|
| 1   |                  |
| 2   |                  |
| 3   |                  |
| 4   |                  |
| 5   |                  |

## Part 2 — The Synchronization Trap

| Version                              | Time (ms) |
|---------------------------------------|-----------|
| Single-threaded (plain local counter) |           |
| 4 threads, `synchronized`             |           |
| 4 threads, `AtomicLong`                |           |

## Part 3 — OpenMP-Style Reduction

Fill in after running `Part3_Reduction` on your machine (100,000,000 points):

| Threads (T) | Runtime (ms) | Speedup (T1/TN) | Efficiency (Speedup/T) |
|-------------|---------------|------------------|--------------------------|
| 1 (baseline)|               | 1.0x             | 100%                     |
| 2           |               |                  |                           |
| 4           |               |                  |                           |
| 8           |               |                  |                           |
| 16          |               |                  |                           |
| 32          |               |                  |                           |

---

## Questions

**1. Look at your row for 16 threads. Why didn't your 8-core CPU run twice as
fast as 8 threads?**

Past the physical core count, extra "threads" don't add extra execution
units — they're scheduled onto the same 8 cores via OS time-slicing (or
onto sibling hyperthreads, which share a core's execution ports and caches
rather than doubling them). So 16 threads on an 8-core CPU mostly adds
context-switch overhead and cache contention instead of parallel throughput:
you get some benefit from hyperthreading hiding memory-latency stalls, but
nowhere near a linear 2x, and efficiency (speedup/T) visibly drops. This is
why the efficiency column keeps falling as T grows past the core count —
it's the practical face of Amdahl's/Gustafson's law plus real hardware
contention (shared caches, shared memory bandwidth, OS scheduling overhead).

**2. Why was the synchronized version in Part 2 slower than running on one
single core?**

Every `totalHits++` now has to be atomic, which forces each of the 4 threads
to serialize on the same memory location: acquiring a monitor lock (or doing
a CAS with AtomicLong) means the CPU has to invalidate that cache line on
every other core and pull the current value across the memory bus/cache
coherency protocol before it can proceed. Because hits happen very
frequently (millions of times), the threads spend the overwhelming majority
of their time stalling, waiting for exclusive ownership of that one cache
line, rather than doing useful work — that's the "95% of cycles stalling on
memory bus locks" from the assignment. A single thread never has this
problem: its local counter lives in a register/cache line that no one else
touches, so every increment is essentially free. Adding threads to a
workload that is dominated by contention on shared state makes it *slower*,
not faster — the fix isn't "more careful locking," it's Part 3's approach of
avoiding the shared write entirely until the very end.

---

## Publishing to Git

This folder is ready to become the class Git repo. From this directory:

```bash
git init
git add .
git commit -m "Monte Carlo pi: race condition, sync trap, lock-free reduction"
git branch -M main
git remote add origin <YOUR_NEW_REPO_URL>   # create an empty repo on GitHub/GitLab first
git push -u origin main
```

Then share repo access with **@sufyanism** (Settings → Collaborators on
GitHub, or Members on GitLab) before class end-time.
