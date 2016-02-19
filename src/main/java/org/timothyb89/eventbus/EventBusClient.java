package org.timothyb89.eventbus;

/**
 * Provides client-level access to an {@link EventBus}. That is, this class
 * exposes only registration and deregistration functionality, allowing it to
 * be safely passed to client classes such that they will be unable to spawn new
 * events and add event queues directly.
 * @author timothyb89
 */
public class EventBusClient {
	
	private final EventBus bus;

	public EventBusClient(EventBus bus) {
		this.bus = bus;
	}
	
	/**
	 * Registers all methods of the given object annotated with
	 * {@link EventHandler}.
	 * @see EventBus#registerMethod(Object, Method, int, boolean, boolean)
	 * @param object the object to process
	 */
	public void register(Object object) {
		bus.register(object);
	}
	
	/**
	 * Removes the given object from any event queues that it may be a
	 * member of. The object will immediately cease to receive notifications
	 * from this MessageBus.
	 * @param object the object to remove
	 */
	public void deregister(Object object) {
		bus.deregister(object);
	}
	
}
