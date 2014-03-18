package org.timothyb89.eventbus;

import java.lang.reflect.Method;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Defines an entry in the event queue for a specific event type.
 * TODO: should we use WeakReferences here? we could potentially avoid having to
 * ever manually deregister a class from event notifications, and just queue
 * "expired" event listeners for removal if we see they've been garbage
 * collected
 * @author timothyb89
 */
@Slf4j
public class EventQueueEntry implements Comparable<EventQueueEntry> {
	
	@Getter
	private Object object;
	
	@Getter
	private Method method;
	
	@Getter
	private int priority;
	
	@Getter
	private boolean vetoable;

	public EventQueueEntry(
			Object object, Method method, int priority, boolean vetoable) {
		this.object = object;
		this.method = method;
		this.priority = priority;
		this.vetoable = vetoable;
	}
	
	@Override
	public int compareTo(EventQueueEntry o) {
		return o.priority - priority;
	}
	
	/**
	 * Notifies this queue entry of an event. Note that this blatantly assumes
	 * that the passed event is compatible with the method associated with this
	 * entry (as it was checked at registration time). As such, any outside 
	 * invocations of this method will need to manually check this.
	 * @param event the event to pass to this queue entry
	 */
	public void notify(Event event) {
		try {
			method.invoke(object, event);
		} catch (EventVetoException ex) {
			// skip this - it needs to be passed to the queue to skip properly
			throw ex;
		} catch (Exception ex) {
			// we don't want non-veto exceptions to break the entire event queue
			// so we catch and log the error here
			log.error("Error in event handler " + method, ex);
		}
	}
	
}
