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

public class DiagnosticPkt extends Packet {

	private int Vbatt; //da convertire
	private int RSSIparent;

	public DiagnosticPkt(int frametype, int senderAddr, byte[] rawsubpacket) {
		super(frametype,senderAddr);
		this.Vbatt=Converter.Decode16U(rawsubpacket,0,false);
		this.RSSIparent=Converter.Decode8(rawsubpacket,2);
	}
	
	public String toString() {
		return "Diagnostic, NTWaddr:"+this.NTWaddr+
		" Vbatt:"+this.Vbatt+" RSSIparent:"+this.RSSIparent;
	}
	
	public int getVbatt() {
		return this.Vbatt;
	}
	
	public int getRSSIparent() {
		return this.RSSIparent;
	}

}
