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

package it.ismb.locationService;

public class Room {
	public static final int DEFAULT_A_PARAM=39;
	public static final int DEFAULT_N_INDEX=16;
	public static final float DEFAULT_N_PARAM=(float) 3.375;
	private static final double[] n_indexVector={1.,1.25,1.5,1.75,1.875,2.,2.125,2.25,2.375,2.5,2.625,2.75
		,2.875,3.,3.125,3.25,3.375,3.5,3.625,3.75,3.875,4.,4.125,4.25,4.375,4.5,4.625,5.,5.5,6.,7.,8.};
	private String name;
	private int id, n_index, paramA;
	private double distMax, A, n;
	
	public Room(int roomId, String roomName, double maxDist, double n, double A) {
		this.id=roomId;
		this.name=roomName;
		this.distMax=maxDist;
		this.setN(n);
		this.setA(A);
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setA(double A) {
		if(A>=30 && A<=50) {
			int Aparam=(int)A;
			if(A>Aparam+0.5) Aparam++;
			this.A=A;
			this.paramA=Aparam;
		} else {
			this.paramA=DEFAULT_A_PARAM;
			this.A=DEFAULT_A_PARAM;
		}
	}
	
	public double getA() {
		return this.A;
	}

	public int getParamA() {
		return this.paramA;
	}
	
	public void setN(double n) {
		if(n>=1 && n<=8) {
			this.n=n;
			int nIndex=0;
			while(n>n_indexVector[nIndex]) nIndex++;
			if(nIndex>0 && n_indexVector[nIndex]-n>n-n_indexVector[nIndex-1]) nIndex--;
			this.n_index=nIndex;
		} else {
			this.n_index=DEFAULT_N_INDEX;
			this.n=DEFAULT_N_PARAM;
		}
		
	}

	public int getN_index() {
		return n_index;
	}
	
	public double getN() {
		return n;
	}

	public void setMaxDist(double maxDist) {
		this.distMax = maxDist;
	}

	public double getMaxDist() {
		return distMax;
	}

}
