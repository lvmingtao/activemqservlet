package com.example.event;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.example.esb.IListener;



public abstract class AbstractMessageEventTemplate implements ApplicationContextAware, IMessageEvent
{
	public ApplicationContext	applicationContext;
	private static Log			log	= LogFactory.getLog(AbstractMessageEventTemplate.class);
	private List<String>		listenBeanNames;
	private MessageEventAdaptor messageEventAdaptor = null;
	
	public AbstractMessageEventTemplate()
	{
	}

	public List<String> getListenBeanNameList()
	{
		return listenBeanNames;
	}

	public void setListenBeanNameList(List<String> beanNameList)
	{
		this.listenBeanNames = beanNameList;
	}

	public void setApplicationContext(ApplicationContext arg0) throws BeansException
	{
		applicationContext = arg0;
		init();
	}

	
	public abstract void onReceiveMessage(String className, Object data);

	private void init()
	{
		if (this.listenBeanNames == null || this.listenBeanNames.size() == 0)
		{
			log.warn("No registered targetServiceBeans for MessageEvent");
			return;
		}
		
		try
		{
			Map map = this.applicationContext.getBeansOfType(this.getClass());
			Set set = map.keySet();
			Object[] hmKeys = set.toArray();

		
			messageEventAdaptor = (MessageEventAdaptor) this.applicationContext
					.getBean(MessageEventAdaptor.class);

			for (Object obj : hmKeys)
			{
				for (String name : this.listenBeanNames)
				{
					messageEventAdaptor.registerEvent(obj.toString(), name);
					try {
						IListener listener = (IListener)this.applicationContext.getBean(name);
						messageEventAdaptor.registerListener(name,listener);
					}catch(Exception e) {
						log.error(e);
					}
				}
			}
		} 
		catch (Exception e)
		{
			log.error(e, e);
		}
	}

	
}
