package org.timothyb89.eventbus;

import lombok.Getter;
import lombok.ToString;

/**
 * Defines an {@link Event} that occurs within some (generic) context. A context
 * could be, for example, the zone in which some entity event took place, or
 * more generally the primary containing entity for the subject of the
 * notification.
 * @author timothyb89
 * @param <T> the type of context for the event
 */
@ToString
public class ContextualEvent<T> extends Event {
	
	@Getter
	private final T context;
	
	public ContextualEvent(T context) {
		this.context = context;
	}
	
}
