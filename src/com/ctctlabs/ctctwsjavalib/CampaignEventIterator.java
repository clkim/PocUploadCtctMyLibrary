package com.ctctlabs.ctctwsjavalib;

import java.util.HashMap;

import org.apache.http.message.BufferedHeader;
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
public class CampaignEventIterator extends ModelIterator {

	CampaignEventIterator(CTCTConnection connection, String nextPage) {
		super(connection, nextPage);
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		// Parse the links
		if(qName.equals("link")) {
			String rel = atts.getValue("rel");
			if(rel != null && rel.equals("next")) {
				this.nextPage = atts.getValue("href");
			}
		
		// Reached another entry	
		} else if(qName.equals("entry")) {
			currentEntry = new HashMap<String, Object>();
			inEntry = true;
		
		// Parse other attributes within the entry
		} else if(inEntry) {
			if(qName.equals("Contact")) {
				String id = atts.getValue("id");
				
				// get rid of the http://api.constantcontact.com
				currentEntry.put("ContactLink", id.substring("http://api.constantcontact.com".length()));
			} else if (qName.equals("Campaign")) {
				String id = atts.getValue("id");
				
				// get rid of the http://api.constantcontact.com
				currentEntry.put("CampaignLink", id.substring("http://api.constantcontact.com".length()));
			}
		}
		
		bufferingCharacters = true;
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		
		if(qName.equals("EmailAddress")) {
			currentEntry.put("EmailAddress", currentString);
		} else if(qName.equals("EventTime")) {
			currentEntry.put("EventTime", currentString);
		} else if(qName.equals("entry")) {
			inEntry = false;
			loadedObjects.add(new CampaignEvent(currentEntry, connection));
		}
	}
}
