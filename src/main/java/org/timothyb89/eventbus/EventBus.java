package org.timothyb89.eventbus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.timothyb89.eventbus.executor.Executor;
import org.timothyb89.eventbus.executor.SimpleExecutor;

/**
 * Defines an event bus that handles the dispatching of events to client
 * classes. The owner class can define "buckets" or queues for subclasses of
 * {@link Event}, which may then be joined by client classes. The owner can then
 * push an instance of the {@code Event} to the bus using {@link #push(Event)}
 * which will notify all registered classes listening for that event type, or
 * any of its superclasses.
 * <p>This implementation allows for inherited and overridden event types,
 * assuming that each superclass has defined a queue specifically for that type.
 * When listeners are registered, they are added to all queues 'compatible' with
 * their event type, meaning that they will be placed into the event queue for
 * their direct event type, and any assignable superclasses. Then, when an event
 * is pushed to the queue, only the queue for that exact type is notified. This
 * ensures that listeners will only ever receive a single notification for
 * events compatible via multiple superclasses.</p>
 * <p>Definition of listeners requires the {@link EventHandler} annotation. When
 * some client class calls {@link #register(Object)}, all methods with an
 * {@code @EventHandler} annotation are scanned and added to the appropriate
 * event queues.</p>
 * <p>{@code @EventHandler} takes an optional {@code priority} parameter. Events
 * with a higher priority (defined either as an integer or preferably using a
 * constant in {@link EventPriority}) will be notified before events with a
 * lower (more negative) priority.</p>
 * <p>Events may also be 'vetoed', in the sense that an event at a higher
 * priority may prevent handlers further down in the queue from being executed.
 * While useful, this should be used with caution to ensure potentially
 * important events may still get an opportunity to be notified.</p>
 * @author timothyb89
 */
@Slf4j
public class EventBus {
	
	private final List<EventQueueDefinition> definitions;
	
	/**
	 * Event dispatching handler
	 */
	private final Executor executor;
	
	/**
	 * Client-safe interface for this bus
	 */
	@Getter
	private final EventBusClient client;
	
	/**
	 * Creates a new event bus. By default, a {@link SimpleExecutor} is used
	 * to dispatch events; use {@link #EventBus(Executor)} to specify a
	 * different implementation.
	 */
	public EventBus() {
		definitions = new ArrayList<>();
		
		executor = new SimpleExecutor();
		client = new EventBusClient(this);
	}
	
	/**
	 * Creates a new event bus with the given {@link Executor} implementation.
	 * @param executor the executor implementation to use
	 */
	public EventBus(Executor executor) {
		this.executor = executor;
		
		definitions = new ArrayList<>();
		client = new EventBusClient(this);
	}
	
	/**
	 * Defines a new event type. A new event queue will be added for the
	 * provided class, and future invocations of {@link push(Event)} will notify
	 * registered listeners.
	 * <p>Note that this class <i>is not</i> thread-safe; {@code add()} and
	 * {@code remove()} may cause a {@code ConcurrentModificationException} when
	 * used concurrently.</p>
	 * @param clazz the event class to register
	 */
	public void add(Class<? extends Event> clazz) {
		definitions.add(new EventQueueDefinition(clazz));
	}
	
	/**
	 * Gets the EventQueueDefinition for the given class. If no event queue has
	 * been created for the given class, {@code null} is returned. Note that
	 * this will not return superclasses of the given class, only exact matches.
	 * @param clazz The class to search for
	 * @return the event queue for the given class
	 */
	public EventQueueDefinition getQueueForClass(Class<? extends Event> clazz) {
		for (EventQueueDefinition d : definitions) {
			if (d.getEventType() == clazz) {
				return d;
			}
		}
		
		return null;
	}
	
	/**
	 * Removes the event queue for the given class. If no queue for the given
	 * class is found, the method fails silently.
	 * @param clazz the class for which to remove the queue
	 */
	public void remove(Class<? extends Event> clazz) {
		definitions.remove(getQueueForClass(clazz));
	}
	
	/**
	 * Pushes the given event to the message bus. This will notify listeners in
	 * order of their priority; any handler that throws an
	 * {@link EventVetoException} will cause handlers further down in the queue
	 * to be skipped. Specifically, this notifies the event queue that exactly
	 * matches the class (as handlers are added to superclass queues at
	 * registration time).
	 * <p>If no queue exists for the given event type, no listeners will be
	 * notified and the method will fail silently.</p>
	 * <p>The actual behavior of this method depends on the {@link Executor}
	 * this {@code EventBus} was constructed with. By default, events are
	 * executed on the current thread, and this method will not return until all
	 * listeners have been notified; however, different executors may offload
	 * processing to one or more dedicated threads, causing this method to
	 * return immediately.</p>
	 * <p>Unlike {@link #add(Class)} and {@link #remove(Class)}, this method
	 * <i>is</i> thread-safe and can be safely used concurrently.</p>
	 * @param event the event to push
	 */
	public void push(Event event) {
		EventQueueDefinition def = getQueueForClass(event.getClass());
		if (def != null) {
			executor.push(def, event);
		} // TODO: else: warn?
	}
	
	/**
	 * Pushes the given event to the bus, with a specified deadline. A deadline
	 * is the time (relative to the start of event processing) after which no
	 * additional event handlers will be notified.
	 * <p>The deadline is not strictly enforced: event handlers will not be
	 * terminated if the deadline is exceeded, but lower-priority handlers
	 * remaining in the queue will not be notified when the specified amount of
	 * time has passed.</p>
	 * <p>Event veto functionality and event prioritization is identical to that
	 * of {@link #push(Event)}.</p>
	 * @see #push(Event) 
	 * @param event the event to push 
	 * @param deadline the maximum time to spend notify handlers (milliseconds)
	 */
	public void push(Event event, long deadline) {
		EventQueueDefinition def = getQueueForClass(event.getClass());
		if (def != null) {
			executor.push(def, event, deadline);
		}
	}
	
	/**
	 * Registers the given method to the event bus. The object is assumed to be
	 * of the class that contains the given method.
	 * <p>The method parameters are
	 * checked and added to the event queue corresponding to the {@link Event}
	 * used as the first method parameter. In other words, if passed a reference
	 * to the following method:</p>
	 * <p><code>
	 * public void xyzHandler(XYZEvent event) { ... }
	 * </code></p>
	 * <p>... <code>xyzHandler</code> will be added to the event queue for
	 * {@code XYZEvent} assuming that {@code XYZEvent} extends {@link Event} and
	 * an event queue has been created for that event type.</p>
	 * @param o an instance of the class containing the method <code>m</code>
	 * @param m the method to register
	 * @param priority the event priority
	 * @param vetoable vetoable flag
	 * @param deadlineExempt deadline exemption flag
	 */
	protected void registerMethod(
			Object o, Method m,
			int priority, boolean vetoable, boolean deadlineExempt) {
		// check the parameter types, and attempt to resolve the event
		// type
		if (m.getParameterTypes().length != 1) {
			log.warn("Skipping invalid event handler definition: " + m);
			return;
		}

		// make sure the parameter is an Event
		// this additional/technically unneeded check should cut down on the
		// time required to process the loop over the definitions for methods
		// that don't match
		Class<?> param = m.getParameterTypes()[0];
		if (!Event.class.isAssignableFrom(param)) {
			log.warn("Skipping event handler without an Event parameter: " + m);
			return;
		}
	
		// add the method to all assignable definitions.
		// this may result in the method being added to multiple queues,
		// that is, the queues for each superclass.
		// (this is intended and is fundamentally what makes subclassed events
		// work as expected)
		for (EventQueueDefinition d : definitions) {
			if (param.isAssignableFrom(d.getEventType())) {
				d.add(new EventQueueEntry(
						o, m, priority, vetoable, deadlineExempt));
				log.debug("Added {} to queue {}", m, d.getEventType());
			}
		}
	}
	
	/**
	 * Scans non-public members of the given object at the level of the given
	 * class. Due to how {@link Class#getDeclaredMethods()} works, this only
	 * scans members directly defined in {@code clazz}.
	 * @param o the object to scan
	 * @param clazz the specific class to scan
	 */
	private void scanInternal(Object o, Class clazz) {
		for (Method m : clazz.getDeclaredMethods()) {
			if (Modifier.isPublic(m.getModifiers())) {
				// public fields have already been processed
				continue;
			}
			
			// skip methods without annotation
			if (!m.isAnnotationPresent(EventHandler.class)) {
				continue;
			}
			
			// set the method accessible and register it
			EventHandler h = m.getAnnotation(EventHandler.class);
			int priority = h.priority();
			boolean vetoable = h.vetoable();
			boolean deadlineExempt = h.deadlineExempt();

			m.setAccessible(true);

			registerMethod(o, m, priority, vetoable, deadlineExempt);
		}
	}
	
	/**
	 * Attempts to register all methods of the given object annotated with
	 * {@link EventHandler} to receive events from this bus. Only methods
	 * that are annotated with {@code @EventHandler} and accept a single
	 * parameter of an event type emitted from this bus (that is, previously
	 * added with {@link #add(java.lang.Class)}) will be registered.
	 * <p>The {@link EventScanMode} annotation may be used to control the
	 * behavior and degree of scanning. By default the scan mode is
	 * {@link EventScanType#FAST} and will scan all public methods for the
	 * {@code EventHandler} annotation, including methods inherited from some
	 * superclass. However, private fields will not be scanned.</p>
	 * <p>{@link EventScanType#EXTENDED} additionally scans and registers
	 * private methods defined directly in the given object, but does not scan
	 * private fields in superclasses. This incurs a higher runtime cost at
	 * registration time as more methods are scanned.</p>
	 * <p>The final scanning type, {@link EventScanType#FULL}, scans all public
	 * methods (as with the {@code FAST} type), direct private members (as with
	 * {@code EXTENDED}), and private members defined in any superclass. This
	 * will incur the highest runtime cost, but will preserve any event-related
	 * functionality.
	 * @see #registerMethod(Object, Method, int, boolean, boolean)
	 * @param o the object to process
	 */
	public void register(Object o) {
		Class<?> c = o.getClass();
		
		EventScanType scanType = EventScanType.FAST;
		if (c.isAnnotationPresent(EventScanMode.class)) {
			EventScanMode modeDef = c.getAnnotation(EventScanMode.class);
			
			scanType = modeDef.type();
		}
		
		// always scan all public members
		for (Method m : c.getMethods()) {
			if (m.isAnnotationPresent(EventHandler.class)) {
				// get the priority from the annotation
				EventHandler h = m.getAnnotation(EventHandler.class);
				int priority = h.priority();
				boolean vetoable = h.vetoable();
				boolean deadlineExempt = h.deadlineExempt();
				
				registerMethod(o, m, priority, vetoable, deadlineExempt);
			}
		}
		
		// scan private fields if requested
		if (scanType == EventScanType.EXTENDED
				|| scanType == EventScanType.FULL) {
			scanInternal(o, c);
		}
		
		// also scan superclasses if requested
		if (scanType == EventScanType.FULL) {
			Class sup = c.getSuperclass();
			
			// work up the obejct hierarchy
			while (sup != Object.class) {
				scanInternal(o, sup);
				
				sup = sup.getSuperclass();
			}
		}
	}
	
	/**
	 * Removes the given object from all event queues that it may be a member
	 * of. The object will immediately stop receiving events from this EventBus.
	 * @param o the object to remove
	 */
	public void deregister(Object o) {
		for (EventQueueDefinition def : definitions) {
			def.removeAll(o);
		}
	}
	
}
