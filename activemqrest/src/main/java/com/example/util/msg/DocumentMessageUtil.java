package com.example.util.msg;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import com.example.util.jms.JMSUtil;
import com.example.util.xml.JdomUtils;

/**
 ****************************************************************************
 *  PACKAGE : com.cim.idm.framework.util.msg <br>
 *  NAME    : MessageUtil.java <br>
 *  TYPE    : JAVA <br>
 *  DESCRIPTION : Message communication related function.
 *
 ****************************************************************************
 */
public class DocumentMessageUtil
{

	private static Log			log						= LogFactory.getLog(DocumentMessageUtil.class);

	public static final String	Command_Tag				= "COMMAND";

	private static final String	ReplySubjectName_Tag	= "ORIGINALSOURCESUBJECTNAME";
	
	public static final String Root_Tag = "Message";

	public static final String Header_Tag = "Header";
	public static final String MessageName_Tag = "MESSAGENAME";
	public static final String TransactionID_Tag = "TRANSACTIONID";
	public static final String Listener_Tag = "listener";
	public static final String EventUser_Tag = "EVENTUSER";
	public static final String EventComment_Tag = "EVENTCOMMENT";
	public static final String Language_Tag = "LANGUAGE";

	public static final String Body_Tag = "Body";

	public static final String Result_Tag = "Return";
	public static final String Result_ReturnCode = "RETURNCODE";
	public static final String Result_ErrorMessage = "RETURNMESSAGE";

	/**
	 * Get the specific return item value from document file.
	 * @param doc
	 * @param nodeName
	 * @return ResultItemValue
	 * @see
	 */
	public static String getResultItemValue(Document doc, String nodeName)
	{
		try
		{
			return (String) JdomUtils.getNodeText(doc, "//" + Root_Tag + "/" + Result_Tag + "/" + nodeName);
		} catch (Exception e)
		{
			//e.printStackTrace();                                                                
		}
		return "";
	}

	/**
	 * Get the bpel Name from document file.
	 * @param doc
	 * @return BpelName
	 * @see
	 */
	public static String getBpelName(Document doc)
	{
		try
		{
			return (String) JdomUtils.getNodeText(doc, "//" + Root_Tag + "/" + Header_Tag + "/" + MessageName_Tag);
		} catch (Exception e)
		{}
		return "";
	}
	
	public static String getListenerBpelName(Document doc)                                    
	{                   
		String Listener = null;
		try {           
			Listener = (String) JdomUtils.getNodeText(doc, "//" + Root_Tag + "/" + Header_Tag + "/" + Listener_Tag);
			if (Listener != null && Listener.length() > 0) {
				if (!Listener.toLowerCase().endsWith(".bpel"))
					Listener = Listener + ".bpel";
			}
		} catch (Exception e) {                                                                 
		}                                                                                       
		return Listener;                                                                              
	}   
	
	public static String getEventComment(Document doc) {
		// IF MESSAGESET IS NOT EVENTCOMMENT, INSERT MESSAGENAME TO EVENTCOMMENT
		Element root = doc.getDocument().getRootElement();
		String messageName = root.getChild(Header_Tag).getChild(MessageName_Tag).getText();
		String eventComment = root.getChild(Header_Tag).getChild(EventComment_Tag).getText();
		if (eventComment == null || eventComment.isEmpty()) {
			eventComment = messageName;
		}
		return eventComment;
	}

	/**
	 * set Element value for a item.
	 * @param nodePath
	 * @param doc
	 * @param nodeName
	 * @param nodeValue
	 * @return 
	 * @see
	 */
	private static void setElement(String nodePath, Document doc, String nodeName, String nodeValue)
	{
		if (nodeValue == null)
			nodeValue = "";
		try
		{
			Element element = JdomUtils.getNode(doc, nodePath + nodeName);
			if (element != null)
			{
				element.setContent(new CDATA(nodeValue));
			}
			else
			{
				//Element createElement = JdomUtils.createElement(nodeName, valueKey);
				if (nodePath.substring(nodePath.length() - 1, nodePath.length()).equalsIgnoreCase("/"))
					nodePath = nodePath.substring(0, nodePath.length() - 1);
				Element parentElement = JdomUtils.getNode(doc, nodePath);
				Element newElement = JdomUtils.addElement(parentElement, nodeName, nodeValue);
				newElement.setContent(new CDATA(nodeValue));
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * set Element value for a result item.
	 * @param doc
	 * @param nodeName
	 * @param nodeValue
	 * @return 
	 * @see
	 */
	public static void setResultItemValue(Document doc, String nodeName, String nodeValue)
	{
		if (doc.getRootElement().getChild(Result_Tag) == null)
			doc.getRootElement().addContent(new Element(Result_Tag));

		String nodePath = "//" + Root_Tag + "/" + Result_Tag + "/";
		setElement(nodePath, doc, nodeName, nodeValue);

		if (nodeName.equalsIgnoreCase(Result_ReturnCode) && !nodeValue.equalsIgnoreCase("0"))
			log.error(nodeName + " : " + nodeValue);
		else if (nodeName.equalsIgnoreCase(Result_ReturnCode) && nodeValue.equalsIgnoreCase("0"))
			log.info(Result_ReturnCode + " : 0");
		else if (nodeName.equalsIgnoreCase(Result_ErrorMessage) && nodeValue.length() > 0)
			log.error(nodeName + " : " + nodeValue);
		else if (nodeName.equalsIgnoreCase(Result_ErrorMessage) && nodeValue.length() == 0)
			log.error(nodeName + " : ");

		//log.debug("[" + nodeName + "] = " + nodeValue);
	}

	/**
	 * convert body part from xml file.
	 * @param message
	 * @return Document
	 * @see
	 */
	public static Document getDocumentFromJmsMessage(Message message)
	{
		Document document = null;
		Object obj = null;
		try
		{
			obj = JMSUtil.getMessageBody(message);
			if (obj instanceof String)
				document = JdomUtils.loadText((String) obj);
			else if (obj instanceof Document)
				return (Document) obj;
			else if ( obj instanceof byte[] )
			{
				return JdomUtils.loadText(new String((byte[])obj));
			}
			else
			{
				log.warn("Could not convert xml.document type, because input data is not string");
				return null;
			}
		} catch (Exception e)
		{
			try
			{
				if (obj instanceof String)
					document = createXmlDocument((String) obj);
			} catch (Exception ex)
			{
				log.error(ex, ex);
				return null;
			}
		}
		try
		{
			if (message.getJMSReplyTo() != null)
				setItemValue(document, Header_Tag, ReplySubjectName_Tag, JMSUtil.getReplyDestinationFullName(message));
		} catch (Exception e)
		{}
		return document;
	}

	
	
	/**
	 * 
	 * @param data
	 * @param dataFieldTag
	 * @return Document
	 * @see
	 */
	

	public static Document createXmlDocumentForNT(String bpelName) throws Exception
	{
		Element envelop = new Element(Root_Tag);
		Document document = new Document(envelop);

		Element header = new Element(Header_Tag);

		Element subElement = new Element(MessageName_Tag);
		subElement.setText(bpelName);
		header.addContent(subElement);

		subElement = new Element(Command_Tag);
		subElement.setText(bpelName);
		header.addContent(subElement);
		envelop.addContent(header);
		Element body = new Element(Body_Tag);
		envelop.addContent(body);
		log.debug(JdomUtils.toString(document));
		return document;
	}

	private static Document createXmlDocument(String receivedData) throws Exception
	{
		Element root = new Element(Root_Tag);
		Document document = new Document(root);

		int idx = receivedData.indexOf(" ");
		String commandName = receivedData.substring(0, idx).trim();
		Map<String, String> messageMap = null;
		messageMap = parsingStringMessage(receivedData);

		Element header = new Element(Header_Tag);

		Element subElement = new Element(MessageName_Tag);
		subElement.setText(commandName);
		header.addContent(subElement);

		subElement = new Element(Command_Tag);
		subElement.setText(commandName);
		header.addContent(subElement);
		root.addContent(header);
		Element body = new Element(Body_Tag);
		Element ele = null;
		while (messageMap.keySet().iterator().hasNext())
		{
			String keyName = messageMap.keySet().iterator().next();
			String keyValue = messageMap.remove(keyName);
			ele = new Element(keyName);
			ele.setText(keyValue);
			body.addContent(ele);
		}
		root.addContent(body);
		messageMap.clear();
		log.debug(JdomUtils.toString(document));
		return document;
	}

	private static Map parsingStringMessage(String msg)
	{
		String delimeter = "[{X*X}]"; 

		String msg2 = msg;
		while (true)
		{
			int idx1 = msg2.indexOf("=[");
			int idx2 = msg2.indexOf("]", idx1);
			if (idx1 > 0 && idx2 > 0)
			{
				String a = msg2.substring(idx1 + 2, idx2);
				if (a.length() == 0)
				{
					msg2 = msg2.substring(idx2 + 1, msg2.length());
					continue;
				}
				String b = org.springframework.util.StringUtils.replace(a, "=", "($%^)");
				msg = org.springframework.util.StringUtils.replace(msg, a, b);
				msg2 = msg2.substring(idx2, msg2.length());
			}
			else
				break;
		}

		String[] messageSplit = msg.split("=");
		Map<String, String> params = new HashMap<String, String>();
		StringBuffer keyNameBuffer = new StringBuffer();
		keyNameBuffer.append(Command_Tag).append(delimeter);
		StringBuffer valueBuffer = new StringBuffer();
		for (int i = 0; i < messageSplit.length; i++)
		{
			if (getKeyName(messageSplit[i]).length() > 0)
			{
				keyNameBuffer.append(getKeyName(messageSplit[i])).append(delimeter);
				valueBuffer.append(getValue(messageSplit[i])).append(delimeter);
			}
			else
			{
				valueBuffer.append(messageSplit[i]).append(delimeter);
			}
		}

		String[] keyNames =
				org.springframework.util.StringUtils.delimitedListToStringArray(keyNameBuffer.toString(), delimeter);
		String[] keyvalues =
				org.springframework.util.StringUtils.delimitedListToStringArray(valueBuffer.toString(), delimeter);

		//if (keyNames.length == (keyvalues.length+1))

		for (int i = 1; i < keyNames.length; i++)
		{
			if (keyNames[i] == null || keyNames[i].trim().length() == 0)
				continue;
			try
			{
				keyvalues[i] = keyvalues[i].trim();
				keyvalues[i] = org.springframework.util.StringUtils.replace(keyvalues[i], "($%^)", "=");
				if (keyvalues[i].startsWith("[") && keyvalues[i].endsWith("]"))
					keyvalues[i] = keyvalues[i].substring(1, keyvalues[i].length() - 1);
				params.put(keyNames[i], keyvalues[i].trim());
			} catch (Exception e)
			{
				params.put(keyNames[i], "");
				log.debug("Value of last data is empty");
			}
		}
		return params;
	}

	private static String getKeyName(String partialMsg)
	{
		if (partialMsg.startsWith("[") && partialMsg.endsWith("]"))
			return "";
		String character = "";
		String value = "";
		for (int i = partialMsg.length(); i > 0; i--)
		{
			character = partialMsg.substring(i - 1, i);
			if (character.equalsIgnoreCase(" "))
			{
				return StringUtils.reverse(value);
			}
			else
			{
				value += character;
			}
		}
		return "";
	}

	private static String getValue(String partialMsg)
	{
		partialMsg = StringUtils.removeEnd(partialMsg, getKeyName(partialMsg));
		//		try {
		//			if (partialMsg.trim().length() == 0)
		//				return "";
		//		} catch (Exception e) {}
		return partialMsg;
	}

	private static void setItemValue(Document doc, String elementName, String nodeName, String nodeValue)
	{
		try
		{
			String nodePath = "//" + Root_Tag + "/" + elementName + "/" + nodeName;
			Element element = JdomUtils.getNode(doc, nodePath);
			if (element != null)
				JdomUtils.setNodeText(doc, nodePath, nodeValue);
			else
			{
				nodePath = "//" + Root_Tag + "/" + elementName + "/";
				if (nodePath.endsWith("/"))
					nodePath = nodePath.substring(0, nodePath.length() - 1);
				Element headerElement = JdomUtils.getNode(doc, nodePath);
				JdomUtils.addElement(headerElement, nodeName, nodeValue);
			}
		} catch (Exception e)
		{
			log.error(e, e);
		}
	}

}
