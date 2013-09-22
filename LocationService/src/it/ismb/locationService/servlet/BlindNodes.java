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

import it.ismb.locationService.Converter;
import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.Nodes.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class BlindNodes extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
    
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType( "text/html" );
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println("<li><a href=\"index.html\">Home</a></li>"+
    			"<li><a href=\"Start\">Start/Stop</a></li>"+
    			"<li><a href=\"Status\">Status</a></li>"+
    			"<li><a href=\"Goods\">Goods</a></li>"+
    			"<li><a id=\"activelink\" href=\"#\">BlindNodes</a></li>"+
    			"<li><a href=\"RefNodes\">RefNodes</a></li>"+
    			"<li><a href=\"Rooms\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"#\" class=\"current\">BlindNodes</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>BlindNodes List</h1>");
    	if(PacketDecoder.isSystemStarted()) {
    		out.println("<h2>List of all BlindNodes</h2>");
    		Collection<BlindNode> blindNodes=PacketDecoder.getAllBlindNodes().values();
    		Iterator<BlindNode> itr=blindNodes.iterator();
    		out.println("<table border=\"1\" summary=\"Blind Nodes\">");
    		out.println("<tr><td><b>Name</b></td><td><b>IEEEaddress</b></td><td><b>NTWaddr</b></td><td><b>Location</b></td><td><b>Position</b></td><td><b>TimeOut</b></td><td><b>Cycle</b></td><td><b>Vbatt</b></td><td><b>Edit</b></td></tr>");
    		while(itr.hasNext()) {
    			BlindNode blind=itr.next();
    			int NTWaddr=blind.getNTWaddr();
    			out.println("<td>"+blind.getName()+"</td>"+
    				"<td>"+Converter.Vector2HexStringNoSpaces(blind.getIEEEaddr())+"</td>"+
    				"<td>"+NTWaddr+"</td>"+
    				"<td>"+PacketDecoder.getRoomName(blind.getRoomId())+"</td>"+
    				"<td>"+String.format("%.2f",blind.getXpos())+" ; "+String.format("%.2f",blind.getYpos())+"</td>"+
    				"<td>"+blind.getTimeOut()+"</td>"+
    				"<td>"+blind.getCycle()+"</td>"+
    				"<td>"+String.format("%.2f",blind.getVbatt())+"</td>"+
    				"<td><form action=\"BlindNode\" method=\"get\">"+
    				"<input type=\"hidden\" name=\"selected\" value=\""+NTWaddr+"\">"+
    				"<input type=\"submit\" value=\"Details\"></form></td></tr>");
    		}
    		out.println("</table><h2>Operations on all blind nodes</h2>"+
    				/*"<h5>Set auto Tx RSSI time on all blind nodes</h5>"+
    				"<form action=\"RequestAllAutoRSSI\" method=\"get\">"+
    				"Auto Tx RSSI time (sec):<input name=\"autoTxRSSItime\" size=\"6\" value=\"2\" />"+
    				"<input type=\"submit\" value=\"Set Auto Tx RSSI Time\"></form>"+
    				"<h5>Set auto Tx Sens time on all blind nodes</h5>"+*/
    				"<h5>Request diagnostic for all nodes</h5>"+
    				"<form action=\"RequestAllDiagnostic\" method=\"get\">"+
    				"<input type=\"hidden\" name=\"isRef\" value=\"0\">"+
    				"<input type=\"submit\" value=\"Get all diagnostic\"></form>");
    	} else out.println("<h3>System is not working!</h3>");
    	out.println(Html.footer);
    }
}

