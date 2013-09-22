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

public class RSSIpkt extends Packet {
	private int[] ieeeAddr;

	public class Neighbor {
		private int neighbor_id;
		private int rssi_uplink;
		private int sigma2;
		private int rssi_downlink;

		public Neighbor(int neighbor_id, int rssi_uplink, int sigma2, int rssi_downlink) {
			this.neighbor_id = neighbor_id;
			this.rssi_uplink = rssi_uplink;
			this.sigma2 = sigma2;
			this.rssi_downlink = rssi_downlink;
		}

		public String toString() {
			return "[NWK:"+this.neighbor_id+
			" Up:"+this.rssi_uplink+
			" Sigma2:"+this.sigma2+
			" Down:"+this.rssi_downlink+"]";
		}

		public int getRssiDownLink() {
			return this.rssi_downlink;
		}
		
		public int getSigma2() {
			return this.sigma2;
		}

		public int getRssiUpLink() {
			return this.rssi_uplink;
		}

		public int getNTWaddr() {
			return this.neighbor_id;
		}
	}

	private int numNbrs;
	private Neighbor[] neighbors;
	private SensPkt sensPkt;
	
	public String toString() {
		String ret="RSSI, NTWaddr:"+this.NTWaddr+" Neighbors:"+this.numNbrs+" ";
		for(int i=0;i<this.numNbrs;i++) ret+=this.neighbors[i];
		return ret;
	}

	public RSSIpkt(int frametype, int senderAddr, byte[] rawsubpacket) {
		super(frametype,senderAddr);
		this.numNbrs=Converter.Decode8(rawsubpacket,0);
		if(numNbrs>0) {
			this.neighbors = new Neighbor[this.numNbrs];
			for(int i=0;i<numNbrs;i++) {
				int neighbor_id=Converter.Decode16U(rawsubpacket,i*5+1,false);
				int rssi_uplink = Converter.Decode8U(rawsubpacket,i*5+3);
				int sigma2 = Converter.Decode8U(rawsubpacket,i*5+4);
				int rssi_downlink = Converter.Decode8U(rawsubpacket,i*5+5);
				this.neighbors[i] = new Neighbor(neighbor_id, rssi_uplink, sigma2, rssi_downlink);
			}
		}
		byte[] sensorUnderPkt;
		sensorUnderPkt=Converter.subarray(rawsubpacket,this.numNbrs*5+1,rawsubpacket.length-9); //8+1 x la pos
		this.sensPkt=new SensPkt(frametype,senderAddr,sensorUnderPkt);
		this.ieeeAddr=new int[8];
		this.ieeeAddr=Converter.DecodeIEEEaddr(rawsubpacket,rawsubpacket.length-8);
	}

	public int getNBrNum() {
		return this.numNbrs;
	}

	public Neighbor getNeighbor(int index) {
		if(index>=0 && index<numNbrs) return this.neighbors[index];
		else return null;
	}
	
	public int[] getIEEEaddr() {
		return ieeeAddr;
	}
	
	public SensPkt getSensPkt() {
		return this.sensPkt;
	}

}
