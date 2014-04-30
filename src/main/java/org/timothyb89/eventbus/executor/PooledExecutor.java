package org.timothyb89.eventbus.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.timothyb89.eventbus.Event;
import org.timothyb89.eventbus.EventQueueDefinition;
import org.timothyb89.eventbus.EventQueueEntry;
import org.timothyb89.eventbus.EventVetoException;

/**
 * An executor implementation that dispatches events to a pool of threads. It
 * will still attempt to notify listeners in priority order, but actual ordering
 * is not guaranteed.
 * <p>Deadline behavior is implemented globally such that events will not be
 * executed if their deadline has been exceeded without regard to the thread
 * that handles it.</p>
 * <p>Thread pool behavior may be modified by specifying a different
 * {@link ExecutorService} implementation. By default,
 * {@link Executors#newCachedThreadPool()} is used, but other implementations
 * may be preferred depending on usage.</p>
 * <p>Different {@code ExecutorService} implementations will have varying
 * implications on event dispatching behavior. Most implementations will prevent
 * immediate deadlock if an event handler performs a blocking operation, however
 * only {@link Executors#newCachedThreadPool()} allows effectively unlimited
 * threads to spawn. {@link Executors#newFixedThreadPool(int)} may be used to
 * spawn a particular number of worker threads.</p>
 * @author timothyb89
 */
@Slf4j
public class PooledExecutor implements Executor {

	private final ExecutorService service;

	public PooledExecutor(ExecutorService service) {
		this.service = service;
	}
	
	public PooledExecutor() {
		service = Executors.newCachedThreadPool();
	}
	
	@Override
	public void push(EventQueueDefinition def, Event event) {
		EventData data = new EventData(event, System.currentTimeMillis(), -1);
		for (EventQueueEntry entry : def.getQueue()) {
			service.submit(new NotifyTask(data, entry));
		}
	}

	@Override
	public void push(EventQueueDefinition def, Event event, long deadline) {
		EventData data = new EventData(
				event,
				System.currentTimeMillis(),
				deadline);
		
		for (EventQueueEntry entry : def.getQueue()) {
			service.submit(new NotifyTask(data, entry));
		}
	}
	
	@Data
	private class EventData {
		private volatile boolean vetoed = false;
		
		private final Event event;
		private final long start;
		private final long deadline;
	}
	
	@Data
	private class NotifyTask implements Callable {

		private final EventData data;
		private final EventQueueEntry entry;
		
		@Override
		public Object call() throws Exception {
			if (data.vetoed && entry.isVetoable()) {
				// event vetoed
				log.trace("Event vetoed: {}", entry);
				return null;
			}
			
			if (data.deadline > 0 && !entry.isDeadlineExempt()) {
				if (System.currentTimeMillis() - data.start > data.deadline) {
					// deadline exceeded
					log.trace(
							"Event handler skipped, deadline exceeded: {}",
							entry);
					return null;
				}
			}
			
			try {
				log.debug("Notifying event: {}", entry);
				entry.notify(data.event);
			} catch (EventVetoException ex) {
				data.vetoed = true;
			}
			
			return null;
		}
		
	}
	
}
