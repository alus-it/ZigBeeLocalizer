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

public class Packet {
	public static final int ANNOUNCE_BLIND_PKT = 0x41;
	public static final int ANNOUNCE_REF_PKT   = 0x46;
	public static final int ACK_CONFIG_REF     = 0x11;
	public static final int ACK_CONFIG_BLIND   = 0xDD;
	public static final int XY_PKT             = 0x14;
	public static final int RSSI_PKT           = 0x22;
	public static final int SENS_PKT           = 0x44;
	public static final int DIAGNOSTIC_PKT     = 0x88;
	
	private int type;
	protected int NTWaddr;

	public Packet(int frametype, int shortAddr) {
		this.type = frametype;
		this.NTWaddr=shortAddr;
	}

	public int getPacketType() {
		return this.type;
	}

	public int getNTWaddr() {
		return this.NTWaddr;
	}
	
	public static Packet buildPacket(byte[] rawpacket) {
		int frametype = Converter.Decode8U(rawpacket,0);
		int senderAddr = Converter.Decode16U(rawpacket,1,false);
		byte[] rawsubpacket;
		rawsubpacket=Converter.subarray(rawpacket,3,rawpacket.length-1); //-1 perchè si intende la posizione
		switch (frametype) {
			case ANNOUNCE_REF_PKT:
				return new AnnounceRefPkt(frametype,senderAddr,rawsubpacket);
			case ANNOUNCE_BLIND_PKT:
				return new AnnounceBlindPkt(frametype,senderAddr,rawsubpacket);
			case ACK_CONFIG_REF:
				return new ACKconfigRefPkt(frametype,senderAddr,rawsubpacket);
			case ACK_CONFIG_BLIND:
				return new ACKconfigBlindPkt(frametype,senderAddr,rawsubpacket);
			case RSSI_PKT:
				return new RSSIpkt(frametype,senderAddr,rawsubpacket);
			case XY_PKT:
				return new XYpkt(frametype,senderAddr,rawsubpacket);
			case SENS_PKT:
				return new SensPkt(frametype,senderAddr,rawsubpacket);
			case DIAGNOSTIC_PKT:
				return new DiagnosticPkt(frametype,senderAddr,rawsubpacket);
			default:
				System.out.println("Packet ERROR: unknown frametype");
				return null;
		}
	}

}
