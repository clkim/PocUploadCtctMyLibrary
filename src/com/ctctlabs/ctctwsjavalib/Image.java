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
 * Model Object encapsulating an individual image entry.
 * Has the following attributes:
 * 		Link (preloaded by CTCTConnection.getImage()) //TODO 
 * 		FileName
 * 		ImageURL
 * 		Description
 * 
 * @author CL Kim, 2012, based on work by Huan Lai
 *
 */
public class Image extends MutableModelObject {
	
	Image(HashMap<String, Object> attributes, CTCTConnection connection, boolean created) {
		super(attributes, connection, created, true);
	}

	@Override
	public String generateCreateXmlRequest() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:content>" +
					"<Image>" +
						"<FileName>" + attributes.get("FileName") + "</FileName>" +
						"<MD5Hash>" + attributes.get("MD5Hash") + "</MD5Hash>" +
						"<Description>" + attributes.get("Description") + "</Description>" +
					"</Image>" +
				"</atom:content>" +
			"</atom:entry>";
		return xml;
	}
	
	@Override
	public String generateUpdateXmlRequest() {
		// Unsupported - To change the image name or image itself, you need to delete an existing image and upload again
		StringBuilder builder = new StringBuilder();
		builder.append("<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">");
		builder.append("</atom:entry>");
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
			if(qName.equals("FileName")) {
				attributes.put("FileName", currentString);
			} else if(qName.equals("ImageURL")) {
				attributes.put("ImageURL", currentString);
			} else if(qName.equals("Description")) {
				attributes.put("Description", currentString);
			}
		}
	}
}
