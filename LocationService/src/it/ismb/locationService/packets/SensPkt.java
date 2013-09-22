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

public class SensPkt extends Packet {

	private int numOfSens;
	private int[] SensIds;
	private int[] SensValues;

	public SensPkt(int frametype, int senderAddr, byte[] rawsubpacket) {
		super(frametype,senderAddr);
		this.numOfSens=Converter.Decode8(rawsubpacket,0);
		if(numOfSens>0) {
			this.SensIds=new int[numOfSens];
			this.SensValues=new int[numOfSens];
			for(int i=0;i<numOfSens;i++) {
				this.SensIds[i]=Converter.Decode8(rawsubpacket,i*3+1);
				this.SensValues[i]=Converter.Decode16U(rawsubpacket,i*3+2,false);
			}
		}
	}
	
	public String toString() {
		String ret="Sens, NTWaddr:"+this.NTWaddr+" Sensors:"+this.numOfSens+" ";
		for(int i=0;i<this.numOfSens;i++) ret+="[Id:"+this.SensIds[i]+" Value:"+this.SensValues[i]+"]";
		return ret;
	}
	
	public int getNumOfSens() {
		return this.numOfSens;
	}
	
	public int getSensId(int index) {
		return this.SensIds[index];
	}

	public int getSensValue(int index) {
		return this.SensValues[index];
	}
	
	public int[] getSensIds() {
		return this.SensIds;
	}

	public int[] getSensValues() {
		return this.SensValues;
	}

}