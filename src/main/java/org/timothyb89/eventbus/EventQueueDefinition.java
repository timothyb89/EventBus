package org.timothyb89.eventbus;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Defines an event queue which contains multiple priority-specific event
 * handler sinks.
 * <p>This class is thread-safe and should block minimally when accessed
 * concurrently.</p>
 * @author timothyb89
 */
@Slf4j
public class EventQueueDefinition {
	
	@Getter
	private final Class<? extends Event> eventType;
	
	@Getter
	private final ConcurrentSkipListMap<Integer, List<EventQueueEntry>> queue;
	
	public EventQueueDefinition(Class<? extends Event> eventType) {
		this.eventType = eventType;
		
		queue = new ConcurrentSkipListMap<>(
				new ReverseNaturalComparator<Integer>());
	}
	
	/**
	 * Registers the given entry for events. This may cause minor blocking on
	 * priority-specific lists when used concurrently with
	 * {@link #remove(EventQueueEntry)}, {@link #removeAll(Object)}, and
	 * {@link #push(Event)}.
	 * @param entry the entry to add to the notification queue
	 */
	public void add(EventQueueEntry entry) {
		List<EventQueueEntry> container = queue.get(entry.getPriority());
		if (container == null) {
			// we'll need to synchronize on the inner list, but the outer list
			// should be safe
			// multiple threads calling add(), remove(), and push() should only
			// cause waits as they operate on the inner lists
			container = Collections.synchronizedList(
					new LinkedList<EventQueueEntry>());
			queue.put(entry.getPriority(), container);
		}
		
		container.add(entry);
	}
	
	/**
	 * Removes the given queue entry from the notification list.
	 * @param entry the entry to remove
	 */
	public void remove(EventQueueEntry entry) {
		List<EventQueueEntry> container = queue.get(entry.getPriority());
		if (container == null) {
			return;
		}
		
		container.remove(entry);
		
		// TODO: can we remove an empty container safely?
	}
	
	/**
	 * Removes all handler methods in the given object from notifications by
	 * this queue.
	 * @param object the object to deregister
	 */
	public void removeAll(Object object) {
		for (Entry<Integer, List<EventQueueEntry>> e : queue.entrySet()) {
			List<EventQueueEntry> entries = e.getValue();
			
			List<EventQueueEntry> toRemove = new LinkedList<>();
			synchronized (entries) {
				for (EventQueueEntry entry : entries) {
					if (entry.getObject() == object) {
						toRemove.add(entry);
					}
				}
				
				entries.removeAll(toRemove);
				
				// TODO: can we safely remove `entries` if it's empty here?
			}
		}
		
		log.debug(
				"Removed {} from notification queue for {}",
				object, eventType);
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
		
		for (Entry<Integer, List<EventQueueEntry>> e : queue.entrySet()) {
			List<EventQueueEntry> entries = e.getValue();
			synchronized (entries) {
				for (EventQueueEntry entry : entries) {
					if (vetoed && entry.isVetoable()) {
						log.debug(
								"Skipping handler (vetoed): {}",
								entry.getMethod());
						continue;
					}
					
					try {
						log.debug("Notifying handler: {}", entry.getMethod());
						entry.notify(event);
					} catch (EventVetoException ex) {
						// skip others on event veto
						vetoed = true;
					}
				}
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
		
		for (Entry<Integer, List<EventQueueEntry>> e : queue.entrySet()) {
			List<EventQueueEntry> entries = e.getValue();
			synchronized (entries) {
				for (EventQueueEntry entry : entries) {
					if (vetoed && entry.isVetoable()) {
						log.debug(
								"Skipping handler (vetoed): {}",
								entry.getMethod());
						continue;
					}
					
					if (entry.getPriority() < priority) {
						log.debug(
								"Skipping handler, priority too low: {}",
								entry.getMethod());
						continue;
					}
					
					try {
						log.debug("Notifying handler: {}", entry.getMethod());
						entry.notify(event);
					} catch (EventVetoException ex) {
						// skip others on event veto
						vetoed = true;
					}
				}
			}
		}
	}
	
	public class ReverseNaturalComparator<T extends Comparable>
			implements Comparator<T> {

		@Override
		public int compare(T o1, T o2) {
			return -1 * (o1.compareTo(o2));
		}
		
	}
	
}
