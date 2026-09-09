package com.github.tommykarlsson.sakta.core.benchmark;

import java.time.Duration;

import com.github.tommykarlsson.sakta.core.ActorSystem;

/**
 * Settings shared by the benchmarks.
 */
final class BenchmarkDefaults {

    /** Two forks, so that a score which only holds in one JVM shows up as error rather than as fact. */
    static final int FORKS = 2;

    /** The first invocation in a fresh JVM costs about twice the rest, so it is warmed away. */
    static final int WARMUP_ITERATIONS = 3;

    static final int MEASUREMENT_ITERATIONS = 6;

    /**
     * The workload's live set is about 2 GB at a million actors, so this leaves headroom without
     * inheriting the machine-dependent default maximum, which was a quarter of physical memory.
     */
    static final String MAX_HEAP = "-Xmx3g";

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofMinutes(2);

    private BenchmarkDefaults() {
    }

    /**
     * Closes the actor system and waits for its actors. {@link ActorSystem#close()} on its own
     * returns while the actor threads are still winding down, and they then compete with the next
     * invocation, which made roughly one invocation in three run several times slower than the rest.
     */
    static void closeAndAwait(ActorSystem actorSystem) {
        try {
            if (!actorSystem.close(SHUTDOWN_TIMEOUT)) {
                throw new IllegalStateException("Actors had not stopped after " + SHUTDOWN_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
