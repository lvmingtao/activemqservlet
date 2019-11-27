package com.example.esb;

/**
 ****************************************************************************
 *  PACKAGE : com.cim.idm.framework.esb <br>
 *  NAME    : BeanBasedSenderProxy.java <br>
 *  TYPE    : JAVA <br>
 *  DESCRIPTION : send Request via this requester.
 *
 ****************************************************************************
 */

public interface IRequester
{
	public void send(Object message);

	public void send(String subjectName, Object message);

	public void reply(String subjectName, Object message);

	public Object sendRequest(Object message);
	
	public Object sendRequest(Object message, int replyTimeout);

	public Object sendRequest(String subjectName, Object message);

	public Object sendRequest(String subjectName, Object message, int replyTimeout);
}
