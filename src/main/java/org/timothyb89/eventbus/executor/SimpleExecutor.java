package org.timothyb89.eventbus.executor;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.timothyb89.eventbus.Event;
import org.timothyb89.eventbus.EventQueueDefinition;
import org.timothyb89.eventbus.EventQueueEntry;
import org.timothyb89.eventbus.EventVetoException;

/**
 * A simple {@link Executor} implementation that processes all events within
 * the invocation of {@code push()}. One implication of this implementation is
 * that notification of a large number of events may cause {@code push()} calls
 * to return slowly.
 * @author timothyb89
 */
@Slf4j
public class SimpleExecutor implements Executor {

	@Override
	public void push(EventQueueDefinition def, Event event) {
		List<EventQueueEntry> queue = new LinkedList<>(def.getQueue());
		
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

	@Override
	public void push(EventQueueDefinition def, Event event, long deadline) {
		List<EventQueueEntry> queue = new LinkedList<>(def.getQueue());
		
		long start = System.currentTimeMillis();
		
		boolean vetoed = false;
		for (EventQueueEntry e : queue) {
			// if the event has been vetoed, and this event is vetoable,
			// skip it
			if (vetoed && e.isVetoable()) {
				continue;
			}
			
			// if we've exceeded the deadline, stop
			// we can't stop handlers already in progress so we may exceed the
			// deadline, but we should at least try to stop ASAP
			if (!e.isDeadlineExempt()) {
				if (System.currentTimeMillis() - start > deadline) {
					continue;
				}
			}
			
			try {
				log.debug("Notifying listener: {}", e.getMethod());
				e.notify(event);
			} catch (EventVetoException ex) {
				// skip others on event veto
				vetoed = true;
			}
		}
	}
	
}
