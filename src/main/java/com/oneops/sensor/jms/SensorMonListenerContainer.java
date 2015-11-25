package com.oneops.sensor.jms;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Session;

import org.apache.log4j.Logger;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class SensorMonListenerContainer extends DefaultMessageListenerContainer {

	private Logger logger = Logger.getLogger(this.getClass());
	
	public static final AtomicInteger COUNT = new AtomicInteger(0);  	
	@Override
	public void initialize() {
		// we need to intercept this one so we can initi it with selector
		logger.debug("Fake init");
	}

	public void initWithSelector(int instanceId, int poolSize) {
		int modulus = instanceId - 1; 
		String selector = "(manifestId % " + poolSize + ") = " + modulus;
		logger.info(">>>>>>>>>>>>>>>>>This sensor instance will use message selector for monitor mgmt - " + selector);
		super.setMessageSelector(selector);
		super.initialize();
	}

	@Override
	protected void messageReceived(Object invoker, Session session) {
		COUNT.getAndIncrement();
		super.messageReceived(invoker, session);
	}

}
