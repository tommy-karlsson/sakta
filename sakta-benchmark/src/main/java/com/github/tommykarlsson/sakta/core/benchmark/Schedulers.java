package com.github.tommykarlsson.sakta.core.benchmark;

import com.github.tommykarlsson.sakta.core.Scheduler;
import com.github.tommykarlsson.sakta.core.impl.ForkJoinPoolScheduler;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;

/**
 * The schedulers the benchmarks compare, named by the parameter that selects them.
 */
final class Schedulers {

    static final String FORK_JOIN = "sakta-forkjoin";
    static final String VIRTUAL_THREADS = "sakta-virtualthreads";

    private Schedulers() {
    }

    static Scheduler create(String what) {
        return switch (what) {
            case FORK_JOIN -> new ForkJoinPoolScheduler();
            case VIRTUAL_THREADS -> new VirtualThreadPerActorScheduler();
            default -> throw new IllegalArgumentException("Unknown scheduler: " + what);
        };
    }
}
