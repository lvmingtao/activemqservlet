package com.example.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.web.AjaxServlet;
import org.apache.activemq.web.MessageServlet;
import org.apache.activemq.web.SessionFilter;
import org.apache.activemq.web.SessionListener;
import org.apache.activemq.web.WebClient;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import com.example.esb.activemq.JmsListener;
import com.example.esb.activemq.web.JmsServlet;
import com.example.event.MessageEvent;

@Configuration
public class ServletConfig {

//	@Value("${spring.activemq.broker-url}")
//	private String brokerUrl;
	@Value("${cnm.jms.listensubject}")
	private String listensubject;
	@Autowired
	private ApplicationContext applicationContext;
	
/*	@WebServlet(asyncSupported=true,loadOnStartup=1,urlPatterns="/message/*")
	public class MyServlet extends MessageServlet {
		//...
	}
	
	@WebFilter(asyncSupported=true,urlPatterns="/*")
	public class MyFilter extends SessionFilter {
		//...
	}
	
	@WebListener()
	public class MyListener extends SessionListener {
		//...
	}*/
	
	@Bean
	public ServletContextInitializer initializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
            	servletContext.setInitParameter("org.apache.activemq.embeddedBroker","false");
//                servletContext.setInitParameter(WebClient.BROKER_URL_INIT_PARAM, brokerUrl);
            	// httpHeader 中可以通过 header="selector: test=1", 表示只消费consume处理 test属性为1 的消息
                servletContext.setInitParameter(WebClient.SELECTOR_NAME,"selector");//default is selector
                servletContext.setAttribute(WebClient.CONNECTION_FACTORY_ATTRIBUTE, ((JmsPoolConnectionFactory) applicationContext.getBean(JmsPoolConnectionFactory.class)).getConnectionFactory());
            }
        };
    }

	
	@Bean
	public ServletRegistrationBean MessageServletRegistrationBean() {  //一定要返回ServletRegistrationBean
		ServletRegistrationBean bean = new ServletRegistrationBean(new JmsServlet());     //放入自己的Servlet对象实例
        bean.addUrlMappings("/message/*");  //访问路径值
        bean.setAsyncSupported(true);
        Map<String,String> initParameters = new HashMap<>();
        initParameters.put("destination", listensubject);
        initParameters.put("topic", "false");
        initParameters.put("maximumReadTimeout", "10000");
        initParameters.put("replyTimeout", "10000");
        initParameters.put(WebClient.CONNECTION_FACTORY_OPTIMIZE_ACK_PARAM, "true");
        initParameters.put(WebClient.CONNECTION_FACTORY_PREFETCH_PARAM, "1");
//        servletContext.setInitParameter(WebClient.USERNAME_INIT_PARAM, "admin");
//        servletContext.setInitParameter(WebClient.PASSWORD_INIT_PARAM, "admin");
        bean.setInitParameters(initParameters);
        bean.setLoadOnStartup(1);
        return bean;
	}
	
//	@Bean
//	public ServletRegistrationBean getAjaxServletRegistrationBean() {  //一定要返回ServletRegistrationBean
//		ServletRegistrationBean bean = new ServletRegistrationBean(new AjaxServlet());     //放入自己的Servlet对象实例
//        bean.addUrlMappings("/amq/*");  //访问路径值
//        bean.setAsyncSupported(true);
//        bean.setLoadOnStartup(1);
//        return bean;
//	}
//	
//	@Bean
//    public FilterRegistrationBean sessionFilter() {
//        FilterRegistrationBean sessionFilter = new FilterRegistrationBean(new SessionFilter());
//        sessionFilter.addUrlPatterns("/*");
//        sessionFilter.setAsyncSupported(true);
//        return sessionFilter;
//    }
	
//	@Bean
//    public ServletListenerRegistrationBean<SessionListener> setStartupServletContextListener(){
//    	ServletListenerRegistrationBean<SessionListener> result = new ServletListenerRegistrationBean<>();
//    	result.setListener(new SessionListener());
//    	result.setOrder(20);
//    	return result;
//    }
//	
	
	@Bean(initMethod="init",name="CNMQueueListener")
	public JmsListener CNMListener() {
		
		JmsListener listener = new JmsListener();
		
		return new JmsListener();
	}
	
	@Bean
	public DefaultMessageListenerContainer jmsContainer() {
		DefaultMessageListenerContainer defaultMessageListenerContainer = new DefaultMessageListenerContainer();
		defaultMessageListenerContainer.setConnectionFactory((ConnectionFactory) applicationContext.getBean(JmsPoolConnectionFactory.class));
		defaultMessageListenerContainer.setMessageListener(applicationContext.getBean(JmsListener.class));
		defaultMessageListenerContainer.setDestination((Destination) applicationContext.getBean("CNMQueue"));
		
		//设置并发数 "最小值 - 最大值"
		defaultMessageListenerContainer.setConcurrency("5-10");
		/*设置消息确认模式
		 * 1. Session.AUTO_ACKNOWLEDGE
		 * 2. Session.CLIENT_ACKNOWLEDGE
		 */
		defaultMessageListenerContainer.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		return defaultMessageListenerContainer;
	}
	
	@Bean(name="CNMQueue")
	public Queue CNMQueue() {
		return new ActiveMQQueue(listensubject);
	}
	
	
//	@Bean(name="CNMTopic")
//	public Topic CNMTopic() {
//		return new ActiveMQTopic("com.boe.mes.cnm.topic");
//	}
	
	@Bean(name="jmsTemplate") 
	public JmsTemplate jmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(applicationContext.getBean(JmsPoolConnectionFactory.class));
		jmsTemplate.setMessageConverter(new SimpleMessageConverter());
		return jmsTemplate;
	}
	
	@Bean
	public MessageEvent messageEvent() {
		MessageEvent messageEvent = new MessageEvent();

		messageEvent.setListenBeanNameList(Arrays.asList(new String[] { "CNMQueueListener" }));
		return messageEvent;
	}
}
