package com.example.esb.activemq;

import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.example.esb.IListener;
import com.example.event.MessageEventAdaptor;

public class JmsListener implements MessageListener, ApplicationContextAware, IListener {
private static Log			log					= LogFactory.getLog(JmsListener.class);
	
	private ApplicationContext applicationContext;
	
	private String beanName;
	
	private MessageEventAdaptor messageEventAdaptor = null;

	@Override
	public void onMessage(Message message) {
//		System.out.println("收到消息！！！！" + message);
		try {
			message.acknowledge();
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			Map map = this.applicationContext.getBeansOfType(this.getClass());
			Set set = map.keySet();
			Object[] hmKeys = set.toArray();

			if (messageEventAdaptor == null)
				messageEventAdaptor = this.applicationContext.getBean(MessageEventAdaptor.class);

			if (messageEventAdaptor != null) {
				for (int i = 0; i < hmKeys.length; i++) {
					String key = (String) hmKeys[i];
					try {
						Object obj = this.applicationContext.getBean(key);

						if (obj.equals(this))
							messageEventAdaptor.notifyEvent(key, message);
					} catch (Exception e) {
						log.error(e, e);
					}
				}
			}
		} catch (Exception ex) {
			this.log.error(ex, ex);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void listen() throws Exception {
//		afterPropertiesSet();
		init();
	}

	@Override
	@PreDestroy
	public void unListen() throws Exception {
//		destroy();
	}

	@SuppressWarnings( "unchecked" )
	protected void setBeanName()
	{
		Map<String, ? extends JmsListener> map = this.applicationContext.getBeansOfType(this.getClass());
		//java 8 forEach
		map.forEach((key,value)-> {
			if(this.equals(value)) {
				this.beanName = key;
			}
		});		
	}
	
	public void init() throws Exception {
		setBeanName();

		

			try {

				messageEventAdaptor = (MessageEventAdaptor) this.applicationContext.getBean(MessageEventAdaptor.class);
				messageEventAdaptor.registerListener(this.beanName, this);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
				log.info("Cannot reference MessageEventAdaptor, so system will be Exit.");
//				System.exit(0);
			}

	}
}
