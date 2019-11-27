package com.example.esb.activemq.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.activemq.MessageAvailableConsumer;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.web.AjaxListener;
import org.apache.activemq.web.AjaxWebClient;
import org.apache.activemq.web.MessageServlet;
import org.apache.activemq.web.NoDestinationSuppliedException;
import org.apache.activemq.web.WebClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;

public class JmsServlet extends MessageServlet{
	private static Log log = LogFactory.getLog(JmsServlet.class);
//	private final HashMap<String, WebClient> clients = new HashMap<String, WebClient>();
    private final String readTimeoutParameter = "timeout";
	private String destinationParameter = "destination";
    private Destination defaultRequestDestination;
    private Destination defaultResponseDestination;
    private Destination defaultBroadcostDestination;
    private boolean defaultTopicFlag = true;
    private long defaultReadTimeout = -1;
    private long maximumReadTimeout = 25000;
    private int maximumMessages = 100;
    private final HashMap<String,JmsWebClient> jmsWebClients = new HashMap<String,JmsWebClient>();
    private final Timer clientCleanupTimer = new Timer("ActiveMQ JmsServlet Client Cleanup Timer", true);
	/*
     * Return the AjaxWebClient for this session+clientId.
     * Create one if it does not already exist.
     */
    protected JmsWebClient getJmsWebClient( HttpServletRequest request ) {
        HttpSession session = request.getSession(true);

        String clientId = request.getParameter( "clientId" );
        // if user doesn't supply a 'clientId', we'll just use a default.
        if( clientId == null ) {
            clientId = "defaultJmsWebClient";
        }
        String sessionKey = session.getId() + '-' + clientId;

        JmsWebClient client = null;
        synchronized (jmsWebClients) {
            client = jmsWebClients.get( sessionKey );
            // create a new JmsWebClient if one does not already exist for this sessionKey.
            if( client == null ) {
                if (log.isDebugEnabled()) {
                    log.debug( "creating new JmsWebClient in "+sessionKey );
                }
                client = new JmsWebClient( request, maximumReadTimeout );
                jmsWebClients.put( sessionKey, client );
            }
            client.updateLastAccessed();
        }
        return client;
    }
    
	public String getDestinationParameter() {
		return destinationParameter;
	}
	public void setDestinationParameter(String destinationParameter) {
		this.destinationParameter = destinationParameter;
	}
	
	public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        String name = servletConfig.getInitParameter("destination");
        if (name != null) {
            if (defaultTopicFlag) {
            	defaultBroadcostDestination = new ActiveMQTopic(name);
            } else {
            	defaultRequestDestination = new ActiveMQQueue(name);
            	defaultResponseDestination = new ActiveMQQueue(name+".rep");
            }
        }
        name = servletConfig.getInitParameter("maximumReadTimeout");
        if (name != null) {
            maximumReadTimeout = asLong(name);
        }
        name = servletConfig.getInitParameter("maximumMessages");
        if (name != null) {
            maximumMessages = (int)asLong(name);
        }
        clientCleanupTimer.schedule( new ClientCleaner(), 5000, 60000 );
    }
	
	/**
     * Sends a message to a destination
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	// lets turn the HTTP post into a JMS Message
        AjaxWebClient client = getJmsWebClient( request );
        String messageIds = "";

        synchronized (client) {

            if (log.isDebugEnabled()) {
            	log.debug("POST client=" + client + " session=" + request.getSession().getId() + " clientId="+ request.getParameter("clientId") + " info=" + request.getPathInfo() + " contentType=" + request.getContentType());
                // dump(request.getParameterMap());
            }

            int messages = 0;

            // loop until no more messages
            while (true) {
                // Get the message parameters. Multiple messages are encoded
                // with more compact parameter names.
                String destinationName = request.getParameter(messages == 0 ? "destination" : ("d" + messages));

                if (destinationName == null) {
                    destinationName = request.getHeader("destination");
                }

                String message = request.getParameter(messages == 0 ? "message" : ("m" + messages));
                String type = request.getParameter(messages == 0 ? "type" : ("t" + messages));

                if (destinationName == null || message == null || type == null) {
                    break;
                }

                try {
                    Destination destination = getDestination(client, request, destinationName);

                    if (log.isDebugEnabled()) {
                    	log.debug(messages + " destination=" + destinationName + " message=" + message + " type=" + type);
                    	log.debug(destination + " is a " + destination.getClass().getName());
                    }

                    messages++;

                    if ("listen".equals(type)) {
                        AjaxListener listener = client.getListener();
                        Map<MessageAvailableConsumer, String> consumerIdMap = client.getIdMap();
                        Map<MessageAvailableConsumer, String> consumerDestinationNameMap = client.getDestinationNameMap();
                        client.closeConsumer(destination); // drop any existing
                        // consumer.
                        MessageAvailableConsumer consumer = (MessageAvailableConsumer)client.getConsumer(destination, request.getHeader(WebClient.selectorName));

                        consumer.setAvailableListener(listener);
                        consumerIdMap.put(consumer, message);
                        consumerDestinationNameMap.put(consumer, destinationName);
                        if (log.isDebugEnabled()) {
                        	log.debug("Subscribed: " + consumer + " to " + destination + " id=" + message);
                        }
                    } else if ("unlisten".equals(type)) {
                        Map<MessageAvailableConsumer, String> consumerIdMap = client.getIdMap();
                        Map<MessageAvailableConsumer, String> consumerDestinationNameMap = client.getDestinationNameMap();
                        MessageAvailableConsumer consumer = (MessageAvailableConsumer)client.getConsumer(destination, request.getHeader(WebClient.selectorName));

                        consumer.setAvailableListener(null);
                        consumerIdMap.remove(consumer);
                        consumerDestinationNameMap.remove(consumer);
                        client.closeConsumer(destination);
                        if (log.isDebugEnabled()) {
                        	log.debug("Unsubscribed: " + consumer);
                        }
                    } else if ("send".equals(type)) {
                        TextMessage text = client.getSession().createTextMessage(message);
                        appendParametersToMessage(request, text);

                        client.send(destination, text);
                        messageIds += text.getJMSMessageID() + "\n";
                        if (log.isDebugEnabled()) {
                        	log.debug("Sent " + message + " to " + destination);
                        }
                    } else {
                    	log.warn("unknown type " + type);
                    }

                } catch (JMSException e) {
                	log.warn("jms", e);
                }
            }
        }

        if ("true".equals(request.getParameter("poll"))) {
            try {
                // TODO return message IDs
                doMessages(client, request, response);
            } catch (JMSException e) {
                throw new ServletException("JMS problem: " + e, e);
            }
        } else {
            // handle simple POST of a message
            if (request.getContentLength() != 0 && (request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/x-www-form-urlencoded"))) {
            	try {
                    Destination destination = getDestination(client, request);
                    String body = getPostedMessageBody(request);
                    TextMessage message = client.getSession().createTextMessage(body);
                    appendParametersToMessage(request, message);

                    client.send(destination, message);
                    if (log.isDebugEnabled()) {
                        log.debug("Sent to destination: " + destination + " body: " + body);
                    }
                    messageIds += message.getJMSMessageID() + "\n";
                } catch (JMSException e) {
                    throw new ServletException(e);
                }
            	response.setContentType("text/plain");
                response.setHeader("Cache-Control", "no-cache");
                response.getWriter().print(messageIds);
            	
            }else {
            	// lets turn the HTTP post into a JMS Message
                try {
                    String action = request.getParameter("action");
                    String clientId = request.getParameter("clientId");
                    if (action != null && clientId != null && action.equals("unsubscribe")) {
                    	log.info("Unsubscribing client " + clientId);
                    	client = getJmsWebClient( request );
                    	client.close();
                    	jmsWebClients.remove(clientId);
                        return;
                    }

					String text = getPostedMessageBody(request);

					// lets create the destination from the URI?
					Destination requestDestination = getDestination(client, request);
					if (requestDestination == null) {
						throw new NoDestinationSuppliedException();
					}
					Destination reponseDestination = getResponseDestination(client, request);

					MessageConsumer consumer = client.getConsumer(reponseDestination,
							request.getHeader(WebClient.selectorName));

//                        consumer.setMessageListener( message -> {
//                        	try {
//            					@SuppressWarnings("unused")
//            					String replyMsg = ((TextMessage)message).getText();
//            				} catch (JMSException e) {
//            					// TODO Auto-generated catch block
//            					log.error(e);
//            				}
//            				            	
//                        });
					boolean sync = isSync(request);
					TextMessage message = client.getSession().createTextMessage(text);

					appendParametersToMessage(request, message);
					message.setJMSReplyTo(reponseDestination);
					boolean persistent = isSendPersistent(request);
					int priority = getSendPriority(request);
					long timeToLive = getSendTimeToLive(request);
					client.send(requestDestination, message, persistent, priority, timeToLive);
					if (log.isDebugEnabled()) {
						log.debug("Sent to destination: " + requestDestination + " body: " + text);
					}
					Message replyMessage = consumer.receive(maximumReadTimeout);
					log.info("replyMsg : "+replyMessage);
					String replyMsg = ((TextMessage) replyMessage).getText();

					response.setContentType(getContentType(request));

					// lets return a unique URI for reliable messaging
					response.setHeader("messageID", message.getJMSMessageID());
					response.setStatus(HttpServletResponse.SC_OK);
//                        response.getWriter().write("Message sent");
//                        response.flushBuffer();
                }catch (JMSException e) {
        	        throw new ServletException("Could not post JMS message: " + e, e);
        	    }
            }

            
        }
    	
        
	} 
    
    private Destination getResponseDestination(WebClient client, HttpServletRequest request) throws JMSException {
    	String destinationName = request.getParameter(destinationParameter);
        if (destinationName == null  || destinationName.equals("")) {
            if (defaultResponseDestination == null) {
                return getResponseDestinationFromURI(client, request);
            } else {
                return defaultResponseDestination;
            }
        }

        return getDestination(client, request, destinationName+"."+request.getSession().getId());
	}
    
	private Destination getRequestDestination(WebClient client, HttpServletRequest request) throws JMSException {
    	String destinationName = request.getParameter(destinationParameter);
        if (destinationName == null  || destinationName.equals("")) {
            if (defaultRequestDestination == null) {
                return getDestinationFromURI(client, request);
            } else {
                return defaultRequestDestination;
            }
        }

        return getDestination(client, request, destinationName);
    }
	
	/**
     * @return the destination to use for the current request using the relative
     *         URI from where this servlet was invoked as the destination name
     */

    protected Destination getResponseDestinationFromURI(WebClient client, HttpServletRequest request) throws JMSException {
    	HttpSession session = request.getSession();
    	String sessionId = session.getId();
        String uri = request.getPathInfo();
        if (uri == null) {
            return null;
        }

        // replace URI separator with JMS destination separator
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
            if (uri.length() == 0) {
                return null;
            }
        }

        uri = uri.replace('/', '.');
        log.debug("destination uri=" + uri);
        return getDestination(client, request, uri+"."+sessionId);
    }

    /**
     * @return the timeout value for read requests which is always >= 0 and <=
     *         maximumReadTimeout to avoid DoS attacks
     */
    protected long getReadTimeout(HttpServletRequest request) {
        long answer = defaultReadTimeout;

        String name = request.getParameter(readTimeoutParameter);
        if (name != null) {
            answer = asLong(name);
        }
        if (answer < 0 || answer > maximumReadTimeout) {
            answer = maximumReadTimeout;
        }
        return answer;
    }
    
    /**
     * Reads a message from a destination up to some specific timeout period
     *
     * @param client The webclient
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doMessages(AjaxWebClient client, HttpServletRequest request, HttpServletResponse response) throws JMSException, IOException {

        int messages = 0;
        // This is a poll for any messages

        long timeout = getReadTimeout(request);
        if (log.isDebugEnabled()) {
        	log.debug("doMessage timeout=" + timeout);
        }

        // this is non-null if we're resuming the continuation.
        // attributes set in AjaxListener
        UndeliveredJmsWebMessage undelivered_message = null;
        Message message = null;
        undelivered_message = (UndeliveredJmsWebMessage)request.getAttribute("undelivered_message");
        if( undelivered_message != null ) {
            message = undelivered_message.getMessage();
        }

        synchronized (client) {

            List<MessageConsumer> consumers = client.getConsumers();
            MessageAvailableConsumer consumer = null;
            if( undelivered_message != null ) {
                consumer = (MessageAvailableConsumer)undelivered_message.getConsumer();
            }

            if (message == null) {
                // Look for a message that is ready to go
                for (int i = 0; message == null && i < consumers.size(); i++) {
                    consumer = (MessageAvailableConsumer)consumers.get(i);
                    if (consumer.getAvailableListener() == null) {
                        continue;
                    }

                    // Look for any available messages
                    message = consumer.receive(10);
                    if (log.isDebugEnabled()) {
                    	log.debug("received " + message + " from " + consumer);
                    }
                }
            }

            // prepare the response
            response.setContentType("text/xml");
            response.setHeader("Cache-Control", "no-cache");

            if (message == null && client.getListener().getUndeliveredMessages().size() == 0) {
                Continuation continuation = ContinuationSupport.getContinuation(request);

                // Add a listener to the continuation to make sure it actually
                // will expire (seems like a bug in Jetty Servlet 3 continuations,
                // see https://issues.apache.org/jira/browse/AMQ-3447
                continuation.addContinuationListener(new ContinuationListener() {
                    @Override
                    public void onTimeout(Continuation cont) {
                        if (log.isDebugEnabled()) {
                            log.debug("Continuation " + cont.toString() + " expired.");
                        }
                    }

                    @Override
                    public void onComplete(Continuation cont) {
                        if (log.isDebugEnabled()) {
                           log.debug("Continuation " + cont.toString() + " completed.");
                        }
                    }
                });

                if (continuation.isExpired()) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    StringWriter swriter = new StringWriter();
                    PrintWriter writer = new PrintWriter(swriter);
                    writer.println("<ajax-response>");
                    writer.print("</ajax-response>");

                    writer.flush();
                    String m = swriter.toString();
                    response.getWriter().println(m);

                    return;
                }

                continuation.setTimeout(timeout);
                continuation.suspend();
                log.debug( "Suspending continuation " + continuation );

                // Fetch the listeners
                AjaxListener listener = client.getListener();
                listener.access();

                // register this continuation with our listener.
                listener.setContinuation(continuation);

                return;
            }

            StringWriter swriter = new StringWriter();
            PrintWriter writer = new PrintWriter(swriter);

            Map<MessageAvailableConsumer, String> consumerIdMap = client.getIdMap();
            Map<MessageAvailableConsumer, String> consumerDestinationNameMap = client.getDestinationNameMap();
            response.setStatus(HttpServletResponse.SC_OK);
            writer.println("<ajax-response>");

            // Send any message we already have
            if (message != null) {
                String id = consumerIdMap.get(consumer);
                String destinationName = consumerDestinationNameMap.get(consumer);
                log.debug( "sending pre-existing message" );
                writeMessageResponse(writer, message, id, destinationName);

                messages++;
            }

            // send messages buffered while continuation was unavailable.
            LinkedList<UndeliveredJmsWebMessage> undeliveredMessages = ((JmsWebListener)consumer.getAvailableListener()).getUndeliveredMessages();
            log.debug("Send " + undeliveredMessages.size() + " unconsumed messages");
            synchronized( undeliveredMessages ) {
                for (Iterator<UndeliveredJmsWebMessage> it = undeliveredMessages.iterator(); it.hasNext();) {
                    messages++;
                    UndeliveredJmsWebMessage undelivered = it.next();
                    Message msg = undelivered.getMessage();
                    consumer = (MessageAvailableConsumer)undelivered.getConsumer();
                    String id = consumerIdMap.get(consumer);
                    String destinationName = consumerDestinationNameMap.get(consumer);
                    log.debug( "sending undelivered/buffered messages" );
                    log.debug( "msg:" +msg+ ", id:" +id+ ", destinationName:" +destinationName);
                    writeMessageResponse(writer, msg, id, destinationName);
                    it.remove();
                    if (messages >= maximumMessages) {
                        break;
                    }
                }
            }

            // Send the rest of the messages
            for (int i = 0; i < consumers.size() && messages < maximumMessages; i++) {
                consumer = (MessageAvailableConsumer)consumers.get(i);
                if (consumer.getAvailableListener() == null) {
                    continue;
                }

                // Look for any available messages
                while (messages < maximumMessages) {
                    message = consumer.receiveNoWait();
                    if (message == null) {
                        break;
                    }
                    messages++;
                    String id = consumerIdMap.get(consumer);
                    String destinationName = consumerDestinationNameMap.get(consumer);
                    log.debug( "sending final available messages" );
                    writeMessageResponse(writer, message, id, destinationName);
                }
            }

            writer.print("</ajax-response>");

            writer.flush();
            String m = swriter.toString();
            response.getWriter().println(m);
        }
    }
    protected void writeMessageResponse(PrintWriter writer, Message message, String id, String destinationName) throws JMSException, IOException {
        writer.print("<response id='");
        writer.print(id);
        writer.print("'");
        if (destinationName != null) {
            writer.print(" destination='" + destinationName + "' ");
        }
        writer.print(">");
        if (message instanceof TextMessage) {
            TextMessage textMsg = (TextMessage)message;
            String txt = textMsg.getText();
            if (txt != null) {
                if (txt.startsWith("<?")) {
                    txt = txt.substring(txt.indexOf("?>") + 2);
                }
                writer.print(txt);
            }
        } else if (message instanceof ObjectMessage) {
            ObjectMessage objectMsg = (ObjectMessage)message;
            Object object = objectMsg.getObject();
            if (object != null) {
                writer.print(object.toString());
            }
        }
        writer.println("</response>");
    }
    /*
     * an instance of this class runs every minute (started in init), to clean up old web clients & free resources.
     */
    private class ClientCleaner extends TimerTask {
        @Override
        public void run() {
            if( log.isDebugEnabled() ) {
                log.debug( "Cleaning up expired web clients." );
            }

            synchronized( jmsWebClients ) {
                Iterator<Map.Entry<String, JmsWebClient>> it = jmsWebClients.entrySet().iterator();
                while ( it.hasNext() ) {
                    Map.Entry<String,JmsWebClient> e = it.next();
                    String key = e.getKey();
                    AjaxWebClient val = e.getValue();
                    if ( log.isDebugEnabled() ) {
                        log.debug( "AjaxWebClient " + key + " last accessed " + val.getMillisSinceLastAccessed()/1000 + " seconds ago." );
                    }
                    // close an expired client and remove it from the ajaxWebClients hash.
                    if( val.closeIfExpired() ) {
                        if ( log.isDebugEnabled() ) {
                            log.debug( "Removing expired JmsWebClient " + key );
                        }
                        it.remove();
                    }
                }
            }
        }
    }
}
