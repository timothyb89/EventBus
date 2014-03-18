package org.timothyb89.eventbus;

import java.util.PriorityQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author timothyb89
 */
@Slf4j
public class EventQueueDefinition {
	
	@Getter
	private Class<? extends Event> eventType;
	
	@Getter
	private PriorityQueue<EventQueueEntry> queue;
	
	public EventQueueDefinition(Class<? extends Event> eventType) {
		this.eventType = eventType;
		
		queue = new PriorityQueue();
	}
	
	/**
	 * Notifies entries in the event queue of the given event. Note that events
	 * may be vetoed by queue entries if their {@code vetoable} property is set
	 * to {@code true} (the default), which will cause remaining, lower priority
	 * events to be skipped. Ideally listeners that may veto events should have
	 * an above-normal priority to ensure consistent behavior.
	 * @param event the event to dispatch
	 */
	public void push(Event event) {
		boolean vetoed = false;
		
		for (EventQueueEntry e : queue) {
			log.debug("Notifying listener: {}", e.getMethod());
			
			// if the event has been vetoed, and this event is vetoable,
			// skip it
			if (vetoed && e.isVetoable()) {
				continue;
			}
			
			try {
				e.notify(event);
			} catch (EventVetoException ex) {
				// skip others on event veto
				vetoed = true;
			}
		}
	}
	
	/**
	 * Notifies entries in the event queue with the given minimum priority.
	 * @see #push(Event)
	 * @param event The event to dispatch
	 * @param priority The minimum priority
	 */
	public void push(Event event, int priority) {
		boolean vetoed = false;
		
		for (EventQueueEntry e : queue) {
			log.debug("Notifying listener: {}", e.getMethod());
			
			if (vetoed && e.isVetoable()) {
				continue;
			}
			
			if (e.getPriority() < priority) {
				continue;
			}
			
			try {
				e.notify(event);
			} catch (EventVetoException ex) {
				vetoed = true;
			}
		}
	}
	
}
