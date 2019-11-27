package com.example.esb.activemq.web;

import javax.jms.Message;
import javax.jms.MessageConsumer;

class UndeliveredJmsWebMessage {
    private Message message;
    private MessageConsumer consumer;
    
    UndeliveredJmsWebMessage( Message message, MessageConsumer consumer ) {
        this.message = message;
        this.consumer = consumer;
    }
    
    public MessageConsumer getConsumer() {
        return this.consumer;
    }
    
    public Message getMessage() {
        return this.message;
    }
}