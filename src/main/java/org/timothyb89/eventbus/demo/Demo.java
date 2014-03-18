package org.timothyb89.eventbus.demo;

/**
 * A simple harness class for the {@link DemoEmitter} and {@link DemoReceiver}.
 * @author timothyb89
 */
public class Demo {
	
	public static void main(String[] args) {
		DemoEmitter emitter = new DemoEmitter();
		
		DemoReceiver receiver = new DemoReceiver();
		emitter.getBus().register(receiver);
		
		emitter.fireA("Hello");
		emitter.fireAB("Hello", "John");
		emitter.fireB(42);
		
		
	}
	
}
