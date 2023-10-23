# Sakta

Simple Actors

## Design

The framework is designed with simplicity and developer experience in mind. The actor system is built around higher
order functions.

An actor is a POJO, there is no need to extend any class, or depend on the sakta framework in any other way in the
actor. Ask/tell on an actor is simply a method invocation. Reply to `ask` is simply done by returning the result from
the called method. The user of an actor will receive the result from `ask` in a `CompletableFuture` since the actor
invocation is asynchronous by nature. Creation of the actor itself is left to the user of the framework, to allow 
injection of any dependencies etc. into the actor.

The implementation leverages virtual threads, and therefore requires Java 21 (or later).

One of the implementation goals is to _not_ produce excessive amounts of garbage on the heap. Sakta is however not
garbage free (or close to garbage free), but relatively low-garbage.

There are _no_ magic static methods in sakta to e.g. get the current actor ref, or some context, or the sender of a
message, etc. There are also _no_ `ThreadLocal`s in sakta.

There are _no_ dependencies in `sakta-core`, there are however integrations with Spring Boot and Micrometer, but those
are provided by the optional modules `sakta-spring-boot` and `sakta-micrometer` respectively.

## Examples

```java
public class CounterActor {
    private int count = 0;

    void add(int amount) {
        this.count += amount;
    }

    int getCount() {
        return count;
    }
}
```

```java
public class CounterService {
    private final ActorSystem actorSystem = new ActorSystem();

    public void demo() {
        ActorRef<CounterActor> counterRef = actorSystem.getOrCreateActorRef("my-counter", CounterActor::new, CounterActor.class);

        counterRef.tell(c -> c.add(5));
        counterRef.tell(c -> c.add(13));
        int count = counterRef.ask(CounterActor::getCount).get();
    }
}
```

## Schedulers

The framework provides only two schedulers;

### VirtualThreadPerActorScheduler

This scheduler starts one virtual thread per actor, and then processes all messages to the actor in that thread. I.e.
the mailbox processing is single-threaded by design, and all messages are not only processed _serially_, but also on a
single thread.

This is the default scheduler (used if no scheduler is provided when creating the `ActorSystem`).

### ForkJoinPoolScheduler

This scheduler runs all actions on the common `ForkJoinPool`. Messages are processed serially per actor, but there is
no guarantee that all messages to a given actor are processed on a single thread. This scheduler may be a good option
in cases where _large_ amount of _short-lived_ actors are created.

## Observability

Observability is provided by the `sakta-micrometer` module. It provides timers on how long each item stays in queue
in a mailbox, and timers on the actual execution of actions on the actors.

## Limitations

- No supervisor
- Limited number of mailbox and scheduler types provided by the framework
- Scheduler cannot be set "per actor" - all actors in an `ActorSystem` share scheduler (but multiple `ActorSystem`
  instances can co-exist).

## Performance

While "sakta" is Swedish for "slow", the sakta framework is not really that slow - in fact it is considerably faster 
than [akka](https://github.com/akka/akka)/[pekko](https://pekko.apache.org/) and even faster than 
[actr](https://github.com/zakgof/actr/tree/master).
