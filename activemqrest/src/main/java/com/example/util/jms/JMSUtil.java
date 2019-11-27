package com.example.util.jms;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;

public class JMSUtil
{

	public static Object getMessageBody(Message message) throws Exception
	{
		if (message instanceof TextMessage)
		{
			TextMessage textMessage = (TextMessage) message;
			return textMessage.getText();
		}
		else if (message instanceof BytesMessage)
		{
			BytesMessage bytesMessage = (BytesMessage) message;
			byte[] data = new byte[(int) bytesMessage.getBodyLength()];
			bytesMessage.readBytes(data);
			return data;
		}
		else if (message instanceof ObjectMessage)
		{
			ObjectMessage objectMessage = (ObjectMessage) message;
			return objectMessage.getObject();
		}
		else
		{
			throw new Exception("Unsupported Message Type. " + message.getClass());
		}
	}

	public static String getReplyDestinationFullName(Message message) throws JMSException
	{
		if (message.getJMSReplyTo() == null)
			return null;
		return message.getJMSReplyTo().toString() + ", JMSCorrelationID[" + message.getJMSCorrelationID() + "]";
	}

	public static String getJMSCorrelationID(String destinationFullName)
	{
		if (destinationFullName == null)
			return null;
		return StringUtils.substringBetween(destinationFullName, ", JMSCorrelationID[", "]");
	}

	public static String getJMSDestinationID(String destinationFullName)
	{
		if (destinationFullName == null)
			return null;
		return StringUtils.substringBefore(destinationFullName, ", JMSCorrelationID[");
	}
}
