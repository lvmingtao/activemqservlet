package com.example.event;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.ThreadContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import com.example.util.msg.DocumentMessageUtil;
import com.example.util.xml.JdomUtils;

public class MessageEvent extends AbstractMessageEventTemplate {
	private static Log log = LogFactory.getLog(MessageEvent.class);

	private final String dataField_TAG = System.getProperty("Message");
	
	@Autowired
	private ApplicationContext appCtx;

	public MessageEvent() {
	}

	public String getTibrvMsgDataFieldTag() {
		return dataField_TAG;
	}

	public void onReceiveMessage(String beanName, Object data) {
		Document document = null;
		if (data instanceof Message) {
			DefaultMessageListenerContainer Listeners = appCtx.getBean(DefaultMessageListenerContainer.class);
			if(Listeners.getSessionAcknowledgeMode()==Session.CLIENT_ACKNOWLEDGE)
				try {
					((Message)data).acknowledge();
				} catch (JMSException e) {
					e.printStackTrace();
					return ;
				}
			document = DocumentMessageUtil.getDocumentFromJmsMessage((Message) data);
			try {
				Destination responseDestination = ((Message)data).getJMSReplyTo();
				JmsTemplate jmsTemplate = (JmsTemplate)appCtx.getBean("jmsTemplate");
				jmsTemplate.convertAndSend(responseDestination, JdomUtils.toString(document));
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(data instanceof HttpServletRequest ) {
			log.info("Http Request");
		}

		try {
			ThreadContext.put("MES.MSGNAME", JdomUtils.getNodeText(document, "//" + DocumentMessageUtil.Root_Tag + "/"
					+ DocumentMessageUtil.Header_Tag + "/" + DocumentMessageUtil.MessageName_Tag));
		} catch (Exception e2) {
			ThreadContext.put("MES.MSGNAME", "MessageParseERROR");
			e2.printStackTrace();
		}
		try {
			ThreadContext.put("MES.TRXID", JdomUtils.getNodeText(document, "//" + DocumentMessageUtil.Root_Tag + "/"
					+ DocumentMessageUtil.Header_Tag + "/" + DocumentMessageUtil.TransactionID_Tag));
		} catch (Exception e1) {
			ThreadContext.put("MES.TRXID", "00000000000000000000");
			e1.printStackTrace();
		}

		
		log.info("[" + beanName + "] execute");

		Element headerElement = null;
		try {
			String nodePath = "//" + DocumentMessageUtil.Root_Tag + "/" + DocumentMessageUtil.Header_Tag;

			headerElement = JdomUtils.getNode(document, nodePath);
			headerElement.getChild(DocumentMessageUtil.EventComment_Tag)
					.setText(DocumentMessageUtil.getEventComment(document));
			Element listenerElement = headerElement.getChild(DocumentMessageUtil.Listener_Tag);
			if (listenerElement == null)
				JdomUtils.addElement(headerElement, DocumentMessageUtil.Listener_Tag, beanName);
			else
				listenerElement.setText(beanName);

		} catch (Exception e) {
			e.printStackTrace();
		}

		String msgName = "";
		String svrName = "";
		String fullMessage = "";

		try {
			svrName = System.getProperty("svr");
			msgName = JdomUtils.getNodeText(document, "//Message/Header/MESSAGENAME");
		} catch (Exception e) {
		}

		// fullMessage = CommonUtil.substrMsgForLog(msgName, data.toString());

		/*
		 * if( StringUtils.startsWith(msgName, "ComponentPanelInByUnit") ||
		 * StringUtils.startsWith(msgName, "ComponentPanelOutByUnit") ||
		 * StringUtils.startsWith(msgName, "MachineStateChanged") ||
		 * StringUtils.startsWith(msgName, "ProductInfoRequest") ||
		 * StringUtils.startsWith(msgName, "ProductProcessData") ||
		 * StringUtils.startsWith(msgName, "ProductInspectionResultReport") ||
		 * StringUtils.startsWith(msgName, "UnitStateChanged") ) { int start =
		 * StringUtils.indexOf(data.toString(), "<Body>"); int messageLogSize; if(
		 * StringUtils.startsWith(msgName, "ComponentPanel")) { messageLogSize = 500;
		 * }else { messageLogSize = 300; }
		 * 
		 * fullMessage = StringUtils.substring(data.toString(), start, start +
		 * messageLogSize);
		 * 
		 * if( (data.toString().length() - start) > (start + messageLogSize) ) {
		 * fullMessage = fullMessage + "\n .....Skip..... "; } }else
		 */
		{
			fullMessage = data.toString();
		}
//		for(int i=100;i<10000;i++)
//		log.info("RCRQ : " + fullMessage);

		/*
		 * if ( !StringUtils.equals(svrName, "FMCsvr") && !(StringUtils.equals(svrName,
		 * "PEMsvr") && StringUtils.equals(msgName, "AlarmReport")) ) { // To write one
		 * line message log. String docString = JdomUtils.toString(document, false);
		 * log.info("MSG : " + docString.replace(System.getProperty("line.separator"),
		 * "")); }
		 */
		// Comment For one row log - Log Management by EnumValue Flag
		/*
		 * if(CommonUtil.getEnumDefValueStringByEnumName("OneRowLog").equals("Y")){
		 * log.info("RCRQ " + toCompactString(document)); }
		 */

		Object[] arguments = new Object[] { document };

		// One Thread
		// WorkflowServiceProxy.getBpelExecuter().executeProcess(arguments, false,
		// null);

		// Multi Thread
//		execute(arguments);
		//执行完成之后将本次的消息对应的log的 %X{MES.MSGNAME} %X{MES.TRXID} 内容清空
		ThreadContext.clearMap();
//		log.info("test");
	}

	/*
	 * @Override public void onBundleMessage(String beanName, Object data) {
	 * Document document = null; if (data instanceof TibrvMsg) { document =
	 * SMessageUtil.getDocumentFromTibrvMsg((TibrvMsg)data, dataField_TAG); }
	 * 
	 * try { MDC.put("MES.MSGNAME", JdomUtils.getNodeText(document,
	 * "//Message/Header/MESSAGENAME")); } catch (Exception e2) { // TODO
	 * Auto-generated catch block e2.printStackTrace(); } try { MDC.put("MES.TRXID",
	 * JdomUtils.getNodeText(document, "//Message/Header/TRANSACTIONID")); } catch
	 * (Exception e1) { // TODO Auto-generated catch block e1.printStackTrace(); }
	 * 
	 * log.info("["+ beanName + "] execute"); Element headerElement = null; try {
	 * String nodePath = "//Message/Header";
	 * 
	 * headerElement = JdomUtils.getNode(document, nodePath);
	 * headerElement.getChild("EVENTCOMMENT").setText(CommonUtil.setEventComment(
	 * document)); Element listenerElement = headerElement.getChild("listener"); if(
	 * listenerElement== null ) JdomUtils.addElement(headerElement, "listener",
	 * beanName); else listenerElement.setText(beanName);
	 * 
	 * } catch (Exception e) { e.printStackTrace(); }
	 * 
	 * String svrName = System.getProperty("svr"); String msgName = ""; try {
	 * msgName = JdomUtils.getNodeText(document, "//Message/Header/MESSAGENAME"); }
	 * catch (Exception e) { }
	 * 
	 * log.info("RCRQ : " + data.toString());
	 * 
	 * // if(svrName.equals("PEMsvr") || svrName.equals("CNMsvr") ||
	 * svrName.equals("TEMsvr") || svrName.equals("FMCsvr")) // { //
	 * if(data.toString().length() >= 270){ // log.info("RCRQ : " +
	 * data.toString().substring(0, 270) + "\n .....Skip..... "); // } // else{ //
	 * log.info("RCRQ : " + data.toString()); // } // // } // else
	 * if(svrName.equals("EDCsvr")) // { // if(data.toString().length() >= 620) // {
	 * // log.info("RCRQ : " + data.toString().substring(0, 620) +
	 * "\n .....Skip..... "); // } // else // { // log.info("RCRQ : " +
	 * data.toString()); // } // } // else // { // if(svrName.equals("PEXsvr")) // {
	 * // if(data.toString().length() >= 270) // { // log.info("RCRQ : " +
	 * data.toString().substring(0, 270) + "\n .....Skip..... "); // } // else // {
	 * // log.info("RCRQ : " + data.toString()); // } // } // else // { //
	 * log.info("RCRQ : " + data.toString()); // } // } // Comment 2013.04.03 For
	 * one row log - Log Management by EnumValue Flag
	 * if(CommonUtil.getEnumDefValueStringByEnumName("OneRowLog").equals("Y")){
	 * log.info("RCRQ " + toCompactString(document)); }
	 * 
	 * Object[] arguments = new Object[]{document};
	 * 
	 * // One Thread
	 * //WorkflowServiceProxy.getBpelExecuter().executeProcess(arguments, false,
	 * null);
	 * 
	 * //Multi Thread execute(arguments);
	 * 
	 * }
	 */

	public String toCompactString(Document document) {
		XMLOutputter out = new XMLOutputter();
		Format format = Format.getCompactFormat();
		out.setFormat(format);

		return out.outputString(document.getRootElement());
	}

}