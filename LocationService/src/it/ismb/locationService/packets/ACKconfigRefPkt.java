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

public class ACKconfigRefPkt extends Packet {

	private int roomId,a,n;
	private double x,y,cycle;
	private boolean automode;
	

	public ACKconfigRefPkt(int frametype, int senderAddress, byte[] rawsubpacket) {
		super(frametype,senderAddress);
		this.x=Converter.DecodeDistance(rawsubpacket,0);
		this.y=Converter.DecodeDistance(rawsubpacket,2);
		this.roomId=Converter.Decode16U(rawsubpacket,4,false);
		this.a=Converter.Decode8U(rawsubpacket,6);
		this.n=Converter.Decode8U(rawsubpacket,7);
		if(Converter.Decode8U(rawsubpacket,8)==0) this.automode=false;
		else this.automode=true;
		this.cycle=Converter.Decode16U(rawsubpacket,9,false)/10;
	}

	public String toString() {
		return "ACK update Ref pos, NTWaddr:"+this.NTWaddr+" IEEEaddr:"+
		" x:"+this.x+" y:"+this.y+" roomId:"+this.roomId;
	}

	public double getX() {
		return this.x;
	}

	public int getRoomId() {
		return this.roomId;
	}

	public double getY() {
		return this.y;
	}
	
	public double getN() {
		return this.n;
	}
	
	public double getA() {
		return this.a;
	}
	
	public boolean isAutomode() {
		return this.automode;
	}
	
	public double getCycle() {
		return this.cycle;
	}

}
