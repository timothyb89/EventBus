package org.timothyb89.eventbus.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.timothyb89.eventbus.Event;
import org.timothyb89.eventbus.EventQueueDefinition;
import org.timothyb89.eventbus.EventQueueEntry;
import org.timothyb89.eventbus.EventVetoException;

/**
 * An {@link Executor} implementation that processes events in a single,
 * dedicated thread. Invocations of {@link #push(EventQueueDefinition, Event)}
 * will not cause (significant) blocking. Note that if new events are being
 * pushed faster than they are being processed, some degree of starvation may
 * occur.
 * @author timothyb89
 */
@Slf4j
public class ThreadedExecutor implements Executor {

	private final LinkedBlockingQueue<QueueEntry> queue;
	
	private final Thread processorThread;
	private volatile boolean killed;

	public ThreadedExecutor(String threadName) {
		queue = new LinkedBlockingQueue<>();
		killed = false;
		
		processorThread = new Thread(eventProcessor, threadName);
		processorThread.start();
	}

	public ThreadedExecutor() {
		queue = new LinkedBlockingQueue<>();
		killed = false;
		
		processorThread = new Thread(eventProcessor);
		processorThread.start();
	}
	
	@Override
	public void push(EventQueueDefinition def, Event event) {
		queue.offer(new QueueEntry(def, event, -1));
	}

	@Override
	public void push(EventQueueDefinition def, Event event, long deadline) {
		queue.offer(new QueueEntry(def, event, deadline));
	}
	
	public void stop() {
		killed = true;
		
		if (processorThread != null) {
			processorThread.interrupt();
		}
	}
	
	public void restart() {
		processorThread.start();
	}
	
	private final Runnable eventProcessor = new Runnable() {

		@Override
		public void run() {
			while (!killed) {
				try {
					QueueEntry entry = queue.take();
					
					List<EventQueueEntry> queue = new LinkedList<>(entry.def.getQueue());
		
					long start = System.currentTimeMillis();

					boolean vetoed = false;
					for (EventQueueEntry e : queue) {
						if (vetoed && e.isVetoable()) {
							log.trace("Event vetoed: {}", e);
							continue;
						}

						if (!e.isDeadlineExempt()) {
							if (entry.deadline > 0 &&
									System.currentTimeMillis() - start > entry.deadline) {
								log.trace(
										"Event skipped; deadline exceeded: {}",
										e);
								continue;
							}
						}

						try {
							log.debug("Notifying event: {}", e);
							e.notify(entry.event);
						} catch (EventVetoException ex) {
							// skip others on event veto
							vetoed = true;
						}
					}
				} catch (InterruptedException ex) {
					break;
				}
			}
		}
		
	};
	
	@Data
	private class QueueEntry {
		private final EventQueueDefinition def;
		private final Event event;
		private final long deadline;
	}
	
}
