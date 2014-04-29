package org.timothyb89.eventbus.executor;

import org.timothyb89.eventbus.Event;
import org.timothyb89.eventbus.EventQueueDefinition;

/**
 * Defines a executor that can dispatch an event to a queue of event handlers.
 * @author timothyb89
 */
public interface Executor {
	
	/**
	 * Dispatches the given event to event handlers in {@code def}.
	 * @param def the queue to notify
	 * @param event the event to dispatch
	 */
	public void push(EventQueueDefinition def, Event event);
	
	/**
	 * Dispatches the given event to event handlers in {@code def}, only
	 * notifying handlers before {@code deadline} has been exceeded.
	 * @param def the queue to notify
	 * @param event the event to dispatch
	 * @param deadline the relative deadline, in milliseconds
	 */
	public void push(EventQueueDefinition def, Event event, long deadline);
	
}
