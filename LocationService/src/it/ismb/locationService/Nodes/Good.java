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

package it.ismb.locationService.Nodes;

import it.ismb.locationService.db.DBmanager;

import java.util.HashMap;


public class Good {
	private static HashMap<Integer, String> types = new HashMap<Integer, String>();
	private String name;
	private int typeId;
	private boolean isAssociated;
	private BlindNode associatedBlind;
	//aggiungere qui altri dati relativi ai beni
	
	public Good(String name, int typeId) {
		this.name=name;
		this.typeId=typeId;
		this.associatedBlind=null;
		this.isAssociated=false;
	}
	
	public boolean isAssociated() {
		return this.isAssociated;
	}
	
	public void associateBlindNode(BlindNode blindNode) {
		this.associatedBlind=blindNode;
		blindNode.assocG(this); //associa reciprocamente anche il BlindNode a questo bene
		this.isAssociated=true;
	}
	
	public void assocBN(BlindNode blindNode) { //richiamata solo da BlindNode.associateGood(Good)
		this.associatedBlind=blindNode;
		this.isAssociated=true;
	}
	
	public void disassociate() {
		this.associatedBlind.disassoc();
		this.associatedBlind=null;
		this.isAssociated=false;
	}
	
	public void disassoc() {
		this.associatedBlind=null;
		this.isAssociated=false;
	}
	
	public BlindNode getAssocitedBlindNode() {
		return this.associatedBlind;
	}
	
	public void setName(String newName) {
		this.name=newName;
	}
	
	public void setTypeId(int newId) {
		this.typeId=newId;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getTypeId() {
		return this.typeId;
	}
	
	public String getType() {
		return types.get(this.typeId);
	}
	
	public static HashMap<Integer, String> getTypes() {
		return types;
	}
	
	public static void loadType(int id, String name) { //so già dove mettere l'id
		types.put(id,name);
	}
	
	public static int insertType(String name) { //notare l'eleganza di questa funzione
		int i=0;
		if(!types.isEmpty()) //se la lista è vuota non mi faccio problemi
			if(types.containsValue(name)) return(-1); //esiste già un tipo con lo stesso nome
			else for(i=0;types.containsKey(i);i++); //cerco il primo id libero
		types.put(i,name);
		DBmanager.insertType(i,name);
		return(i);
	}
	
	public static String getTypeName(int typeID) {
		return types.get(typeID);
	}

}
