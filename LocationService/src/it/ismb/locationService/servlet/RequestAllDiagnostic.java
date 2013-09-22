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

package it.ismb.locationService.servlet;

import it.ismb.locationService.PacketDecoder;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestAllDiagnostic extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
    
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"#\" class=\"current\">RequestAllDiagnostic</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Request diagnostic data</h1>");
    	int isRef=Integer.parseInt(req.getParameter("isRef"));
    	if(isRef==0) PacketDecoder.requestAllBlindsNodesDiagnostic();
    	else PacketDecoder.requestAllRefNodesDiagnostic();
    	out.println("<h3>Processing request...</h3>");
    	if(isRef==1) out.println("<br/><form action=\"RefNodes\" method=\"get\">");
    	else out.println("<br/><form action=\"BlindNodes\" method=\"get\">");
    	out.println("<input type=\"submit\" value=\"Back to node list\"></form>"+Html.footer);
    }
}
