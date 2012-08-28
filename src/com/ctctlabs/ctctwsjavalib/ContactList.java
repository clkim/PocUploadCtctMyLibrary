package com.ctctlabs.ctctwsjavalib;

import java.util.HashMap;

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
 * Model Object encapsulating an individual contact list entry.
 * Has the following attributes:
 * 		ContactListId (preloaded by CTCTConnection.getContactLists())
 * 		Link (preloaded by CTCTConnection.getContactLists())
 * 		Name (preloaded by CTCTConnection.getContactLists())
 * 
 * @author Huan Lai
 *
 */
public class ContactList extends MutableModelObject {

	ContactList(HashMap<String, Object> attributes, CTCTConnection connection, boolean created) {
		super(attributes, connection, created);
	}

	@Override
	public String generateCreateXmlRequest() {
		String xml = "<entry xmlns=\"http://www.w3.org/2005/Atom\">" +
						"<id>data:,</id>" + 
						"<title/>" +
						"<author/>" +
						"<updated>2008-04-16</updated>" +
						"<content type=\"application/vnd.ctct+xml\">" +
							"<ContactList xmlns=\"http://ws.constantcontact.com/ns/1.0/\">" +
								"<OptInDefault>false</OptInDefault>" +
								"<Name>" + attributes.get("Name") + "</Name>" +
								"<SortOrder>99999</SortOrder>" +
							"</ContactList>" +
						"</content>" +
					"</entry>";
		return xml;
	}
	
	@Override
	public String generateUpdateXmlRequest() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("<entry xmlns=\"http://www.w3.org/2005/Atom\">");
		builder.append("<link href=\"");
		builder.append(attributes.get("Link"));
		builder.append("\" rel=\"edit\" />");
		builder.append("<id>");
		builder.append(attributes.get("ContactListId"));
		builder.append("</id>");
		builder.append("<title/>");
		builder.append("<author/>");
		builder.append("<updated>2008-04-16</updated>");
		builder.append("<content type=\"application/vnd.ctct+xml\">");
		builder.append("<ContactList xmlns=\"http://ws.constantcontact.com/ns/1.0/\">");
		builder.append("<OptInDefault>false</OptInDefault>");
		builder.append("<Name>");
		builder.append(attributes.get("Name"));
		builder.append("</Name>");
		builder.append("<SortOrder>99</SortOrder>");
		builder.append("</ContactList>");
		builder.append("</content>");
		builder.append("</entry>");
		
		return builder.toString();
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		
		// Skip the source section of the response
		if(qName.equals("source")) {
			inSource = false;
		}
		
		if(!inSource) {
			if(qName.equals("id")) {
				attributes.put("ContactListId", currentString);
			} else if(qName.equals("Name")) {
				attributes.put("Name", currentString);
			}
		}
	}
}
