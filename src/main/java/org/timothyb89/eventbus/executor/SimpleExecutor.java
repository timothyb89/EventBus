package org.timothyb89.eventbus.executor;

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
	public void push(EventQueueDefinition def, Event event, long deadline) {
		boolean vetoed = false;
		long start = System.currentTimeMillis();
		
		for (List<EventQueueEntry> entries : def.getQueue().values()) {
			synchronized (entries) {
				for (EventQueueEntry entry : entries) {
					if (!entry.canInvoke(vetoed, start, deadline)) {
						continue;
					}
					
					try {
						entry.notify(event);
					} catch (EventVetoException ex) {
						// skip others on event veto
						vetoed = true;
					}
				}
			}
		}
	}
	
}
