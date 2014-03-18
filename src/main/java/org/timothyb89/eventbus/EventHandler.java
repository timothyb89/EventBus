package org.timothyb89.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a flag for a method that allows it to receive events
 * @see EventBus#register(Object)
 * @author timothyb89
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
	
	/**
	 * Defines the priority for this event handler. Events with a higher
	 * priority are executed before events with a lower priority. This value
	 * is {@link EventPriority#NORMAL} unless otherwise specified.
	 * @see EventPriority
	 * @return the integer priority; more positive is higher; negative is lower
	 */
	int priority() default EventPriority.NORMAL;
	
	/**
	 * Specifies whether or not this event handler may be vetoed. By default,
	 * this is {@code true}, and setting this to {@code false} should be used
	 * with extreme caution. Handlers that are not vetoable will still be run
	 * after a more important event has specifically requested that they not,
	 * and as such may cause some logical errors.
	 * <p>Specifically, {@code true} status should be reserved either for event
	 * handlers vital to the operation of the event bus host, or completely
	 * passive events (such as loggers) that can't have an effect on the greater
	 * system.</p>
	 * @return true if this handler may be vetoed; false otherwise
	 */
	boolean vetoable() default true;
	
}
