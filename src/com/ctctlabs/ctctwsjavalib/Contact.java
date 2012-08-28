package com.ctctlabs.ctctwsjavalib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
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
 * Model Object encapsulating an individual contact entry.
 * Has the following attributes:
 * 		ContactId (preloaded by CTCTConnection.getContacts()) (preloaded by CTCTConnection.getContactListMembers())
 * 		Link (preloaded by CTCTConnection.getContacts()) (preloaded by CTCTConnection.getContactListMembers())
 * 		Updated (preloaded by CTCTConnection.getContacts())
 * 		Status (preloaded by CTCTConnection.getContacts())
 * 		EmailAddress (preloaded by CTCTConnection.getContacts()) (preloaded by CTCTConnection.getContactListMembers())
 * 		EmailType (preloaded by CTCTConnection.getContacts())
 * 		Name (preloaded by CTCTConnection.getContacts()) (preloaded by CTCTConnection.getContactListMembers())
 * 		OptInTime (preloaded by CTCTConnection.getContacts())
 * 		OptInSource (preloaded by CTCTConnection.getContacts())
 * 		FirstName
 * 		MiddleName
 * 		LastName
 * 		JobTitle
 * 		CompanyName
 * 		HomePhone
 * 		WorkPhone
 * 		Addr1
 * 		Addr2
 * 		Addr3
 * 		City
 * 		StateCode
 * 		StateName
 * 		CountryCode
 * 		PostalCode
 * 		SubPostalCode
 * 		Note
 * 		ContactLists (ArrayList<ContactList>)
 * 		Confirmed
 * 		LastUpdateTime
 * 
 * @author Huan Lai
 *
 */
public class Contact extends MutableModelObject {
	private boolean inContactLists;
	private HashMap<String, Object> currentContactList;
	
	Contact(HashMap<String, Object> attributes, CTCTConnection connection, boolean created) {
		super(attributes, connection, created);
		inContactLists = false;
	}
	
	@Override
	public String generateCreateXmlRequest() {
		StringBuilder builder = new StringBuilder();
		builder.append("<entry xmlns=\"http://www.w3.org/2005/Atom\">");
		builder.append("<title type=\"text\"> </title>");
		builder.append("<updated>2008-07-23T14:21:06.407Z</updated>");
		builder.append("<author></author>");
		builder.append("<id>data:,none</id>");
		builder.append("<summary type=\"text\">Contact</summary>");
		builder.append("<content type=\"application/vnd.ctct+xml\">");
		builder.append("<Contact xmlns=\"http://ws.constantcontact.com/ns/1.0/\">");
		builder.append("<OptInSource>ACTION_BY_CUSTOMER</OptInSource>");

		builder.append(generateAttributesXmlRequest());


		builder.append("</Contact>");
		builder.append("</content>");
		builder.append("</entry>");
		
		return builder.toString();
	}

	@Override
	public String generateUpdateXmlRequest() {
		StringBuilder builder = new StringBuilder();
		builder.append("<entry xmlns=\"http://www.w3.org/2005/Atom\">");
		builder.append("<link href=\"");
		builder.append(attributes.get("Link"));
		builder.append("\" rel=\"edit\" />");
		builder.append("<title type=\"text\"> </title>");
		builder.append("<updated>2008-07-23T14:21:06.407Z</updated>");
		builder.append("<author></author>");
		builder.append("<id>");
		builder.append(attributes.get("ContactId"));
		builder.append("</id>");
		builder.append("<summary type=\"text\">Contact</summary>");
		builder.append("<content type=\"application/vnd.ctct+xml\">");
		builder.append("<Contact xmlns=\"http://ws.constantcontact.com/ns/1.0/\">");
		builder.append("<OptInSource>ACTION_BY_CUSTOMER</OptInSource>");
		
		builder.append(generateAttributesXmlRequest());

		builder.append("</Contact>");
		builder.append("</content>");
		builder.append("</entry>");
		
		return builder.toString();
	}
	
	@SuppressWarnings("unchecked")
	public String generateAttributesXmlRequest() {
		StringBuilder builder = new StringBuilder();
		
		if(hasAttribute("EmailAddress")) {
			builder.append("<EmailAddress>");
			builder.append(attributes.get("EmailAddress"));
			builder.append("</EmailAddress>");
		}
		if(hasAttribute("EmailType")) {
			builder.append("<EmailType>");
			builder.append(attributes.get("EmailType"));
			builder.append("</EmailType>");
		}
		if(hasAttribute("FirstName")) {
			builder.append("<FirstName>");
			builder.append(attributes.get("FirstName"));
			builder.append("</FirstName>");
		}
		if(hasAttribute("MiddleName")) {
			builder.append("<MiddleName>");
			builder.append(attributes.get("MiddleName"));
			builder.append("</MiddleName>");
		}
		if(hasAttribute("LastName")) {
			builder.append("<LastName>");
			builder.append(attributes.get("LastName"));
			builder.append("</LastName>");
		}
		if(hasAttribute("JobTitle")) {
			builder.append("<JobTitle>");
			builder.append(attributes.get("JobTitle"));
			builder.append("</JobTitle>");
		}
		if(hasAttribute("CompanyName")) {
			builder.append("<CompanyName>");
			builder.append(attributes.get("CompanyName"));
			builder.append("</CompanyName>");
		}
		if(hasAttribute("HomePhone")) {
			builder.append("<HomePhone>");
			builder.append(attributes.get("HomePhone"));
			builder.append("</HomePhone>");
		}
		if(hasAttribute("WorkPhone")) {
			builder.append("<WorkPhone>");
			builder.append(attributes.get("WorkPhone"));
			builder.append("</WorkPhone>");
		}
		if(hasAttribute("Addr1")) {
			builder.append("<Addr1>");
			builder.append(attributes.get("Addr1"));
			builder.append("</Addr1>");
		}
		if(hasAttribute("Addr2")) {
			builder.append("<Addr2>");
			builder.append(attributes.get("Addr2"));
			builder.append("</Addr2>");
		}
		if(hasAttribute("Addr3")) {
			builder.append("<Addr3>");
			builder.append(attributes.get("Addr3"));
			builder.append("</Addr3>");
		}
		if(hasAttribute("City")) {
			builder.append("<City>");
			builder.append(attributes.get("City"));
			builder.append("</City>");
		}
		if(hasAttribute("StateName")) {
			builder.append("<StateName>");
			builder.append(attributes.get("StateName"));
			builder.append("</StateName>");
		}
		if(hasAttribute("CountryName")) {
			builder.append("<CountryName>");
			builder.append(attributes.get("CountryName"));
			builder.append("</CountryName>");
		}
		if(hasAttribute("PostalCode")) {
			builder.append("<PostalCode>");
			builder.append(attributes.get("PostalCode"));
			builder.append("</PostalCode>");
		}
		if(hasAttribute("SubPostalCode")) {
			builder.append("<SubPostalCode>");
			builder.append(attributes.get("SubPostalCode"));
			builder.append("</SubPostalCode>");
		}
		if(hasAttribute("Note")) {
			builder.append("<Note>");
			builder.append(attributes.get("Note"));
			builder.append("</Note>");
		}
		
		builder.append("<ContactLists>");
		if(hasAttribute("ContactLists")) {
			ArrayList<ContactList> contactLists = (ArrayList<ContactList>) attributes.get("ContactLists");
			for(ContactList contactList : contactLists) {
				try {
					String contactListId = (String) contactList.getAttribute("ContactListId");
					builder.append("<ContactList id=\"");
					builder.append(contactListId);
					builder.append("\" />");
					
				// If anything bad happens, just skip it
				} catch (ClientProtocolException e) {
				} catch (IOException e) {}
			}
		}
		builder.append("</ContactLists>");
		
		return builder.toString();
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		super.startElement(namespaceURI, localName, qName, atts);
		
		if(qName.equals("ContactLists")) {
			inContactLists = true;
		} else if(qName.equals("ContactList")) {
			currentContactList = new HashMap<String, Object>();
			currentContactList.put("ContactListId", atts.getValue("id"));
		} else if(inContactLists && qName.equals("link")) {
			currentContactList.put("Link", atts.getValue("href").replaceFirst("http://api.constantcontact.com", ""));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		
		// Skip the source section of the response
		if(qName.equals("source")) {
			inSource = false;
		}
		
		if(inContactLists) {
			if(qName.equals("ContactLists")) {
				inContactLists = false;
			} else if(qName.equals("ContactList")) {
				if(attributes.get("ContactLists") == null) {
					attributes.put("ContactLists", new ArrayList<ContactList>());
				}
				
				ArrayList<ContactList> contactLists = null;
				try {
					contactLists = (ArrayList<ContactList>)attributes.get("ContactLists");
				} catch(ClassCastException e) {
					attributes.put("ContactLists", new ArrayList<ContactList>());
				} finally {
					ContactList contactList = new ContactList(currentContactList, connection, false);
					contactLists.add(contactList);
				}
			} else if(qName.equals("OptInSource")) {
				currentContactList.put("OptInSource", currentString);
			} else if(qName.equals("OptInTime")) {
				currentContactList.put("OptInTime", currentString);
			}
		} else if(!inSource) {
			if(qName.equals("id")) {
				attributes.put("ContactId", currentString);
			} else if(qName.equals("updated")) {
				attributes.put("Updated", currentString);
			} else if(qName.equals("Status")) {
				attributes.put("Status", currentString);
			} else if(qName.equals("EmailAddress")) {
				attributes.put("EmailAddress", currentString);
			} else if(qName.equals("EmailType")) {
				attributes.put("EmailType", currentString);
			} else if(qName.equals("Name")) {
				attributes.put("Name", currentString);
			} else if(qName.equals("OptInTime")) {
				attributes.put("OptInTime", currentString);
			} else if(qName.equals("OptInSource")) {
				attributes.put("OptInSource", currentString);
			} else if(qName.equals("FirstName")) {
				attributes.put("FirstName", currentString);
			} else if(qName.equals("MiddleName")) {
				attributes.put("MiddleName", currentString);
			} else if(qName.equals("LastName")) {
				attributes.put("LastName", currentString);
			} else if(qName.equals("JobTitle")) {
				attributes.put("JobTitle", currentString);
			} else if(qName.equals("CompanyName")) {
				attributes.put("CompanyName", currentString);
			} else if(qName.equals("HomePhone")) {
				attributes.put("HomePhone", currentString);
			} else if(qName.equals("WorkPhone")) {
				attributes.put("WorkPhone", currentString);
			} else if(qName.equals("Addr1")) {
				attributes.put("Addr1", currentString);
			} else if(qName.equals("Addr2")) {
				attributes.put("Addr2", currentString);
			} else if(qName.equals("Addr3")) {
				attributes.put("Addr3", currentString);
			} else if(qName.equals("City")) {
				attributes.put("City", currentString);
			} else if(qName.equals("StateCode")) {
				attributes.put("StateCode", currentString);
			} else if(qName.equals("StateName")) {
				attributes.put("StateName", currentString);
			} else if(qName.equals("CountryCode")) {
				attributes.put("CountryCode", currentString);
			} else if(qName.equals("PostalCode")) {
				attributes.put("PostalCode", currentString);
			} else if(qName.equals("SubPostalCode")) {
				attributes.put("SubPostalCode", currentString);
			} else if(qName.equals("Note")) {
				attributes.put("Note", currentString);
			} else if(qName.equals("Confirmed")) {
				attributes.put("Confirmed", currentString);
			} else if(qName.equals("LastUpdateTime")) {
				attributes.put("LastUpdateTime", currentString);
			} else if(qName.equals("OptInSource")) {
				attributes.put("OptInSource", currentString);
			}
		}
	}
}
