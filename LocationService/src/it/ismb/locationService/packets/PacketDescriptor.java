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

public class PacketDescriptor {
	private int type;
	private int destinationNTWaddr;
	private int[] destinationIEEEaddr;
	private long trasmissionTime;
	
	public PacketDescriptor(int frametype, int destinationNTWaddress) {
		this.type=frametype;
		this.destinationNTWaddr=destinationNTWaddress;
		this.trasmissionTime=System.currentTimeMillis();
	}
	
	public PacketDescriptor(int destinationIEEEaddress[]) {
		this.type=Packet.ANNOUNCE_REF_PKT; //o l'uno o l'altro è lo stesso
		this.destinationIEEEaddr=destinationIEEEaddress;
		this.trasmissionTime=System.currentTimeMillis();
	}

	public int getType() {
		return type;
	}

	public int getDestinationNTWaddr() {
		return destinationNTWaddr;
	}
	
	public int[] getDestinationIEEEaddr() {
		return this.destinationIEEEaddr;
	}

	public long getTrasmissionTime() {
		return trasmissionTime;
	}
	
	public boolean compare(int frametype, int address) {
		if(this.type==frametype && this.destinationNTWaddr==address) return true;
		else return false;
	}

	public boolean isAnnceWithIEEE(int[] IEEEaddr) {
		if((this.type==Packet.ANNOUNCE_REF_PKT || this.type==Packet.ANNOUNCE_BLIND_PKT) &&
				Converter.compareIEEEaddr(IEEEaddr,this.destinationIEEEaddr)) return true;
		else return false;
	}
	
}
