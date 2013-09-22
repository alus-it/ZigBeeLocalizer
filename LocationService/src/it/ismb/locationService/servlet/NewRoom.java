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
import it.ismb.locationService.Room;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NewRoom extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
       
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Rooms\">Rooms</a>"+
    	"&gt;<a href=\"#\" class=\"current\">New Room</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Insert new room</h1>");
    	out.println("<form action=\"NewRoom\" method=\"post\"><table>" );
    	out.println("<tr><td>Room name:</td><td><input name=\"newRoomName\" size=\"20\" value=\"\" /></td></tr>");
    	out.println("<tr><td>Max distance:</td><td><input name=\"newRoomDmax\" size=\"6\" value=\"10\" /></td></tr>");
    	out.println("<tr><td>Parameter A:</td><td><input name=\"newRoomAparam\" size=\"6\" value=\""+Room.DEFAULT_A_PARAM+"\" /></td></tr>");
    	out.println("<tr><td>Parameter n:</td><td><input name=\"newRoomNparam\" size=\"6\" value=\""+Room.DEFAULT_N_PARAM+"\" /></td></tr>");
    	out.println("<tr><td><td/><td><input type=\"submit\" value=\"Insert room\"></td></table></form>");
    	out.println(Html.footer);
    }
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Rooms\">Rooms</a>"+
    	"&gt;<a href=\"#\" class=\"current\">New room</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Inserting new room...</h1>");
    	String roomName=req.getParameter("newRoomName");
    	double paramA=Double.parseDouble(req.getParameter("newRoomAparam"));
    	double paramN=Double.parseDouble(req.getParameter("newRoomNparam"));
		double dmax=Double.parseDouble(req.getParameter("newRoomDmax"));
		Room roomToInsert=new Room(-1,roomName,dmax,paramN,paramA);
    	if(PacketDecoder.insertRoom(roomToInsert)!=-1)
    		out.println("<h3>New room inserted succesfully.</h3>");
    	else out.println("<h3>ERROR: There is another room with the same name!</h3>");
    	out.println(Html.footer);
    }
}