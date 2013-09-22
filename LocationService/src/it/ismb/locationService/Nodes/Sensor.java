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

public class Sensor {
	public static final int TEMP_ID  =0; // Teperature
	public static final int BATT_ID  =1; // Battery Reading
	public static final int LIGHT_ID =2; // Light Reading
	public static final int X_ACC_ID =3; // X_Acc Reading
	public static final int Y_ACC_ID =4; // Y_Acc Reading
	public static final int POTENT_ID=5; // Potentiometer
	public static final int SWITCH_ID=6; // Switch
	private static final double V_BATT_OFFSET=0.05;
	private int id;
	private int rawValue;
	private double value;
	
	public Sensor(int sensId) {
		if(id>=0 && id<=6) {
			this.id=sensId;
		} else this.id=0;
	}
	
	public double setValue(int redValue) {
		this.rawValue=redValue;
		switch(this.id) {
			case TEMP_ID:
				this.value=redValue/36; //da verificare!!!
				break;
			case BATT_ID:
				this.value=redValue*(15.0/4094.0)+V_BATT_OFFSET;
				break;
			case LIGHT_ID: this.value=redValue; break;
			case X_ACC_ID: this.value=redValue; break;
			case Y_ACC_ID: this.value=redValue; break;
			case POTENT_ID: this.value=redValue; break;
			case SWITCH_ID: this.value=redValue;
		}
		return this.value;
	}
	
	public void setValue(double value) {
		this.rawValue=0;
		this.value=value;
	}
	
	public int getRawValue() {
		return this.rawValue;
	}
	
	public double getValue() {
		return this.value;
	}
	
	public int getId() {
		return this.id;
	}

	public String getSensorName() {
		String ret="";
		switch(this.id) {
			case TEMP_ID: ret="Temperature"; break;
			case BATT_ID: ret="Battery Voltage"; break;
			case LIGHT_ID: ret="Light intensity"; break;
			case X_ACC_ID: ret="X axis accelerometer"; break;
			case Y_ACC_ID: ret="Y axis accelerometer"; break;
			case POTENT_ID: ret="Potentiometer"; break;
			case SWITCH_ID: ret="Switch"; break;
		}
		return ret;
	}
	
	public String getSensorUnit() {
		String ret="";
		switch(this.id) {
			case TEMP_ID: ret="°C"; break;
			case BATT_ID: ret="V"; break;
			case LIGHT_ID: ret="cd/m²"; break;
			case X_ACC_ID:
			case Y_ACC_ID: ret="m/s²"; break;
			case POTENT_ID:
			case SWITCH_ID: ret="pos"; break;
		}
		return ret;
	}
	
	public String getSensorUnitHTML() {
		String ret="";
		switch(this.id) {
			case TEMP_ID: ret="&deg;C"; break;
			case BATT_ID: ret="V"; break;
			case LIGHT_ID: ret="cd/m&sup2;"; break;
			case X_ACC_ID:
			case Y_ACC_ID: ret="m/s&sup2;"; break;
			case POTENT_ID:
			case SWITCH_ID: ret="pos"; break;
		}
		return ret;
	}

}
