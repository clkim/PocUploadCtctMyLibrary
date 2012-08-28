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
 * Model Object encapsulating an individual schedule entry.
 * Has the following attributes:
 * 		Link (preloaded by CTCTConnection.getSchedule()) //TODO 
 * 		ScheduleId
 * 		AuthorName
 * 		ScheduledTime
 * 
 * @author CL Kim, 2012, based on work by Huan Lai
 *
 */
public class Schedule extends MutableModelObject {

	Schedule(HashMap<String, Object> attributes, CTCTConnection connection, boolean created) {
		super(attributes, connection, created);
	}

	@Override
	public String generateCreateXmlRequest() {
		String xml =
			"<entry xmlns=\"http://www.w3.org/2005/Atom\">" +
				"<id>data:,</id>" +
				"<title/>" +
				"<author/>" +
				"<updated>2010-11-13T20:03:35.000Z</updated>" +
				"<content type=\"application/vnd.ctct+xml\">" +
					"<Schedule xmlns=\"http://ws.constantcontact.com/ns/1.0/\" " +
							  "id=\""+CTCTConnection.API_BASE.replace("https","http") + attributes.get("Link") +"\" >" +
						"<ScheduledTime>"+attributes.get("ScheduledTime")+"</ScheduledTime>" +
					"</Schedule>" +
				"</content>" +
			"</entry>";
		return xml;
	}
	
	@Override
	public String generateUpdateXmlRequest() {
		StringBuilder builder = new StringBuilder();
		//TODO
		builder.append("<entry xmlns=\"http://www.w3.org/2005/Atom\">");

		builder.append("</entry>");
		
		return builder.toString();
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		
		// Skip the source section of the response
		if(qName.equals("source")) {
			inSource = false;
		}
		
		if(!inSource) {
			if(qName.equals("id")) {
				attributes.put("ScheduleId", currentString);
			} else if(qName.equals("name")) {
				attributes.put("AuthorName", currentString);
			} else if(qName.equals("ScheduledTime")) {
				attributes.put("ScheduledTime", currentString);
			}
		}
	}
}
