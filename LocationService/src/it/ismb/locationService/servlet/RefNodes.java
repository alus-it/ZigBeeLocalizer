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
import it.ismb.locationService.Nodes.Node;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RefNodes extends HttpServlet {
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
    			"<li><a href=\"BlindNodes\">BlindNodes</a></li>"+
    			"<li><a id=\"activelink\" href=\"#\">RefNodes</a></li>"+
    			"<li><a href=\"Rooms\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"#\" class=\"current\">RefNodes</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>RefNodes List</h1>");
    	if(PacketDecoder.isSystemStarted()) {
    		out.println("<h3>List of all RefNodes</h3>");
    		Collection<Node> blindNodes=PacketDecoder.getAllRefNodes().values();
    		if(!blindNodes.isEmpty()) {
    			Iterator<Node> itr=blindNodes.iterator();
    			out.println("<table border=\"1\" summary=\"Ref Nodes\">");
    			out.println("<tr><td><b>Name</b></td><td><b>IEEEaddress</b></td><td><b>NTWaddr</b></td><td><b>Location</b></td><td><b>Position</b></td><td><b>Vbatt</b></td><td><b>Edit</b></td></tr>");
        		while(itr.hasNext()) {
        			Node ref=itr.next();
        			int NTWaddr=ref.getNTWaddr();
    				out.println("<tr><td>"+ref.getName()+"</td>"+
    						"<td>"+Converter.Vector2HexStringNoSpaces(ref.getIEEEaddr())+"</td>"+
    						"<td>"+NTWaddr+"</td>"+
    						"<td>"+PacketDecoder.getRoomName(ref.getRoomId())+"</td>"+
    						"<td>"+String.format("%.2f",ref.getXpos())+" ; "+String.format("%.2f",ref.getYpos())+"</td>"+
    						//"<td>"+ref.getAutoTxSensTime()+"</td>"+
    						"<td>"+String.format("%.2f",ref.getVbatt())+"</td>"+
    						"<td><form action=\"RefNode\" method=\"get\">"+
    	    				"<input type=\"hidden\" name=\"selected\" value=\""+NTWaddr+"\">"+
    	    				"<input type=\"submit\" value=\"Details\"></form></td></tr>");
    			}
    			out.println("</table><h2>Operations on all reference nodes</h2>"+
        				/*"<h5>Set auto Tx Sens time on all reference nodes</h5>"+
        				"<form action=\"RequestAllAutoSens\" method=\"get\">"+
        				"Auto Tx Sens time (sec):<input name=\"autoTxSenstime\" size=\"6\" value=\"120\" />"+
        				"<input type=\"hidden\" name=\"isRef\" value=\"1\">"+
        				"<input type=\"submit\" value=\"Set Auto Tx Sens Time\"></form>"+*/
        				"<h5>Request diagnostic for all reference nodes</h5>"+
        				"<form action=\"RequestAllDiagnostic\" method=\"get\">"+
        				"<input type=\"hidden\" name=\"isRef\" value=\"1\">"+
        				"<input type=\"submit\" value=\"Get all diagnostic\"></form>");
    		} else out.println("<h3>No Reference nodes found!</h3>");
    	} else out.println("<h3>System is not working!</h3>");
    	out.println(Html.footer);
    }
}
