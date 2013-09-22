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

package it.ismb.locationService.packets;

import it.ismb.locationService.Converter;

public class AnnounceRefPkt extends Packet {

	private double x,y;
	private int roomId;
	private int a,n;
	private boolean automode;
	private double sensCycle;
	private int numOfSens;
	private int[] SensId;
	private int[] ieeeAddr;

	public AnnounceRefPkt(int announcetype, int senderAddress, byte[] rawsubpacket) {
		super(announcetype,senderAddress);
		this.x = Converter.DecodeDistance(rawsubpacket,0);
		this.y = Converter.DecodeDistance(rawsubpacket,2);
		this.roomId = Converter.Decode16U(rawsubpacket,4,false);
		this.a=Converter.Decode8U(rawsubpacket,6);
		this.n=Converter.Decode8U(rawsubpacket,7);
		if(Converter.Decode8U(rawsubpacket,8)==1) this.automode=true;
		else this.automode=false;
		this.sensCycle=Converter.Decode16U(rawsubpacket,9,false)/10;
		this.numOfSens=Converter.Decode8(rawsubpacket,11);
		if(numOfSens>0) {
			this.SensId=new int[numOfSens];
			for(int i=0;i<this.numOfSens;i++) SensId[i]=Converter.Decode8(rawsubpacket,i+12);
		}
		this.ieeeAddr=new int[8];
		this.ieeeAddr=Converter.DecodeIEEEaddr(rawsubpacket,rawsubpacket.length-8);
	}

	public String toString() {
		String returnString="AnnounceRef, IEEE:"+Converter.Vector2HexStringNoSpaces(ieeeAddr)+" NTWaddr:"+this.NTWaddr+
		" x:"+this.x+" y:"+this.y+" roomId:"+this.roomId+" Sensors: "+this.numOfSens+" ";
		for(int i=0;i<this.numOfSens;i++) returnString+="["+SensId[i]+"]";
		return returnString;
	}

	public int getRoomId() {
		return this.roomId;
	}

	public double getY() {
		return this.y;
	}

	public double getX() {
		return this.x;
	}
	
	public double getSensTime() {
		return this.sensCycle;
	}
	
	public boolean isAutomode() {
		return this.automode;
	}
	
	public int getA() {
		return this.a;
	}
	
	public int getNindex() {
		return this.n;
	}
	
	public int getNumOfSens() {
		return this.numOfSens;
	}
	
	public int[] getSensId() {
		if(numOfSens>0) return this.SensId;
		else return null;
	}
	
	public int[] getIEEEaddr() {
		return ieeeAddr;
	}


}
