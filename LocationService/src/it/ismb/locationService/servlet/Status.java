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
import it.ismb.locationService.PacketDecoderMain;
import it.ismb.locationService.db.DBmanager;
import it.ismb.locationService.uart.UARTmanager;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Status extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
    
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println("<li><a href=\"index.html\">Home</a></li>"+
    			"<li><a href=\"Start\">Start/Stop</a></li>"+
    			"<li><a id=\"activelink\" href=\"#\">Status</a></li>"+
    			"<li><a href=\"Goods\">Goods</a></li>"+
    			"<li><a href=\"BlindNodes\">BlindNodes</a></li>"+
    			"<li><a href=\"RefNodes\">RefNodes</a></li>"+
    			"<li><a href=\"Rooms\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"#\" class=\"current\">Status</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>System Status</h1><ul>");
    	if(PacketDecoderMain.isServiceStarted()) out.println("<li>Service is started</li></ul><br/>");
    	else out.println("<li><b>Service is not started</b></li></ul><br/>");
    	out.println("<h3>DB Manager</h3><ul>");
    	if(PacketDecoderMain.isDBmanagerStarted()) out.println("<li>DB manager is started</li>");
    	else out.println("<li><b>DB manager is not started</b></li>");
    	if(DBmanager.isActive()) out.println("<li>DB manager is active</li></ul><br/>");
    	else out.println("<li><b>DB manager is not active</b></li></ul><br/>");
    	out.println("<h3>UART Manager</h3><ul>");
    	if(PacketDecoderMain.isUartStarted()) out.println("<li>UART manager is started</li>");
    	else out.println("<li><b>UART manager is not started</b></li>");
    	if(UARTmanager.isActive()) out.println("<li>UART manager is active</li></ul><br/>");
    	else out.println("<li><b>UART manager is not active</b></li></ul><br/>");
    	out.println("<h3>Packet Decoder</h3><ul>");
    	if(PacketDecoderMain.isPacketDecoderStarted()) out.println("<li>The PacketDecoder is started</li>");
    	else out.println("<li><b>The PacketDecoder is not started</b></li>");
    	if(PacketDecoder.isSystemStarted()) out.println("<li>System is initialized</li></ul>");
    	else out.println("<li><b>System is not initialized</b></li></ul>");
    	out.println(Html.footer);
    }

}
