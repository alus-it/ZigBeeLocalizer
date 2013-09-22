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

package it.ismb.locationService.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class MysqlDatabase {

	private String server;
	private String user;
	private String pass;
	private String dbname;
	private Connection con;
	private String lastquery;

	public MysqlDatabase(String server, String user, String pass, String dbname) {

		this.server = server;
		this.user = user;
		this.pass = pass;
		this.dbname = dbname;
		this.lastquery = null;

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		String url = "jdbc:mysql://" + server + ":3306/" + dbname;

		try {
			this.con = DriverManager.getConnection(url, user, pass);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public boolean isavailable() {
		boolean valid = true;
		Statement stmt = null;
		try {
			stmt = this.con.createStatement();
			if (stmt == null)
				return false;
		} catch (SQLException e) {

			valid = false;
		}

		try {
			if (stmt != null)
				stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return valid;
	}

	public void disconnect() {
		try {
			this.con.close();
		} catch (SQLException e) {
				e.printStackTrace();
		}

	}

	public MysqlDatabaseResponse query(String string) {
		Statement stmt = null;
		this.lastquery = string;
		ResultSet s = null;
		try {
			stmt = this.con.createStatement();
			s = stmt.executeQuery(string);

		} catch (SQLException e) {
			MysqlDatabaseResponse ret = new MysqlDatabaseResponse(null, false);

			try {
				stmt.close();

			} catch (Exception e1) {
		
				e1.printStackTrace();
			}
			return ret;
		}

		MysqlDatabaseResponse ret = new MysqlDatabaseResponse(s, true);
		return ret;

	}

	public boolean queryNoResponse(String string) {
		this.lastquery = string;
		try {
			Statement stmt = this.con.createStatement();
			stmt.executeUpdate(string);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public String getdbname() {
		return this.dbname;
	}

	public String getlastquery() {
		return this.lastquery;
	}

	public String getpass() {
		return this.pass;
	}

	public String getserver() {
		return this.server;
	}

	public String getuser() {
		return this.user;
	}

}
