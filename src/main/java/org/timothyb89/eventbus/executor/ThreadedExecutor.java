package org.timothyb89.eventbus.executor;

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

		private void process(QueueEntry qe) {
			boolean vetoed = false;
			long start = System.currentTimeMillis();
			
			for (List<EventQueueEntry> entries : qe.def.getQueue().values()) {
				synchronized (entries) {
					for (EventQueueEntry handler : entries) {
						if (!handler.canInvoke(vetoed, start, qe.deadline)) {
							continue;
						}
						
						try {
							handler.notify(qe.event);
						} catch (EventVetoException ex) {
							// skip others on event veto
							vetoed = true;
						}
					}
				}
			}
		}
		
		@Override
		public void run() {
			while (!killed) {
				try {
					process(queue.take());
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
