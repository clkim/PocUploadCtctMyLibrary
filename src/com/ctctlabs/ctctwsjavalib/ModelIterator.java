package com.ctctlabs.ctctwsjavalib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
 * Encapsulates a collection of model objects. 
 * Handles the loading of the entries from the web service and instantiates the appropriate model objects.
 * 	For more complex model objects, only loads some data for that object; the rest is lazy loaded.
 * Takes the pagination of the web service into consideration, allowing for a way to load more pages.
 * 
 * @author Huan Lai
 *
 */
public abstract class ModelIterator extends DefaultHandler {
	protected ArrayList<ModelObject> loadedObjects;
	protected String nextPage;
	protected CTCTConnection connection;
	
	protected String currentString;
	protected HashMap<String, Object> currentEntry;
	protected boolean inEntry;
	protected boolean bufferingCharacters;
	
	ModelIterator(CTCTConnection connection, String nextPage) {
		this.connection = connection;
		this.nextPage = nextPage;
		inEntry = false;
		loadedObjects = new ArrayList<ModelObject>();
		bufferingCharacters = false;
	}
	
	/**
	 * @return True if there is exists a next page of entries to load
	 */
	public boolean hasNextPage() {
		return (nextPage != null);
	}
	
	/**
	 * Loads the next page of entries into loadedObjects
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public void loadNextPage() 
	throws ClientProtocolException, IOException {
		InputStream stream = connection.doGetRequest(nextPage);
		nextPage = null;
		
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
    
	public ArrayList<ModelObject> getLoadedEntries() {
		return loadedObjects;
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
			if(rel != null && rel.equals("next")) {
				this.nextPage = CTCTConnection.API_BASE + atts.getValue("href");
			} else if(rel != null && rel.equals("edit") && inEntry) {
				currentEntry.put("Link", atts.getValue("href"));
			}
		
		// Reached another entry	
		} else if(qName.equals("entry")) {
			currentEntry = new HashMap<String, Object>();
			inEntry = true;
			
		// Skip the source section of the response
		} else if(qName.equals("source")) {
			inEntry = false;
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
