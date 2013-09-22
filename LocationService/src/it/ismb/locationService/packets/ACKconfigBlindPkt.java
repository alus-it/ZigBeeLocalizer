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

public class ACKconfigBlindPkt extends Packet {

	private boolean autoMode, distribuitedLoc;
	private double timeOut,cycle;

	public ACKconfigBlindPkt(int frametype, int senderAddress, byte[] rawsubpacket) {
		super(frametype,senderAddress);
		if(Converter.Decode8U(rawsubpacket,0)==1) this.autoMode=true;
		else this.autoMode=false;
		if(Converter.Decode8U(rawsubpacket,1)==1) this.distribuitedLoc=true;
		else this.distribuitedLoc=false;
		this.timeOut=Converter.Decode16U(rawsubpacket,2,false)/10;
		this.cycle=Converter.Decode16U(rawsubpacket,4,false)/10;
	}

	public String toString() {
		return "ACK confif Blind, NTWaddr:"+this.NTWaddr+" timeout:"+this.timeOut+" cycle:"+this.cycle;
	}

	public double getTimeOut() {
		return this.timeOut;
	}

	public double getCycle() {
		return this.cycle;
	}
	
	public boolean isAutoMode() {
		return this.autoMode;
	}
	
	public boolean isDistributtedLoc() {
		return this.distribuitedLoc;
	}

}