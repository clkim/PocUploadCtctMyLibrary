package com.ctctlabs.ctctwsjavalib;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/*
 * Copyright 1996-2009 Constant Contact, Inc.
 *   Licensed under the Apache License, Version 2.0 (the "License"); 
 *   you may not use this file except in compliance with the License. 
 *   You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *      
 *   Unless required by applicable law or agreed to in writing, software 
 *   distributed under the License is distributed on an "AS IS" BASIS, 
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   See the License for the specific language governing permissions and 
 *   limitations under the License.
 */

/**
 * Model Object encapsulating an individual campaign event entry.
 * Has the following attributes:
 * 		EmailAddress (preloaded by CTCTConnection.getCampaignEvents())
 * 		ContactId (preloaded by CTCTConnection.getCampaignEvents())
 * 		CampaignId (preloaded by CTCTConnection.getCampaignEvents())
 * 		EventTime (preloaded by CTCTConnection.getCampaignEvents())
 * 
 * @author Huan Lai
 *
 */
public class CampaignEvent extends ModelObject {

	CampaignEvent(HashMap<String, Object> attributes, CTCTConnection connection) {
		super(attributes, connection);
	}
	
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		super.startElement(namespaceURI, localName, qName, atts);
		
		if(!inSource) {
			if(qName.equals("Contact")) {
				String id = atts.getValue("id");
				
				// get rid of the http://api.constantcontact.com
				attributes.put("ContactLink", id.substring("http://api.constantcontact.com".length()));
			} else if (qName.equals("Campaign")) {
				String id = atts.getValue("id");
				
				// get rid of the http://api.constantcontact.com
				attributes.put("CampaignLink", id.substring("http://api.constantcontact.com".length()));
			}
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		if(qName.equals("EmailAddress")) {
			attributes.put("EmailAddress", currentString);
		} else if(qName.equals("EventTime")) {
			attributes.put("EventTime", currentString);
		}
	}
}
