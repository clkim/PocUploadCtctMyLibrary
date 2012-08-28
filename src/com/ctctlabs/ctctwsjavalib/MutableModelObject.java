package com.ctctlabs.ctctwsjavalib;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
 * Special type of ModelObject that allows for attributes to be modified.
 * When attributes have been modified, is marked as dirty.
 * 		Once dirty, can commit changes to the web service to update the associated entry.
 * 
 * August 2012
 * Update to do commit by posting a multipart request, for uploading an image to Library
 * 
 * @author Huan Lai, updated CL Kim August 2012
 *
 */
public abstract class MutableModelObject extends ModelObject {
	private boolean dirty;
	private boolean created;
	private boolean multipart;
	
	MutableModelObject(HashMap<String, Object> attributes, CTCTConnection connection, boolean created) {
		super(attributes, connection);
		this.created = created;
	}
	
	MutableModelObject(HashMap<String, Object> attributes, CTCTConnection connection, boolean created, boolean multipart) {
		super(attributes, connection);
		this.created = created;
		this.multipart = multipart;
	}
	
	/**
	 * Sets the value attribute of given name.
	 * Marks this object as dirty.
	 * @param attribute Name of attribute to save
	 * @param value Value of attribute to save
	 */
	public void setAttribute(String attribute, Object value) {
		dirty = true;
		attributes.put(attribute, value);
	}
	
	/**
	 * @return True if the object is dirty.
	 */
	public boolean isDirty() {
		return dirty;
	}
	
	/**
	 * Sends a request to the web service to delete this entry.
	 * Contacts aren't actually deleted, they are opted out. 
	 * 		This should be used if the contact has decided to unsubscribe from 
	 * 			receiving all emails or has asked to stop sending all emails.  
	 * 		Opted-out Contacts become members of the do-not-mail special list.
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public boolean delete() 
	throws ClientProtocolException, IOException {
		return connection.doDeleteRequest(CTCTConnection.API_BASE + (String)getAttribute("Link"));
	}
	
	/**
	 * Commits the changes to the web service entry.
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public void commit() 
	throws ClientProtocolException, IOException {
		
		InputStream response;
		// If an image is to be uploaded to the server, perform a multipart post
		if (multipart && created) {
			multipart = false;
			created = false;
			String request = generateCreateXmlRequest();
			byte[] imageData = (byte[])getAttribute("Image");
			String fileName = (String)getAttribute("FileName");
			response = connection.doPostMultipartRequest(CTCTConnection.API_BASE + (String)getAttribute("Link"), request, imageData, fileName);
		}  
		// If the entry is to be created on the server, perform a post
		else if(created) {
			created = false;
			String request = generateCreateXmlRequest();
			response = connection.doPostRequest(CTCTConnection.API_BASE + (String)getAttribute("Link"), request);
		// If the entry is to be updated, perform a put
		} else {
			String request = generateUpdateXmlRequest();
			response = connection.doPutRequest(CTCTConnection.API_BASE + (String)getAttribute("Link"), request);
		}
		if(response != null) {
			processResponse(response);
		}
	}

	/**
	 * Processes the response from the server after a commit()
	 * @throws IOException 
	 */
	public void processResponse(InputStream response)
	throws IOException {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.parse(new InputSource(response));
		} catch (ParserConfigurationException e) {
			System.out.println("ModelIterator ParserConfigurationException");
		} catch (SAXException e) {
			System.out.println("ModelIterator SAXException");
		}
	}
	
    /**
     * Generates the appropriate XML to be sent to the server as a request
     * using the object's attributes
     * @throws IOException 
     * @throws ClientProtocolException 
     */
	public abstract String generateCreateXmlRequest();
	public abstract String generateUpdateXmlRequest();
	
}
