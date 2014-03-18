package org.timothyb89.eventbus.demo;

import lombok.extern.slf4j.Slf4j;
import org.timothyb89.eventbus.EventHandler;
import org.timothyb89.eventbus.EventPriority;
import org.timothyb89.eventbus.demo.DemoEmitter.DemoEventA;
import org.timothyb89.eventbus.demo.DemoEmitter.DemoEventAB;
import org.timothyb89.eventbus.demo.DemoEmitter.DemoEventB;

/**
 * A simple receiver class for events emitted by {@link DemoEmitter}
 * @author timothyb89
 */
@Slf4j
public class DemoReceiver {
	
	@EventHandler(priority = EventPriority.LOW)
	public void onEventALow(DemoEventA event) {
		// this method should receive all A and AB-typed events after onEventAHigh()
		log.info("onEventALow(): " + event);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEventAHigh(DemoEventA event) {
		// this method should receive all A and AB-typed events before onEventALow()
		log.info("onEventAHigh(): " + event);
	}
	
	@EventHandler // priority defaults to 'normal' ( 0 )
	public void onEventAB(DemoEventAB event) {
		// this method should receive all events of type AB, but not A
		log.info("onEventAB(): " + event);
	}
	
	@EventHandler
	public void onEventB(DemoEventB event) {
		log.info("onEventB(): " + event);
	}
	
}
