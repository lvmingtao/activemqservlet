package com.example.esb.activemq.web;

import java.util.LinkedList;

import javax.jms.Message;
import javax.jms.MessageConsumer;

import org.apache.activemq.MessageAvailableListener;
import org.apache.activemq.web.AjaxWebClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.continuation.Continuation;

public class JmsWebListener implements MessageAvailableListener{
	private static Log log = LogFactory.getLog(JmsWebListener.class);
    private final long maximumReadTimeout;
    private final AjaxWebClient client;
    private long lastAccess;
    private Continuation continuation;
    private final LinkedList<UndeliveredJmsWebMessage> undeliveredMessages = new LinkedList<UndeliveredJmsWebMessage>();
    JmsWebListener(AjaxWebClient client, long maximumReadTimeout) {
        this.client = client;
        this.maximumReadTimeout = maximumReadTimeout;
        access();
    }

    public void access() {
        lastAccess = System.currentTimeMillis();
    }

    public synchronized void setContinuation(Continuation continuation) {
        this.continuation = continuation;
    }

    public LinkedList<UndeliveredJmsWebMessage> getUndeliveredMessages() {
        return undeliveredMessages;
    }
	@Override
	public synchronized void onMessageAvailable(MessageConsumer consumer) {

		log.debug("Message for consumer: "+consumer+" continuation: "+continuation);

        if (continuation != null) {
            try {
                Message message = consumer.receive(10);
                log.debug("message is " + message);
                if (message != null) {
                    if (!continuation.isResumed()) {
                    	log.debug("Resuming suspended continuation "+continuation);
                        continuation.setAttribute("undelivered_message", new UndeliveredJmsWebMessage(message, consumer));
                        continuation.resume();
                    } else {
                        log.debug("Message available, but continuation is already resumed. Buffer for next time.");
                        bufferMessageForDelivery(message, consumer);
                    }
                }
            } catch (Exception e) {
            	log.warn("Error receiving message " + e.getMessage() + ". This exception is ignored.", e);
            }

        } else if (System.currentTimeMillis() - lastAccess > 2 * this.maximumReadTimeout) {
            new Thread() {
                @Override
                public void run() {
                	log.debug("Closing consumers on client: "+ client);
                    client.closeConsumers();
                }
            }.start();
        } else {
            try {
                Message message = consumer.receive(10);
                bufferMessageForDelivery(message, consumer);
            } catch (Exception e) {
            	log.warn("Error receiving message " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
		
	}
	
	public void bufferMessageForDelivery(Message message, MessageConsumer consumer) {
        if (message != null) {
            synchronized (undeliveredMessages) {
                undeliveredMessages.addLast(new UndeliveredJmsWebMessage(message, consumer));
            }
        }
    }

}
