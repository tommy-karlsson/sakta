package com.github.tommykarlsson.sakta.core.benchmark;

import com.github.tommykarlsson.sakta.core.ActorSystem;
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
public class JmhSequential {

    public final int ACTOR_COUNT = 1_000_000;

    @Param({ "sakta-forkjoin" ,"sakta-virtualthreads"})
    public String what;

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    @BenchmarkMode(Mode.AverageTime)
    public void run() {
        switch (what) {
        case "sakta-forkjoin":
            SaktaSequential.run(ACTOR_COUNT, new ActorSystem(new UnboundedMailboxFactory(), new ForkJoinPoolScheduler()));
            break;
        case "sakta-virtualthreads":
            SaktaSequential.run(ACTOR_COUNT, new ActorSystem(new UnboundedMailboxFactory(), new VirtualThreadPerActorScheduler()));
            break;
        }
    }
}
