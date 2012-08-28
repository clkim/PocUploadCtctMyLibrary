package com.ctctlabs.ctctwsjavalib;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

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
 * Model object encapsulating individual entries from the web service.
 * Contains a list of all the attributes associated with that entry.
 * If instantiated by a ModelIterator, might only partially preload.
 * 		The rest is automatically lazy loaded if queried for.
 * After instantiation, attributes are immutable.
 * 
 * @author Huan Lai
 *
 */
public abstract class ModelObject extends DefaultHandler{
	protected HashMap<String, Object> attributes;
	protected CTCTConnection connection;
	protected String currentString;
	protected boolean inSource;
	protected boolean bufferingCharacters;
	
	ModelObject(HashMap<String, Object> attributes, CTCTConnection connection) {
		this.attributes = attributes;
		this.connection = connection;
		inSource = false;
		bufferingCharacters = false;
	}
	
	/**
	 * If the attribute queried for is not already loaded, automatically load it from server.
	 * @param attribute Name of attribute to retrieve
	 * @return Value of the attribute with the given name
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public Object getAttribute(String attribute) 
	throws ClientProtocolException, IOException {
		if(!hasAttribute(attribute)) {
			InputStream stream = connection.doGetRequest(CTCTConnection.API_BASE + (String) attributes.get("Link"));
			
			if(stream == null) {
				return null;
			}
			
			try {
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				xr.setContentHandler(this);
				xr.parse(new InputSource(stream));
			} catch (ParserConfigurationException e) {
				System.out.println("ModelIterator ParserConfigurationException");
			} catch (SAXException e) {
				System.out.println("ModelIterator SAXException");
			}
		}
		
		return attributes.get(attribute);
	}
	
	/**
	 * @param attribute Name of attribute to retrieve
	 * @return True if an attribute with given name has already been loaded from server
	 */
	public boolean hasAttribute(String attribute) {
		return attributes.containsKey(attribute);
	}
	
    /** Gets be called on the following structure:
     * <tag>characters</tag> */
    @Override
   public void characters(char ch[], int start, int length) {
    	if(bufferingCharacters && currentString != null) {
    		currentString += new String(ch, start, length).trim();
    	} else {
    		currentString = new String(ch, start, length).trim();
    	}
    }
    
	/** Gets be called on opening tags like:
     * <tag>
     * Can provide attribute(s), when xml was like:
     * <tag attribute="attributeValue">*/
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		// Parse the links
		if(qName.equals("link")) {
			String rel = atts.getValue("rel");
			if(rel != null && rel.equals("edit")) {
				attributes.put("Link", atts.getValue("href"));
			}
		
		// Skip the source section of the response
		} else if(qName.equals("source")) {
			inSource = true;
		}
		
		bufferingCharacters = true;
	}
	
	/** Gets be called on closing tags like:
     * </tag> */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    throws SAXException {
    	bufferingCharacters = false;
    }
}
