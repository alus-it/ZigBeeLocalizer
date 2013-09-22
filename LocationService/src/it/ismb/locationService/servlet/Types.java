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

import it.ismb.locationService.PacketDecoderMain;
import it.ismb.locationService.db.DBmanager;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Types extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
    
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType( "text/html" );
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"#\" class=\"current\">Types</a>"+
    			"</div><div id=\"content\">");
    	out.println("<h1>Types List</h1>");
    	if(PacketDecoderMain.isDBmanagerStarted() && PacketDecoderMain.isPacketDecoderStarted()) {
    		List<String> types=DBmanager.getTypes();
    		if(!types.isEmpty()) {
    			Iterator<String> itr=types.iterator();
    			out.println("<form action=\"Type\" method=\"post\">" );
    			out.println("<br/><table border=\"1\" summary=\"Types\">");
    			out.println("<tr><td><b>Select</b></td><td><b>Type</b></td></tr>");
    			boolean first=true;
    			while(itr.hasNext()) {
    				String type=itr.next();
    				if(!first) out.println("<tr><td><input name=\"selected\" type=\"radio\" value=\""+
    						type+"\" /></td><td>"+type+"</td></tr>");
    				else {
    					out.println("<tr><td><input name=\"selected\" type=\"radio\" value=\""+
        						type+"\" checked=\"checked\"/></td><td>"+type+"</td></tr>");
    					first=false;
    				}
    			}
    			out.println("</table><br/><table><tr><td rowspan=\"2\">Action:</td><td>"+
    					"<input name=\"action\" type=\"radio\" checked=\"checked\" value=\"1\" />"+
    					"Rename selected type</td><td rowspan=\"2\">"+
    					"<input type=\"submit\" value=\"Perform action\" /></td></tr><tr><td>"+
    					"<input name=\"action\" type=\"radio\" value=\"2\" />Delete selected type"+
    					"</td></tr></table></form><br/><form action=\"NewType\" method=\"get\">"+
    					"<input type=\"submit\" value=\"Insert new type\" / ></form>");
    		} else out.println("<h3>No types found!</h3>");
    	} else out.println("<h3>System is not working!</h3>");
    	out.println(Html.footer);
    }
}