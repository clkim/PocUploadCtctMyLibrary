package com.ctctlabs.ctctwsjavalib;

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
public class ContactListIterator extends ModelIterator {

	ContactListIterator(CTCTConnection connection, String nextPage) {
		super(connection, nextPage);
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		
		// Skip the source section of the response
		if(qName.equals("source")) {
			inEntry = true;
		}
		
		if(inEntry) {
			if(qName.equals("id")) {
				currentEntry.put("ContactListId", currentString);
			} else if(qName.equals("Name")) {
				currentEntry.put("Name", currentString);
			} else if(qName.equals("entry")) {
				inEntry = false;
				loadedObjects.add(new ContactList(currentEntry, connection, false));
			}
		}
	}
}
