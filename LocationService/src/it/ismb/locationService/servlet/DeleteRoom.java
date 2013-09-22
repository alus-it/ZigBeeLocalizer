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

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public class DeleteRoom extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Rooms\">Rooms</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Delete room</a>"+
    		    	"</div><div id=\"content\">");
    	out.println("<h1>Deleting room...</h1>");
		int RoomID=(Integer)sessione.getAttribute("deletingRoomID");
		int numOfNodesInRoom=(Integer)sessione.getAttribute("numOfNodesInRoom");
		if(numOfNodesInRoom>0) {
			int newRoom=Integer.valueOf(req.getParameter("newRoom")).intValue();
			PacketDecoder.deleteRoomAndMoveNodes(RoomID, newRoom);
		} else PacketDecoder.deleteRoom(RoomID);
		out.println("<h3>Room deleted.</h3>");
		out.println(Html.footer);
	}

}
