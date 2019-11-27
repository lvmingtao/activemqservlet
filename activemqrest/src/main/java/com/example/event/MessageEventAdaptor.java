package com.example.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.example.esb.IListener;


@Component
public class MessageEventAdaptor implements ApplicationContextAware
{
	private static Hashtable<String, List<String>>	eventList = new Hashtable<String, List<String>>();			// must be static : 
	private static Log								log	= LogFactory.getLog(MessageEventAdaptor.class);
	private Map<String, IListener>					listeners = new HashMap<String, IListener>();
	
	private ApplicationContext applicationContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}	

	public void registerEvent( String listenerBeanName, String adaptorBeanName )
	{
		List<String> listenerList = eventList.get( adaptorBeanName );

		if ( listenerList == null )
		{
			listenerList = new ArrayList<String>();
			eventList.put( adaptorBeanName, listenerList );
		}

		if ( !listenerList.contains( listenerBeanName )) 
			listenerList.add( listenerBeanName );
	}
	
	public void registerListener( String beanName, IListener listener )
	{
		this.listeners.put( beanName, listener );
	}


	public void notifyEvent( String adaptorBeanName, Object data )
	{
		List<String> listenerList = eventList.get( adaptorBeanName );
		if ( listenerList == null || listenerList.size() == 0 )
		{
			log.warn( "No registered EventListener [" + adaptorBeanName + "], please check formatter bundle xml files." );
			return;
		}
		for ( String listenerBeanName : listenerList )
		{
			Object obj = null;
			try
			{
				obj = applicationContext.getBean(listenerBeanName);
				if (obj == null)
					obj = applicationContext.getBean( listenerBeanName );
				(( IMessageEvent )obj ).onReceiveMessage( adaptorBeanName, data );
			}
			catch ( Exception e )
			{
				log.error( e, e );
			}
		}
	}

	public void terminate()
	{
		Iterator<Entry<String, IListener>> iter = this.listeners.entrySet().iterator();
		
		while ( iter.hasNext())
		{
			Entry<String, IListener> entry = iter.next();
			
			try
			{
				log.info( "Closing listener [" + entry.getKey() + "]" );
				entry.getValue().unListen();
			}
			catch (Exception e)
			{}
		}
	}
}
