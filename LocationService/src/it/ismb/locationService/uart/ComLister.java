//=======================================================================================
// This file is part of: ZigBee Localizer - LocationService
// A localization system for ZigBee networks, based on the analysis of RSSI.
// It estimates the positions of the mobile nodes. This Java program produces
// results in a accessible webservlet where the user can manage the WSN network.
//
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// Git Repository : https://github.com/alus-it/ZigBeeLocalizer.git
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

package it.ismb.locationService.uart;

import javax.comm.*;
import java.util.*;

public class ComLister {
	private static final long serialVersionUID = 1L;
	private static HashMap<String, CommPortIdentifier> map = new HashMap<String, CommPortIdentifier>(); //mapping from names to CommPortIdentifiers.
	private static List<String> list=new LinkedList<String>();
	private static int numOfPorts=0;

	public static void populate() {
		String osname = System.getProperty("os.name","").toLowerCase();
		if(osname.startsWith("linux")) try { //inizializzazione del driver per linux
			CommDriver driver=(CommDriver) Class.forName("com.sun.comm.LinuxDriver").newInstance();
			driver.initialize(); //ATT: può portare ad avere doppie porte se viene ricaricato
		} catch(Exception e) {
			System.out.println("ERROR while initializing the serial driver: "+e.getMessage());
		}
		else if(osname.startsWith("windows")) try { //inizializzazione del driver per windows
			CommDriver driver = (CommDriver) Class.forName("com.sun.comm.Win32Driver").newInstance();
			driver.initialize(); //ATT: può portare ad avere doppie porte se viene ricaricato
		} catch (Exception e) {
			System.out.println("ERROR while initializing the serial driver: "+e.getMessage());
		}
	    else {
	    	System.out.println("Sorry, your operating system is not supported");
	        return;
	    }		
		Enumeration<?> pList = CommPortIdentifier.getPortIdentifiers();
		while (pList.hasMoreElements()) {
			CommPortIdentifier cpi = (CommPortIdentifier)pList.nextElement();
			if(!map.containsKey(cpi.getName()) && cpi.getPortType()==CommPortIdentifier.PORT_SERIAL) { 
				map.put(cpi.getName(), cpi);
				list.add(cpi.getName());
				numOfPorts++;
			}
		}
	}
	
	public static CommPortIdentifier getCommPortIdenfifier(String comPortName) {
		if(map.containsKey(comPortName)) return map.get(comPortName);
		else return null;
	}

	public static boolean isReady() {
		if(numOfPorts>0) return true;
		else return false;
	}
	
	public static List<String> getComPortsNames() {
		return list;
	}
}
