package org.timothyb89.eventbus;

/**
 * Defines basic event levels. Registered events with higher priority levels
 * will be ordered first in the event queue for each event type.
 * @author timothyb89
 */
public class EventPriority {
	
	public static final int HIGHEST = 1000;
	public static final int HIGHER = 100;
	public static final int HIGH = 10;
	public static final int NORMAL = 0;
	public static final int LOW = -10;
	public static final int LOWER = -100;
	public static final int LOWEST = -1000;
	
}
