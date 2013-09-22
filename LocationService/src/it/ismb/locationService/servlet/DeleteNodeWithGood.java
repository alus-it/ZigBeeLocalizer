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

import it.ismb.locationService.Converter;
import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.Nodes.BlindNode;
import it.ismb.locationService.Nodes.Good;
import it.ismb.locationService.db.DBmanager;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public class DeleteNodeWithGood extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
    
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"#\" class=\"current\">DeleteNodeWithGood</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Delete node with good</h1>");
    	BlindNode n=(BlindNode)sessione.getAttribute("updatingNode");
    	int NTWaddr=n.getNTWaddr();
    	int action=Integer.parseInt(req.getParameter("action"));
    	if(action==1) {
    		int newNodeAddr=Integer.parseInt(req.getParameter("node"));
    		Good g=n.getGood();
    		PacketDecoder.deleteGood(Converter.Vector2HexStringNoSpaces(n.getIEEEaddr()));
    		DBmanager.deleteGood(Converter.Vector2HexStringNoSpaces(n.getIEEEaddr()));
    		n.disassociate();
    		BlindNode newNode=PacketDecoder.getBlindNode(newNodeAddr);
    		newNode.associateGood(g);
    		PacketDecoder.insertGood(Converter.Vector2HexStringNoSpaces(newNode.getIEEEaddr()),g);
    		DBmanager.insertGood(Converter.Vector2HexStringNoSpaces(newNode.getIEEEaddr()), g);
    	}
		PacketDecoder.deleteNode(NTWaddr,false);
    	out.println("<p>Node deleted successfully.</p>");
    	out.println(Html.footer);
    }

}
