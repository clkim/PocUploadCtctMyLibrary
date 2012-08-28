package com.ctctlabs.ctctwsjavalib;

import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.text.TextUtils;

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
 * Model object encapsulating an individual campaign entry.
 * Has the following attributes:
 * 		CampaignId (preloaded by CTCTConnection.getCampaigns())
 * 		Link (preloaded by CTCTConnection.getCampaigns())
 * 		Updated (preloaded by CTCTConnection.getCampaigns())
 * 		Name (preloaded by CTCTConnection.getCampaigns())
 * 		Date  (preloaded by CTCTConnection.getCampaigns())
 * 		LastEditDate
 * 		LastRunDate
 * 		NextRunDate
 * 		Status (preloaded by CTCTConnection.getCampaigns())
 * 		Sent
 * 		Opens
 * 		Clicks
 * 		Bounces
 * 		Forwards
 * 		OptOuts
 * 		Urls
 * 		EmailContent // clk added
 * 
 * [July 12]
 * Update to extend MutableModelObject instead of ModelObject
 * Can create a new campaign
 * 
 * @author Huan Lai, updated CL Kim July 2012
 *
 */
public class Campaign extends MutableModelObject {
	private boolean inUrls;
		
	Campaign(HashMap<String, Object> attributes, CTCTConnection connection) {
		super(attributes, connection, false);
		inUrls = false;
	}
	
	Campaign(HashMap<String, Object> attributes, CTCTConnection connection, boolean created) {
		super(attributes, connection, created);
		inUrls = false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		super.startElement(namespaceURI, localName, qName, atts);
		
		if(qName.equals("Urls")) {
			inUrls = true;
		} else if(inUrls && qName.equals("Url")) {
			if(!attributes.containsKey("Urls")) {
				attributes.put("Urls", new ArrayList<String>());
			}
			
			ArrayList<String> urls = (ArrayList<String>) attributes.get("Urls");
			urls.add(atts.getValue("id"));
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		super.endElement(namespaceURI, localName, qName);
		
		if(qName.equals("source")) {
			inSource = false;
		}
		
		if(qName.equals("Urls")) {
			inUrls = false;
		}
		
		// Skip the source section of the response
		if(!inSource) {
			if(qName.equals("id")) {
				attributes.put("CampaignId", currentString);
			} else if(qName.equals("updated")) {
				attributes.put("Updated", currentString);
			} else if(qName.equals("Name")) {
				attributes.put("Name", currentString);
			} else if(qName.equals("Date")) {
				attributes.put("Date", currentString);
			} else if(qName.equals("Status")) {
				attributes.put("Status", currentString);
			} else if(qName.equals("LastEditDate")) {
				attributes.put("LastEditDate", currentString);
			} else if(qName.equals("LastRunDate")) {
				attributes.put("LastRunDate", currentString);
			} else if(qName.equals("NextRunDate")) {
				attributes.put("NextRunDate", currentString);
			} else if(qName.equals("Sent")) {
				attributes.put("Sent", currentString);
			} else if(qName.equals("Opens")) {
				attributes.put("Opens", currentString);
			} else if(qName.equals("Clicks")) {
				attributes.put("Clicks", currentString);
			} else if(qName.equals("Bounces")) {
				attributes.put("Bounces", currentString);
			} else if(qName.equals("Forwards")) {
				attributes.put("Forwards", currentString);
			} else if(qName.equals("OptOuts")) {
				attributes.put("OptOuts", currentString);
			} else if(qName.equals("EmailContent")) {			// clk added
				attributes.put("EmailContent", currentString);
			}
		}
	}

	@Override
	public String generateCreateXmlRequest() {
		String xml = 
		"<entry xmlns=\"http://www.w3.org/2005/Atom\">" +
		  "<id>data:,</id>" +
		  "<title />" +
		  "<updated>2011-04-25T13:53:04.243Z</updated>" +
		  "<content type=\"application/vnd.ctct+xml\">" +
		    "<Campaign xmlns=\"http://ws.constantcontact.com/ns/1.0/\">" +
		      "<Name>"+attributes.get("Name")+"</Name>" +
		      "<Subject>"+attributes.get("Subject")+"</Subject>" +
		      "<FromName>"+attributes.get("FromName")+"</FromName>" +
		      "<ViewAsWebpage>YES</ViewAsWebpage>" +
		      "<ViewAsWebpageLinkText>Click here</ViewAsWebpageLinkText>" +
		      "<ViewAsWebpageText>Having trouble viewing this email?</ViewAsWebpageText>" +
		      "<PermissionReminder>NO</PermissionReminder>" +
		      "<OrganizationName />" +
		      "<OrganizationAddress1 />" +
		      "<OrganizationAddress2 />" +
		      "<OrganizationAddress3 />" +
		      "<OrganizationCity />" +
		      "<OrganizationState />" +
		      "<OrganizationInternationalState />" +
		      "<OrganizationCountry />" +
		      "<OrganizationPostalCode />" +
		      "<IncludeForwardEmail>NO</IncludeForwardEmail>" +
		      "<IncludeSubscribeLink>NO</IncludeSubscribeLink>" +
		      "<EmailContentFormat>HTML</EmailContentFormat>" +
		      "<EmailContent>" +
		        TextUtils.htmlEncode( (String) attributes.get("EmailContent") ) +
		      "</EmailContent>" + 
		      "<EmailTextContent>"+attributes.get("EmailTextContent")+"</EmailTextContent>" +
		      "<ContactLists>" +
		        "<ContactList id=\"" + CTCTConnection.API_BASE.replace("https","http") + attributes.get("ContactListPath") +"\" >" + "</ContactList>" +
		      "</ContactLists>" +
		      "<FromEmail>" +
		        "<Email id=\"" + CTCTConnection.API_BASE.replace("https","http") + attributes.get("SenderEmailAddressPath") +"\" >" + "</Email>" +
		        "<EmailAddress>"+attributes.get("SenderEmailAddress")+"</EmailAddress>" +
		      "</FromEmail>" +
		      "<ReplyToEmail>" +
		        "<Email id=\"" + CTCTConnection.API_BASE.replace("https","http") + attributes.get("SenderEmailAddressPath") +"\" >" + "</Email>" +
		        "<EmailAddress>"+attributes.get("SenderEmailAddress")+"</EmailAddress>" +
		      "</ReplyToEmail>" +
		    "</Campaign>" +
		  "</content>" +
		  "<source>" +
		    "<author />" +
		  "</source>" +
		"</entry>";

		return xml;
	}

	@Override
	public String generateUpdateXmlRequest() {
		// TODO Auto-generated method stub
		return null;
	}
}
