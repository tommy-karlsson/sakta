package com.github.tommykarlsson.sakta.core.benchmark;

import java.time.Duration;

import com.github.tommykarlsson.sakta.core.ActorSystem;

/**
 * Settings shared by the benchmarks.
 */
final class BenchmarkDefaults {

    /**
     * One fork, to keep a run short. The cost is that a score which only holds in one JVM has
     * nothing to disagree with, so run-to-run variation does not show up in the error.
     */
    static final int FORKS = 1;

    /** The first invocation in a fresh JVM costs about twice the rest, so it is warmed away. */
    static final int WARMUP_ITERATIONS = 1;

    static final int MEASUREMENT_ITERATIONS = 2;

    /**
     * The workload's live set is about 2 GB at a million actors, so this leaves headroom without
     * inheriting the machine-dependent default maximum, which was a quarter of physical memory.
     */
    static final String MAX_HEAP = "-Xmx3g";

    /**
     * Teardown budget, not a measured quantity: it decides when teardown gives up, and is excluded
     * from the scores. Joining a million virtual threads one at a time measured around nine
     * seconds, so ten left no margin at all.
     */
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    private BenchmarkDefaults() {
    }

    /**
     * Stops the actor system and waits for its actors. Shutting down on its own returns while the
     * actor threads are still winding down, and they then compete with the next invocation, which
     * made roughly one invocation in three run several times slower than the rest.
     */
    static void closeAndAwait(ActorSystem actorSystem) {
        try {
            if (!actorSystem.shutdown(SHUTDOWN_TIMEOUT)) {
                throw new IllegalStateException("Actors had not stopped after " + SHUTDOWN_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
