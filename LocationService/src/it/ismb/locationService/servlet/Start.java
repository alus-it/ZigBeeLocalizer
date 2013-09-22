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

import it.ismb.locationService.PacketDecoderMain;
import it.ismb.locationService.uart.ComLister;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Start extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String defaultComPort = "/dev/ttyUSB0"; //è la porta preselezionata
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
    
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType( "text/html" );
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println("<li><a href=\"index.html\">Home</a></li>"+
    			"<li><a id=\"activelink\" href=\"#\">Start/Stop</a></li>"+
    			"<li><a href=\"Status\">Status</a></li>"+
    			"<li><a href=\"Goods\">Goods</a></li>"+
    			"<li><a href=\"BlindNodes\">BlindNodes</a></li>"+
    			"<li><a href=\"RefNodes\">RefNodes</a></li>"+
    			"<li><a href=\"Rooms\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"#\" class=\"current\">Start/Stop</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>LocationService Starter</h1>");
    	if(PacketDecoderMain.isServiceStarted()) {
    		out.println("<h3>Service is started</h3><br/>");
    		out.println("<form action=\"Start\" method=\"post\">");
    		out.println("<input type=\"submit\" value=\"Stop Service\"></form>");
    	}
    	else {
    		out.println("<h3>Service is stopped</h3>");
    		ComLister.populate();
    		List<String> portsNames=ComLister.getComPortsNames();
    		out.println("<p>Select the COM port where is connected the ZigBee Gateway and set the DB connection parameter.</p>");
    		out.println("<form action=\"Start\" method=\"post\">" );
    		out.println("<table><tr><td>COM Port:</td><td><select name=\"ComPort\" size=\"1\">");
    		Iterator<String> itr=portsNames.iterator();
    		String portName;
    		while(itr.hasNext()) {
    			portName=(String) itr.next();
    			if(portName.compareTo(defaultComPort)!=0)
    				out.println("<option value=\""+portName+"\">"+portName+"</option>");
    			else out.println("<option selected=\"selected\" value=\""+portName+"\">"+portName+"</option>");
    		}
    		out.println("</select><td/></tr>");
    		out.println("<tr><td>DB Server:</td><td><input name=\"DBserver\" size=\"20\" value=\"localhost\" /></td></tr>");
    		out.println("<tr><td>DB User:</td><td><input name=\"DBuser\" size=\"20\" value=\"locationService\" /></td></tr>");
    		out.println("<tr><td>DB Password:</td><td><input type=\"password\" name=\"DBpasswd\" size=\"20\" value=\"pass\" /></td></tr></table>");
    		out.println("<input type=\"submit\" value=\"Start Service\"></form>");
    	}
    	out.println(Html.footer);
    }
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println("<li><a href=\"index.html\">Home</a></li>"+
    			"<li><a id=\"activelink\" href=\"Start\">Start/Stop</a></li>"+
    			"<li><a href=\"Status\">Status</a></li>"+
    			"<li><a href=\"Goods\">Goods</a></li>"+
    			"<li><a href=\"BlindNodes\">BlindNodes</a></li>"+
    			"<li><a href=\"RefNodes\">RefNodes</a></li>"+
    			"<li><a href=\"Rooms\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"Start\" class=\"current\">Start/Stop</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>LocationService Starter</h1>");
    	if(it.ismb.locationService.PacketDecoderMain.isServiceStarted()) {
    		out.println("<h3>Stopping service...</h3>");
			PacketDecoderMain.Stop();
    	} else {
    		out.println("<h3>Starting service...</h3>");
    		String args[]=new String[4];
    		args[0]=req.getParameter("ComPort");
    		args[1]=req.getParameter("DBserver");
    		args[2]=req.getParameter("DBuser");
    		args[3]=req.getParameter("DBpasswd");
    		PacketDecoderMain.main(args);
    		out.println("<p>Now, please wait util the system is fully initialized, you can check the status of the "+
    				"system in the <a href=\"Status\">Status page</a>.</p>");
    	}
    	out.println("<br/><ul>");
    	if(PacketDecoderMain.isServiceStarted()) out.println("<li><b>Service</b> is started</li>");
    	else out.println("<li><b>Service</b> is not started</li>");
    	if(PacketDecoderMain.isDBmanagerStarted()) out.println("<li><b>DB manager</b> is started</li>");
    	else out.println("<li><b>DB manager</b> is not started</li>");
    	if(PacketDecoderMain.isUartStarted()) out.println("<li><b>UART manager</b> is started</li>");
    	else out.println("<li><b>UART manager</b> is not started</li>");
    	if(PacketDecoderMain.isPacketDecoderStarted()) out.println("<li><b>PacketDecoder</b> is started</li></ul>");
    	else out.println("<li><b>PacketDecoder</b> is not started</li></ul>");
    	out.println(Html.footer);
    }
}
