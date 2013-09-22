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

import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.PacketDecoderMain;
import it.ismb.locationService.Room;

import java.io.IOException;
import java.util.Collection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Rooms extends HttpServlet {
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
    			"<li><a href=\"RefNodes\">RefNodes</a></li>"+
    			"<li><a id=\"activelink\" href=\"#\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"#\" class=\"current\">Rooms</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>Rooms List</h1>");
    	if(PacketDecoderMain.isDBmanagerStarted() && PacketDecoderMain.isPacketDecoderStarted()) {
    		Collection<Room> rooms=PacketDecoder.getAllRooms().values();
    		if(!rooms.isEmpty()) {
    			out.println("<br/><table border=\"1\" summary=\"Rooms\">");
    			out.println("<tr><td><b>RoomID</b></td><td><b>Name</b></td><td><b>dist Max</b></td>"+
    					"<td><b>A</b></td><td><b>n</b></td><td><b>Edit</b></td></tr>");
    			for(Room r:rooms) {
    				out.println("<tr><td>"+r.getId()+"</td><td>"+r.getName()+"</td>"+
    						"<td>"+r.getMaxDist()+"</td><td>"+r.getA()+"</td><td>"+r.getN()+"</td>"+
    						"<td><form action=\"Room\" method=\"get\">"+
    	    				"<input type=\"hidden\" name=\"selected\" value=\""+r.getId()+"\">"+
    	    				"<input type=\"submit\" value=\"Details\"></form></td></tr>");
    			}
    			out.println("</table><br/><form action=\"NewRoom\" method=\"get\">"+
    					"<input type=\"submit\" value=\"Insert new Room\" / ></form>");
    		} else out.println("<h3>No rooms found!</h3>");
    	} else out.println("<h3>System is not working!</h3>");
    	out.println(Html.footer);
    }
}