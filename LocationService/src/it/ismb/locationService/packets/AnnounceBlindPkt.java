//=======================================================================================
// This file is part of: ZigBee Localizer - LocationService
// A localization system for ZigBee networks, based on the analysis of RSSI.
// It estimates the positions of the mobile nodes. This Java program produces
// results in a accessible webservlet where the user can manage the WSN network.
//
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

package it.ismb.locationService.packets;

import it.ismb.locationService.Converter;

public class AnnounceBlindPkt extends Packet {
	
	private float timeout;
	private float cycle;
	private boolean autoMode;
	private boolean distribuitedLoc;
	private int numOfSens;
	private int[] SensId;
	private int[] ieeeAddr;

	public AnnounceBlindPkt(int announcetype, int senderAddr, byte[] rawsubpacket) {
		super(announcetype,senderAddr);
		if(Converter.Decode8(rawsubpacket,0)==0) this.autoMode=false;
		else this.autoMode=true;
		this.timeout=Converter.Decode16U(rawsubpacket,1,false)/10;
		this.cycle=Converter.Decode16U(rawsubpacket,3,false)/10;
		if(Converter.Decode8(rawsubpacket,5)==0) this.distribuitedLoc=false;
		else this.distribuitedLoc=true;
		this.numOfSens=Converter.Decode8(rawsubpacket,6);
		if(numOfSens>0) {
			this.SensId=new int[numOfSens];
			for(int i=0;i<numOfSens;i++) SensId[i]=Converter.Decode8(rawsubpacket,i+7);
		}
		this.ieeeAddr=new int[8];
		this.ieeeAddr=Converter.DecodeIEEEaddr(rawsubpacket,rawsubpacket.length-8);
	}

	public String toString() {
		String returnString="AnnounceBlind, IEEE:"+Converter.Vector2HexStringNoSpaces(ieeeAddr)+" NTWaddr:"+this.NTWaddr+" Sensors:"+this.numOfSens+" ";
		for(int i=0;i<this.numOfSens;i++) returnString+="["+SensId[i]+"]";
		return returnString;
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

	public float getTimeout() {
		return timeout;
	}

	public float getCycle() {
		return cycle;
	}

	public boolean isAutoMode() {
		return autoMode;
	}

	public boolean isDistribuitedLoc() {
		return distribuitedLoc;
	}

}
