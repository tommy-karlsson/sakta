package com.github.tommykarlsson.sakta.core.benchmark;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.Scheduler;
import com.github.tommykarlsson.sakta.core.impl.ForkJoinPoolScheduler;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
public class JmhParallelTell {

    public static final int ACTOR_COUNT = 100;
    public static final int MESSAGE_COUNT = 100_000;

    @Param({"sakta-forkjoin", "sakta-virtualthreads"})
    public String what;

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.AverageTime)
    public void run() {
        switch (what) {
        case "sakta-forkjoin":
            run(new ForkJoinPoolScheduler());
            break;
        case "sakta-virtualthreads":
            run(new VirtualThreadPerActorScheduler());
            break;
        }
    }

    private void run(Scheduler scheduler) {
        SaktaMassiveTellToActorGroup.run(MESSAGE_COUNT, ACTOR_COUNT, new ActorSystem(new UnboundedMailboxFactory(), scheduler));
    }
}
