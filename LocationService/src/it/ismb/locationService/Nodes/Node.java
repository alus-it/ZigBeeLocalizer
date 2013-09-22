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

package it.ismb.locationService.Nodes;
import it.ismb.locationService.Converter;
import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.Room;
import it.ismb.locationService.packets.*;

import java.util.HashMap;


public class Node {
	protected int[] IEEEaddr;
	protected int NTWaddr;
	protected double xPos,yPos;
	protected int numOfSens;
	protected Room room;
	protected HashMap<Integer,Sensor> sensors=null;
	protected int parentRSSI;
	protected long timestamp;
	protected String name;
	protected boolean autoMode;
	protected double cycle;
	protected boolean isRef;
	
	public Node(int[] longAddr, int shortAddr, double x, double y, Room imInRoom, int sensNo, long timest, 
			String nodename, boolean automode, double cycleTime, boolean isReference) {
		this.IEEEaddr=new int[8];
		this.IEEEaddr=longAddr;
		this.NTWaddr=shortAddr;
		this.xPos=x;
		this.yPos=y;
		this.room=imInRoom;
		this.numOfSens=sensNo;
		this.timestamp=timest; 
		this.parentRSSI=-20;
		this.name=nodename;
		this.autoMode=automode;
		this.cycle=cycleTime;
		this.isRef=isReference;
		if(sensNo>0) this.sensors=new HashMap<Integer,Sensor>();
	}
	
	public Node(AnnounceRefPkt refAnnounce) {
		this.name="New RefNode";
		this.IEEEaddr=refAnnounce.getIEEEaddr();
		this.NTWaddr=refAnnounce.getNTWaddr();
		this.xPos=refAnnounce.getX();
		this.yPos=refAnnounce.getY();
		this.room=PacketDecoder.getRoomById(refAnnounce.getRoomId()); //messo a zero se non esiste
		this.autoMode=refAnnounce.isAutomode();
		this.cycle=refAnnounce.getSensTime();
		this.numOfSens=refAnnounce.getNumOfSens();
		this.isRef=true;
		if(numOfSens>0) {
			this.sensors=new HashMap<Integer,Sensor>();
			for(int i=0;i<numOfSens;i++) {
				Sensor currentSensor=new Sensor(refAnnounce.getSensId()[i]);
				this.sensors.put(refAnnounce.getSensId()[i],currentSensor);
			}
		}	
		this.timestamp=System.currentTimeMillis();
	}
	
	public double setDiagnostic(DiagnosticPkt diaPkt) {
		double ret=this.sensors.get(Sensor.BATT_ID).setValue(diaPkt.getVbatt());
		this.parentRSSI=-diaPkt.getRSSIparent();
		this.timestamp=System.currentTimeMillis();
		return ret;
	}
	
	public boolean isReference() {
		return this.isRef;
	}
	
	public double getVbatt() {
		return this.sensors.get(Sensor.BATT_ID).getValue();
	}
	
	public int getParentRSSI() {
		return this.parentRSSI;
	}
	
	public int[] getIEEEaddr() {
		return this.IEEEaddr;
	}
	
	public String getIEEEaddrString() {
		return Converter.Vector2HexStringNoSpaces(this.IEEEaddr);
	}
	
	public void setNTWaddr(int nTWaddr) {
		this.NTWaddr = nTWaddr;
		this.timestamp=System.currentTimeMillis();
	}
	
	public int getNTWaddr() {
		return this.NTWaddr;
	}
	
	public void setPosition(double x, double y, int roomId) {
		this.xPos = x;
		this.yPos = y;
		this.room = PacketDecoder.getRoomById(roomId);
		this.timestamp=System.currentTimeMillis();
	}
	
	public void setXYPosition(double x, double y) {
		this.xPos = x;
		this.yPos = y;
	}
	
	public void setCycle(double time) {
		this.cycle = time;
	}

	public double getCycle() {
		return this.cycle;
	}
	
	public void setAutoMode(boolean isAutoMode) {
		this.autoMode = isAutoMode;
	}

	public boolean isAutoMode() {
		return this.autoMode;
	}

	public double getXpos() {
		return this.xPos;
	}
	
	public double getYpos() {
		return this.yPos;
	}
	
	public int getNumOfSens() {
		return this.numOfSens;
	}

	public int getRoomId() {
		return this.room.getId();
	}
	
	public Room getRoom() {
		return this.room;
	}
	
	public boolean hasSensId(int id) {
		if(this.sensors.containsKey(id)) return true;
		else return false;
	}
	
	public double getSensValue(int id) {
		if(this.sensors.containsKey(id)) return this.sensors.get(id).getValue();
		else return 0;
	}
	
	public void setSensor(int id, int rawValue) {
		if(this.sensors.containsKey(id)) this.sensors.get(id).setValue(rawValue);
		else {
			Sensor newSens=new Sensor(id);
			newSens.setValue(rawValue);
			this.sensors.put(id,newSens);
		}
	}
	
	public void setSensor(int id, double value) {
		if(this.sensors.containsKey(id)) this.sensors.get(id).setValue(value);
		else {
			Sensor newSens=new Sensor(id);
			newSens.setValue(value);
			this.sensors.put(id,newSens);
		}
	}
	
	public void setSensors(SensPkt sensPkt) {
		this.timestamp=System.currentTimeMillis();
		for(int i=0;i<sensPkt.getNumOfSens();i++) {
			int currentId=sensPkt.getSensId(i);
			if(!this.sensors.containsKey(currentId)) {
				Sensor currentSens=new Sensor(currentId);
				currentSens.setValue(sensPkt.getSensValue(i));
				this.sensors.put(currentId,currentSens);
			} else this.sensors.get(currentId).setValue(sensPkt.getSensValue(i));
		}
	}
	
	public HashMap<Integer,Sensor> getSensors() {
		return this.sensors;
	}

	public long getTimestamp() {
		return timestamp;
	}

//	public void setAutoTxSensTime(float time) {
//		this.autoTxSensTime = time;
//	}

//	public float getAutoTxSensTime() {
//		return autoTxSensTime;
//	}

	public void setName(String text) {
		this.name = text;
	}

	public String getName() {
		return name;
	}
	
	public void setRoom(Room newRoom) {
		this.room=newRoom;
	}

	public void setVbatt(double volts) {
		if(!this.sensors.containsKey(Sensor.BATT_ID)) {
			Sensor battSens=new Sensor(Sensor.BATT_ID);
			battSens.setValue(volts);
			this.sensors.put(Sensor.BATT_ID,battSens);
		} else this.sensors.get(Sensor.BATT_ID).setValue(volts);
	}

	public void setParentRSSI(int parentRSSI) {
		this.parentRSSI=parentRSSI;
	}

}
