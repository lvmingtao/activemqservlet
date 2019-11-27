package com.example.util.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Attribute;
import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;

/**
 ****************************************************************************
 *  PACKAGE : com.cim.idm.framework.util.xml<br>
 *  NAME    : JdomUtils.java<br>
 *  TYPE    : JAVA<br>
 *  DESCRIPTION : jdom-related utility<br>
 *
 ****************************************************************************
 */

public class JdomUtils
{
	private static Log	log	= LogFactory.getLog(JdomUtils.class.getName());

	/**
	 * load Document File
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public static Document loadFile(String filename) throws Exception
	{
		try
		{
			File file = new File(filename);
			if (!file.exists())
			{
				InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
				return new SAXBuilder().build(inputStream);
			}
			return new SAXBuilder().build(new File(filename));
		} catch (Exception e)
		{
			if (log.isDebugEnabled())
				log.error(e, e);
			else
				log.error(e);

		}
		return null;
	}

	public static Document loadText(String text) throws Exception
	{
		return new SAXBuilder().build(new StringReader(text));
	}

	public static XMLOutputter indentOutputter()
	{
		return indentOutputter(false);
	}

	public static XMLOutputter indentOutputter(boolean persistWhitespace)
	{
		XMLOutputter out = new XMLOutputter();
		Format format = getFormat(persistWhitespace);
		if (persistWhitespace)
			format.setIndent("    ");
		out.setFormat(format);
		return out;
	}

	public static XMLOutputter indentlessOutputter(boolean persistWhitespace)
	{
		XMLOutputter out = new XMLOutputter();
		Format format = getFormat(persistWhitespace);
		out.setFormat(format);
		return out;
	}

	private static Format getFormat(boolean persistWhitespace)
	{
		//		return persistWhitespace ? Format.getRawFormat() : Format.getCompactFormat();
		return persistWhitespace ? Format.getPrettyFormat() : Format.getCompactFormat();
	}

	/**
	 * save File
	 * 
	 * @param any
	 * @param file
	 * @throws Exception
	 */
	public static void saveFile(Object any, String file) throws Exception
	{
		XMLOutputter out = indentOutputter();
		Writer writer = new FileWriter(new File(file));

		if (any instanceof Document)
			out.output((Document) any, writer);
		else if (any instanceof Element)
			out.output((Element) any, writer);
		else if (any instanceof CDATA)
			out.output((CDATA) any, writer);
		else if (any instanceof Comment)
			out.output((Comment) any, writer);
		else if (any instanceof List)
			out.output((List) any, writer);
		else
			throw new IllegalArgumentException("Unknown type");

		writer.close();
	}

	public static String toString(Object any)
	{
		return toString(any, true);
	}

	public static String toString(Object any, boolean persistWhitespace)
	{
		XMLOutputter out = indentOutputter(persistWhitespace);
		return toString(any, out);
	}

	public static String toStringIndentless(Object any)
	{
		return toString(any, false);
	}

	/**
	 * xml to string
	 * 
	 * @param any
	 * @param out
	 * @return
	 */
	private static String toString(Object any, XMLOutputter out)
	{
		if (any instanceof Document)
			return out.outputString((Document) any);
		else if (any instanceof Element)
			return out.outputString((Element) any);
		else if (any instanceof CDATA)
			return out.outputString((CDATA) any);
		else if (any instanceof Comment)
			return out.outputString((Comment) any);
		else if (any instanceof List)
			return out.outputString((List) any);
		else
			throw new IllegalArgumentException("Unknown type");
	}

	public static List getList(Object any, String xpath) throws Exception
	{
		XPath xp = XPath.newInstance(xpath);
		return xp.selectNodes(any);
	}

	public static Element getNode(Object any, String xpath) throws Exception
	{
		return (Element) XPath.selectSingleNode(any, xpath);
	}

	public static Element getNode(Object any, String xpath, int index) throws Exception
	{
		String locationPath = xpath + "[" + String.valueOf(index) + "]";

		return (Element) XPath.selectSingleNode(any, locationPath);
	}

	public static String getNodeText(Object any, String xpath) throws Exception
	{
		Element element = (Element) XPath.selectSingleNode(any, xpath);
		return (element != null) ? element.getText() : "";
	}

	public static String getNodeText(Object any, String xpath, int index) throws Exception
	{
		Element element = getNode(any, xpath, index);
		return (element != null) ? element.getText() : "";
	}

	/**
	 * Received node Text, Add it to the list
	 * 
	 * @param any
	 * @param xpath
	 * @return
	 * @throws Exception
	 */
	public static List<String> getNodeTextList(Object any, String xpath) throws Exception
	{
		List list = getList(any, xpath);

		List<String> values = new ArrayList<String>();
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			String value = ((Element) iter.next()).getTextTrim();
			if (value.length() > 0)
				values.add(value);
		}

		return values;
	}

	/**
	 * Bring the list of child element.
	 * 
	 * @param element
	 * @param index
	 * @return
	 */
	public static String getChildValue(Element element, int index)
	{
		List list = element.getChildren();
		return ((Element) list.get(index)).getText();
	}

	/**
	 * Bring the value of the Attribute Value
	 * 
	 * @param any
	 * @param xpath
	 * @return
	 * @throws Exception
	 */
	public static String getAttributeValue(Object any, String xpath) throws Exception
	{
		Attribute obj = (Attribute) XPath.selectSingleNode(any, xpath);
		return obj.getValue();
	}

	/**
	 * Bring the value of the Corresponding Attribute Value from element
	 * 
	 * @param any
	 * @param xpath
	 * @param attributeName
	 * @return
	 * @throws Exception
	 */
	public static String getAttributeValue(Object any, String xpath, String attributeName) throws Exception
	{
		Element element = getNode(any, xpath);
		return element.getAttributeValue(attributeName);
	}

	public static Document createDocument(String root)
	{
		return new Document(new Element(root));
	}

	public static Document createDocument(Element root)
	{
		return new Document(root);
	}

	public static Element createElement(String name, String prefix, String namespace)
	{
		return new Element(name, Namespace.getNamespace(prefix, namespace));
	}

	public static Element createElement(String name, Namespace namespace)
	{
		return new Element(name, namespace);
	}

	public static Element createElement(String name)
	{
		return new Element(name);
	}

	/**
	 * create element
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public static Element createElement(String name, String value)
	{
		Element element = new Element(name);
		element.setText(value);
		return element;
	}

	/**
	 * Add created element to the parent element
	 * 
	 * @param parent
	 * @param nodeName
	 * @param nodeValue
	 * @return
	 */
	public static Element addElement(Element parent, String nodeName, String nodeValue)
	{
		Element newNode = createElement(nodeName, nodeValue);
		parent.addContent(newNode);
		return newNode;
	}

	/**
	 * Add received node to the element
	 * 
	 * @param doc
	 * @param parentXPath
	 * @param nodeName
	 * @param nodeValue
	 * @return
	 */
	public static Element addElement(Object doc, String parentXPath, String nodeName, String nodeValue)
	{
		try
		{
			Element parent = getNode(doc, parentXPath);

			return addElement(parent, nodeName, nodeValue);
		} catch (Exception ex)
		{
			return null;
		}
	}

	public static Element appendChildElement(Element to, Element from)
	{
		return to.addContent(from);
	}

	public static Attribute createAttr(String name, String value)
	{
		return new Attribute(name, value);
	}

	public static Attribute createAttr(String name, String value, Namespace namespace)
	{
		return new Attribute(name, value, namespace);
	}

	public static Element appendAttr(Element to, String name, String value, String namespace)
	{
		return to.setAttribute(name, value, Namespace.getNamespace(namespace));
	}

	public static Element appendAttr(Element to, String name, String value, Namespace namespace)
	{
		return to.setAttribute(name, value, namespace);
	}

	public static Element appendAttr(Element to, String name, String value)
	{
		return to.setAttribute(name, value);
	}

	public static Element appendAttr(Element to, Attribute attr)
	{
		return to.setAttribute(attr);
	}

	/**
	 * Set to the node Text on element
	 * 
	 * @param any
	 * @param xpath
	 * @param text
	 * @throws Exception
	 */
	public static void setNodeText(Object any, String xpath, String text) throws Exception
	{
		Element node = getNode(any, xpath);
		if (node == null)
			return;
		node.setText(text);
	}

	/**
	 * Set to the node Text on element.
	 * 
	 * @param any
	 * @param xpath
	 * @param text
	 * @param index
	 * @throws Exception
	 */
	public static void setNodeText(Object any, String xpath, String text, int index) throws Exception
	{
		Element node = getNode(any, xpath, index);
		if (node == null)
			return;
		node.setText(text);
	}

	/**
	 * Set to the node Text on element.
	 * 
	 * @param any
	 * @param parentXpath
	 * @param nodeName
	 * @param nodeValue
	 * @throws Exception
	 */
	public static void setNodeText(Object any, String parentXpath, String nodeName, String nodeValue) throws Exception
	{
		Element parent = getNode(any, parentXpath);
		if (parent != null)
		{
			Element element = parent.getChild(nodeName);
			if (element == null)
			{
				element = createElement(nodeName, nodeValue);
				parent.addContent(element);
			}
			else
			{
				element.setText(nodeValue);
			}
		}
	}

	//	Element root = new Element("root", Namespace.getNamespace("aa"));
	//	
	//	Namespace ns = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	//	
	//	root.addNamespaceDeclaration(ns);
	//	
	//	root.setAttribute(new Attribute("schemaLocation", "http://jbpm.org/3/jpdl http://jbpm.org/xsd/jpdl-3.0.xsd", ns));
	//
	//	System.out.println(JdomUtils.toString(root));

}