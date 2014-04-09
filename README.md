EventBus
========

A general-purpose Java event bus / dispatching library.

Quickstart
----------

_These examples make use of [Lombok](http://projectlombok.org/) annotations for brevity._

### Define Events
Events are simple Java objects that extend an `Event` class. There are no restrictions about what can be stored in them.
```java
import org.timothyb89.eventbus.Event;

@Data
@EqualsAndHashCode(callSuper = false)
public class SomeEvent extends Event {

  private final String someString;
  private final Object someObject;

}
```


### Make an EventBus

EventBus instances only need to have a list of event classes the may need to process. To add an event type, use `EventBus.add(Class)`.

```java
import org.timothyb89.eventbus.*;

public class SomeEventProducer implements EventBusProvider {

  private EventBus bus;
  
  public SomeEventProducer() {
    bus = new EventBus() {{
      add(SomeEvent.class);
      // any number of events here
    }};
  }
  
  public EventBusClient bus() {
    return bus.getClient();
  }
  
  public void triggerSomeEvent() {
    bus.push(new SomeEvent("some parameter", new Object()));
  }

}
```

It's not required to implement `EventBusProvider`, but it can help to provide a consistent interface. The `EventBusClient` class only exposes event registration / deregistration functionality.

### Register to receive events

First, flag any number of methods with the `@EventHandler` annotation. The type of event handled by a method is determined by its parameter:

```java
import org.timothyb89.eventbus.*;

public class SomeEventReceiver {

  @EventHandler
  public void someEventOccurred(SomeEvent event) {
    System.out.println("an event occurred!");
  }

}
```

Then, register for events:

```java
SomeEventProducer producer = new SomeEventProducer();

SomeEventReceiver receiver = new SomeEventReceiver();
producer.bus().register(receiver);

producer.triggerSomeEvent();
// -> "an event occurred!"
```


Example
-------
For a complete working example, see [the demo package](https://github.com/timothyb89/EventBus/tree/master/src/main/java/org/timothyb89/eventbus/demo).

FAQ
---

### Is it thread-safe?
It should be safe to use between multiple threads, but currently events are distributed on the same thread as the caller. Unintential locking may occur if event listeners attempt to perform long-running tasks or do not otherwise return quickly.

In the future more event processing implementations may be introduced, allowing events to be processed in a dedicated event processing thread, in the context of another preexisting thread (in the style of GUI event loops), or as they are currently handled.

### How do event hierarchies work?
All events must subclass `Event`, but you may also create event hierarchies. If you have a base `AnimalEvent` class with a subclass `DogEvent` you'll see the following behavior:
 * Listeners for `DogEvent` will only be notified of actual `DogEvent` instances.
 * Listeners for `AnimalEvent` will be notified of both `AnimalEvent` and `DogEvent` instances.

Note that you'll still need to create dedicated event queues via `EventBus.add()` for each subclass you actually wish to deliver events for.

### How are exceptions handled?
Currently all exceptions are caught and logged. `EventVetoException` is a special case and is described below.

### Can events be prioritized?
Yes. The `@EventHandler` annotation accepts an additional `priority` parameter as an integer. Some common priorities are defined in the `EventPriority` class.

By default, event handlers are implicitly `@EventHandler( priority = EventPriority.NORMAL )`, but this can be any other integer. Higher priorities are positive, while lower priorities are negative; "normal" has a priority of zero.

During execution, events handlers are executed in order of priority.

### Can events be vetoed?
Yes, but event handlers may additionally flag themselves immune from this. By default, all event handlers are vetoable, but may disable this with `@EventHandler( vetoable = false )`.

Event handlers may throw an `EventVetoException` during the course of their execution to prevent lower-priority events from executing. However, any event handlers defined with `vetoable = false` _will_ still execute.  

