package org.timothyb89.eventbus;

/**
 * Defines an exception that will cause the event bus to stop processing further
 * events within the event queue for a particular event type.
 * @author timothyb89
 */
public class EventVetoException extends RuntimeException {

	public EventVetoException() {
	}

	public EventVetoException(String message) {
		super(message);
	}

	public EventVetoException(Throwable cause) {
		super(cause);
	}

	public EventVetoException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
