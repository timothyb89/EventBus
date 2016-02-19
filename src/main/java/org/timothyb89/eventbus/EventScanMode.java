package org.timothyb89.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A simple mark that a class should have detailed scanning enabled. This
 * functionality is kept separate as scanning for private methods can be fairly
 * expensive
 * @author timothyb89
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventScanMode {
	
	EventScanType type() default EventScanType.FAST;
	
}
