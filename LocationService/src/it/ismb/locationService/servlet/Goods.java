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
import it.ismb.locationService.Nodes.Good;

import java.io.IOException;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Goods extends HttpServlet {
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
    			"<li><a id=\"activelink\" href=\"#\">Goods</a></li>"+
    			"<li><a href=\"BlindNodes\">BlindNodes</a></li>"+
    			"<li><a href=\"RefNodes\">RefNodes</a></li>"+
    			"<li><a href=\"Rooms\">Rooms</a></li>"+
    			"</ul></td></tr></table>"+
    			"</div><div id=\"position\">"+
    			"&gt;&gt;<a href=\"index.html\">LocationService</a>"+
    			"&gt;<a href=\"#\" class=\"current\">Goods</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>Goods List</h1>");
    	if(PacketDecoderMain.isDBmanagerStarted() && PacketDecoderMain.isPacketDecoderStarted()) {
    		out.println("<br/><table border=\"1\" summary=\"Goods\">");
    		out.println("<tr><td><b>Associated</b></td><td><b>Name</b></td><td><b>Type</b></td><td><b>Room"+
    		"</b></td><td><b>X</b></td><td><b>Y</b></td><td><b>IEEEaddr</b></td><td><b>Details</b></td></tr>");
    		for(Entry<String,Good>e:PacketDecoder.getAllGoods().entrySet()) {
    			if(e.getValue().isAssociated()) out.println("<tr><td>Yes</td><td>"+
    					e.getValue().getName()+"</td>"+
            			"<td>"+e.getValue().getType()+"</td>"+
            			"<td>"+PacketDecoder.getRoomName(e.getValue().getAssocitedBlindNode().getRoomId())+"</td>"+
            			"<td>"+String.format("%.2f",e.getValue().getAssocitedBlindNode().getXpos())+"</td>"+
            			"<td>"+String.format("%.2f",e.getValue().getAssocitedBlindNode().getYpos())+"</td>"+
            			"<td>"+e.getKey()+"</td>"+
            			"<td><form action=\"Good\" method=\"get\">"+
            			"<input type=\"hidden\" name=\"selected\" value=\""+e.getKey()+"\">"+
            			"<input type=\"submit\" value=\"Edit\"></form></td></tr>");
    			else out.println("<tr><td>No</td><td>"+
						e.getValue().getName()+"</td>"+
        				"<td>"+e.getValue().getType()+"</td>"+
        				"<td>Unknown</td><td>Unknown</td><td>Unknown</td>"+
        				"<td>"+e.getKey()+"</td>"+
        				"<td><form action=\"Good\" method=\"get\">"+
        				"<input type=\"hidden\" name=\"selected\" value=\""+e.getKey()+"\">"+
        				"<input type=\"submit\" value=\"Edit\"></form></td></tr>");
    		}
    		out.println("</table><br/>");
    		out.println("<table><tr><td><form action=\"NewGood\" method=\"get\">"+
    				"<input type=\"submit\" value=\"Insert new Good\" / ></form></td><td>"+
    				"<form action=\"Types\" method=\"get\">"+
    				"<input type=\"submit\" value=\"Edit types\" /></form></td></tr></table>");
    	} else out.println("<h3>System is not working!</h3>");
    	out.println(Html.footer);
    }
}