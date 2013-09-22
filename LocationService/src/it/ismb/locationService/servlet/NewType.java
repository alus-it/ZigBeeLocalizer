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

package it.ismb.locationService.servlet;

import it.ismb.locationService.Nodes.Good;
import it.ismb.locationService.db.DBmanager;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NewType extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
       
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"Types\">Types</a>"+
		    	"&gt;<a href=\"#\" class=\"current\">New type</a>"+
		    	"</div><div id=\"content\">");
    	out.println("<h1>Insert new type</h1>");
    	out.println("<form action=\"NewType\" method=\"post\">" );
    	out.println("<p>Type name:</p><input name=\"newTypeName\" size=\"20\" value=\"\" />");	
    	out.println("<input type=\"submit\" value=\"Insert type\"></form>");
    	out.println(Html.footer);
    }
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"Types\">Types</a>"+
		    	"&gt;<a href=\"#\" class=\"current\">New type</a>"+
		    	"</div><div id=\"content\">");
    	out.println("<h1>Inserting new type...</h1>");
    	String newTypeName=(String)req.getParameter("newTypeName");
    	if(!DBmanager.existsType(newTypeName)) {
    		Good.insertType(newTypeName);
    		out.println("<h3>New type inserted succesfully.</h3>");
    	}
    	else out.println("<h3>ERROR: There is another type with the same name!</h3>");
    	out.println(Html.footer);
    }
}