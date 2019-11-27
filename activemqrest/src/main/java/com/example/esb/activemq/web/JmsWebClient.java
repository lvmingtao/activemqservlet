package com.example.esb.activemq.web;

import javax.servlet.http.HttpServletRequest;

import org.apache.activemq.web.AjaxWebClient;

/*
 * Collection of all data needed to fulfill requests from a single web client.
 */
public class JmsWebClient extends AjaxWebClient {

	public JmsWebClient(HttpServletRequest request, long maximumReadTimeout) {
		super(request, maximumReadTimeout);
	}
    
}
