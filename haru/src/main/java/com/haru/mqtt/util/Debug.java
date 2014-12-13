/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package com.haru.mqtt.util;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility to help debug problems with the Paho MQTT client
 * Once initialised a call to dumpClientDebug will force any memory trace
 * together with pertinent client and system state to the main log facility.
 * 
 * No client wide lock is taken when the dump is progress. This means the 
 * set of client state may not be consistent as the client can still be 
 * processing work while the dump is in progress.
 */
public class Debug {
	
	private static final String separator = "==============";
	private static final String lineSep = System.getProperty("line.separator","\n");

	/**
	 * Return a set of properties as a formatted string
	 */
	public static String dumpProperties(Properties props, String name) {
		
		StringBuffer propStr = new StringBuffer();
	    Enumeration propsE = props.propertyNames();
    	propStr.append(lineSep+separator+" "+name+" "+ separator+lineSep);
	    while (propsE.hasMoreElements()) {
	    	String key = (String)propsE.nextElement();
	    	propStr.append(left(key,28,' ') + ":  "+ props.get(key)+lineSep);
	    }
    	propStr.append(separator+separator+separator+lineSep);

    	return propStr.toString();
	}
	
	/**
	   * Left justify a string.
	   *
	   * @param s the string to justify
	   * @param width the field width to justify within
	   * @param fillChar the character to fill with
	   *
	   * @return the justified string.
	   */
	  public static String left(String s, int width, char fillChar) {
	    if (s.length() >= width) {
	      return s;
	    }
	    StringBuffer sb = new StringBuffer(width);
	    sb.append(s);
	    for (int i = width - s.length(); --i >= 0;) {
	      sb.append(fillChar);
	    }
	    return sb.toString();
	  }
	
}
