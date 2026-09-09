package com.github.tommykarlsson.sakta.core.benchmark;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
public class JmhSequential {

    public final int ACTOR_COUNT = 1_000_000;

    @Param({Schedulers.FORK_JOIN, Schedulers.VIRTUAL_THREADS})
    public String what;

    private ActorSystem actorSystem;

    @Setup(Level.Invocation)
    public void createActorSystem() {
        actorSystem = new ActorSystem(new UnboundedMailboxFactory(), Schedulers.create(what));
    }

    /**
     * Stopping the actors is not what this benchmark is about, so it happens here rather than in the
     * measured method: JMH subtracts what an invocation-level fixture costs. It still has to happen
     * between invocations though, since actors left winding down would land on the next one.
     */
    @TearDown(Level.Invocation)
    public void closeActorSystem() {
        BenchmarkDefaults.closeAndAwait(actorSystem);
    }

    @Benchmark
    @Fork(value = BenchmarkDefaults.FORKS, jvmArgs = {BenchmarkDefaults.MAX_HEAP})
    @Warmup(iterations = BenchmarkDefaults.WARMUP_ITERATIONS)
    @Measurement(iterations = BenchmarkDefaults.MEASUREMENT_ITERATIONS)
    @BenchmarkMode(Mode.AverageTime)
    public void run() {
        SaktaSequential.run(ACTOR_COUNT, actorSystem);
    }
}
