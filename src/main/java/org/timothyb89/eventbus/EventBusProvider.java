package org.timothyb89.eventbus;

/**
 * A simple denotation that a particular class has an event bus that can be used
 * by client classes. This doesn't guarantee that a particular event type can be
 * emitted from an unknown EventBusProvider, but it does guarantee that a client
 * class can at least attempt to register itself.
 * <p>This can be advantageous if given a list of varying objects that may emit
 * certain events: a listener can be registered for all of them with essentially
 * no repercussions if the target event isn't supported.</p> 
 * @author timothyb89
 */
public interface EventBusProvider {
	
	/**
	 * Gets the EventBusClient for this ClientSession, which allows other
	 * classes to register themselves to receive event notifications.
	 * @return the {@link EventBusClient} for this class
	 */
	public EventBusClient bus();
	
}
