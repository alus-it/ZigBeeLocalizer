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

public class XYpkt extends Packet {

	private boolean status;
	private int numRefs, roomId;
	private double x,y;
	private int[] ieeeAddr;
	private SensPkt sensPkt;

	public XYpkt(int frametype, int senderAddr, byte[] rawsubpacket) {
		super(frametype,senderAddr);
		if(Converter.Decode8U(rawsubpacket,0)==1) this.status=true;
		else this.status=false;
		this.numRefs=Converter.Decode8U(rawsubpacket,1);
		this.x=Converter.DecodeDistance(rawsubpacket,2);
		this.y=Converter.DecodeDistance(rawsubpacket,4);
		this.roomId=Converter.Decode16U(rawsubpacket,6,false);
		byte[] sensorUnderPkt;
		sensorUnderPkt=Converter.subarray(rawsubpacket,8,rawsubpacket.length-9); //8+1 x la pos
		this.sensPkt=new SensPkt(frametype,senderAddr,sensorUnderPkt);
		this.ieeeAddr=new int[8];
		this.ieeeAddr=Converter.DecodeIEEEaddr(rawsubpacket,rawsubpacket.length-8);
	}

	public String toString() {
		return "XY pkt, NTWaddr:"+this.NTWaddr+" x:"+this.x+" y:"+this.y+" numRefs:"+this.numRefs+
		" roomId:"+this.roomId;
	}

	public int getNumRefs() {
		return this.numRefs;
	}

	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}

	public boolean getStatus() {
		return this.status;
	}

	public int getRoomId() {
		return this.roomId;
	}
	
	public int[] getIEEEaddr() {
		return ieeeAddr;
	}
	
	public SensPkt getSensPkt() {
		return this.sensPkt;
	}

}