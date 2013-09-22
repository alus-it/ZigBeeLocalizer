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

package it.ismb.locationService;

import it.ismb.locationService.db.DBmanager;
import it.ismb.locationService.uart.ComLister;
import it.ismb.locationService.uart.UARTmanager;

import java.io.IOException;
import javax.comm.CommPortIdentifier;

public class PacketDecoderMain {
	private static boolean serviceStarted=false;
	private static boolean uartStarted=false;
	private static boolean DBmanagerStarted=false;
	private static boolean packetDecoderStarted=false;

	public static void main(String[] args) { //starts all the components in the right order
		if(args.length!=4) {
			System.out.println("USAGE: java locationService.PacketDecoderMain <ComPort> <DBserver> <DBuser> <DBpassword>\n" +
					"where <ComPort> is the COM port where is attached the gateway (Ex. COM1)\n" +
					"<DBserver> is the name or IP address of the DB server (Ex. localhost)\n" +
					"<DBuser> is the username for accessing to the DB server (Ex. locationService)\n" +
					"<DBpassword> is the password for accessing the DB server.");
			return;
		}
		if(!DBmanagerStarted) {
			new DBmanager(args[1],args[2],args[3],"locationdb"); //locationdb is the name of the DB schema
			DBmanagerStarted=true;
		}
		if(!uartStarted) {
			if(!ComLister.isReady()) ComLister.populate();
			CommPortIdentifier comPortId=ComLister.getCommPortIdenfifier(args[0]);
			if(comPortId==null) System.out.println("ERROR: Requested COM port not found!");
			else try {
				new UARTmanager(comPortId);
				uartStarted=true;
			} catch (IOException e) {
				System.out.println("WARNING: UARTmanager is not started due to serial port errors: ");
				e.printStackTrace();
			}
		}
		if(!packetDecoderStarted) {
			new PacketDecoder();
			if(DBmanagerStarted) {
				PacketDecoder.load();
				if(uartStarted) DBmanager.checkAllRefNodesInDB();
				else System.out.println("ERROR: UARTmanager is not started can't initialize the nodes.");
			}
			else System.out.println("ERROR: DBmanager is not started can't load data from DB.");
			packetDecoderStarted=true;
		}
		serviceStarted=true;
	}
	
	public static void Stop() {
		serviceStarted=false;
		if(uartStarted) {
			uartStarted=false;
			UARTmanager.Stop();
		}
		if(packetDecoderStarted) {
			packetDecoderStarted=false;
			PacketDecoder.Stop();
		}
		if(DBmanagerStarted) {
			DBmanagerStarted=false;
			DBmanager.Stop();
		}
		System.gc();
	}
	
	public static boolean isServiceStarted() {
		return serviceStarted;
	}

	public static boolean isUartStarted() {
		return uartStarted;
	}

	public static boolean isDBmanagerStarted() {
		return DBmanagerStarted;
	}
	
	public static boolean isPacketDecoderStarted() {
		return packetDecoderStarted;
	}
}
