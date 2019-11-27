package com.example.esb;

/**
 ****************************************************************************
 *  PACKAGE : com.cim.idm.framework.esb <br>
 *  NAME    : BeanBasedSenderProxy.java <br>
 *  TYPE    : JAVA <br>
 *  DESCRIPTION : Receive Tib Messages via this Listener.
 *
 ****************************************************************************
 */

public interface IListener
{
	public void listen() throws Exception;
	public void unListen() throws Exception;
}
