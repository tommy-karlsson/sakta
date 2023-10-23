package com.github.tommykarlsson.sakta.cluster;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.tommykarlsson.sakta.core.ActorRef;

public class ClusterActorRefProxy<T> implements ActorRef<T> {

    private final Object address;
    private final MemberService memberService;

    public ClusterActorRefProxy(Object address, MemberService memberService) {
        this.address = address;
        this.memberService = memberService;
    }


    private ActorRef<T> findLocalActorRef() {
        return null;
    }

    @Override
    public void tell(Consumer<T> teller) {
        ActorRef<T> delegate = findLocalActorRef();
        if (delegate != null) {
            delegate.tell(teller);
        } else {
            // send to remote
            memberService.tellOnOwner(address, teller);
        }
    }

    @Override
    public <U> CompletableFuture<U> ask(Function<T, U> asker) {
        return null;
    }

    @Override
    public <U> CompletableFuture<U> flatAsk(Function<T, CompletableFuture<U>> asker) {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
