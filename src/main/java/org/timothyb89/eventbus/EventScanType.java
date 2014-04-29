package org.timothyb89.eventbus;

/**
 *
 * @author timothyb89
 */
public enum EventScanType {
	
	/**
	 * The default scanning type; only public members are scanned, but all
	 * public methods in superclasses are included.
	 */
	FAST,
	
	/**
	 * Extended scanning; private members are additionally included.
	 */
	EXTENDED,
	
	/**
	 * Extended scanning, but also scans superclasses and registers their
	 * private annotated methods.
	 */
	FULL;
	
}
