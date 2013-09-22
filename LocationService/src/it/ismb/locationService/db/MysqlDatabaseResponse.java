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

package it.ismb.locationService.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MysqlDatabaseResponse {
	private boolean valid;
	private Map<String, Integer> column_names;
	private Map<Integer, Map<String, String>> values_map;

	public MysqlDatabaseResponse(ResultSet s, boolean valid) {
		this.valid = valid;
		this.column_names = new HashMap<String, Integer>();
		this.values_map = new TreeMap<Integer, Map<String, String>>();
		ResultSetMetaData rsmd;

		try {
			rsmd = s.getMetaData();
			int numColumns = rsmd.getColumnCount();
			for (int i = 1; i < numColumns + 1; i++) {
				String nomecolonna = rsmd.getColumnName(i);
				this.column_names.put(nomecolonna, new Integer(i));
			}
		} catch (Exception e) {
			this.valid = false;
		}
		if (this.valid) {
			// patch: continuo solo in caso di valori consistenti
			int counter = 0;
			try {
				while (s.next()) { // per ogni riga del resultset
					Iterator<String> i = this.column_names.keySet().iterator();
					Map<String, String> riga = new HashMap<String, String>();
					while (i.hasNext()) { // per ogni colonna della riga
						String key = i.next();
						String value = null;
						boolean canadd = true;
						try {
							value = s.getString(key);
						} catch (SQLException e) {
							canadd = false;
						}
						if(canadd) riga.put(key, value);
					} // fine per ogni colonna della tabella
					this.values_map.put(new Integer(counter++), riga);

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}// fine per ogni riga del resultset

			// attenzione: non aggiungo alla risposta i valori nulli
			try {
				java.sql.Statement tmp = s.getStatement();
				s.close();
				tmp.close();

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	public boolean success() {
		return this.valid;
	}

	public int nlines() {
		return this.values_map.size();
	}

	public Map<Integer, Map<String, String>> getvaluesmap() {
		return this.values_map;
	}

}
