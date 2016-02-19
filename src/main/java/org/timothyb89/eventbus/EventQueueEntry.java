package org.timothyb89.eventbus;

import java.lang.reflect.Method;
import lombok.Getter;
import lombok.ToString;
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
@ToString
public class EventQueueEntry implements Comparable<EventQueueEntry> {
	
	@Getter
	private final Object object;
	
	@Getter
	private final Method method;
	
	@Getter
	private final int priority;
	
	@Getter
	private final boolean vetoable;
	
	@Getter
	private final boolean deadlineExempt;

	public EventQueueEntry(
			Object object, Method method, int priority,
			boolean vetoable, boolean deadlineExempt) {
		this.object = object;
		this.method = method;
		this.priority = priority;
		this.vetoable = vetoable;
		this.deadlineExempt = deadlineExempt;
	}
	
	@Override
	public int compareTo(EventQueueEntry o) {
		return o.priority - priority;
	}
	
	/**
	 * Checks if the event handler referenced by this object can be invoked.
	 * Specifically, this determines if the event can be triggered after a veto
	 * has occurred, or if a deadline has already passed.
	 * @param vetoed true if the event has already been vetoed, false otherwise
	 * @param start the starting time of event processing
	 * @param deadline the deadline, or -1 if none is set
	 * @return true if the handler can be notified, false otherwise
	 */
	public boolean canInvoke(boolean vetoed, long start, long deadline) {
		if (vetoed && vetoable) {
			log.trace("Skipping handler (vetoed): {}", this);
			return false;
		}
		
		if (deadline > 0 && !deadlineExempt) {
			if (System.currentTimeMillis() - start > deadline) {
				log.trace(
						"Skipping handler (deadline exceeded): {}",
						this);
				return false;
			}
		}
		
		return true;
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
			log.debug("Notifying handler: {}", method);
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
