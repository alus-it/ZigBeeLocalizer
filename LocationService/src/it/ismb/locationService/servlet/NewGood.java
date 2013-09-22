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

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class NewGood extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
       
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Goods\">Goods</a>"+
    	"&gt;<a href=\"#\" class=\"current\">New Good</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Insert new good</h1>");
    	out.println("<form action=\"NewGood\" method=\"post\">" );
    	out.println("<p>Good name:</p><input name=\"newGoodName\" size=\"20\" value=\"\" />");
		out.println("<p>Type:</p><select name=\"type\" size=\"1\">");
		for(Entry<Integer,String> type:Good.getTypes().entrySet())
			out.println("<option value=\""+type.getKey()+"\">"+type.getValue()+"</option>");
		out.println("</select><br/>");
		out.println("<p>Select a blindNode to associate to this good:</p>");
		out.println("<br/><table border=\"1\" summary=\"BlindsNodes\">");
		out.println("<tr><td><b>Select</b></td><td><b>Name</b></td><td><b>IEEEaddr</b></td>"+
				"<td><b>NTWaddr</b></td><td><b>Location</b></td><td><b>X</b></td><td><b>Y</b></td></tr>");
		Set<Entry<Integer, BlindNode>> blindNodesSet=PacketDecoder.getAllBlindNodes().entrySet();
		Iterator<Entry<Integer, BlindNode>> itr2=blindNodesSet.iterator();
		boolean first=true;
		BlindNode bn;
		int i=0;
		while(itr2.hasNext()) {
			bn=itr2.next().getValue();
			if(!bn.hasGood()) {
				i++;
				if(!first) out.println("<tr><td><input name=\"selected\" type=\"radio\" value=\"");
				else {
					out.println("<tr><td><input name=\"selected\" type=\"radio\" checked=\"checked\""+
							"value=\"");
					first=false;
				}
				out.println(bn.getNTWaddr()+"\" /></td><td>"+bn.getName()+"</td>"+
						"<td>"+Converter.Vector2HexStringNoSpaces(bn.getIEEEaddr())+"</td>"+
						"<td>"+bn.getNTWaddr()+"</td>"+
						"<td>"+PacketDecoder.getRoomName(bn.getRoomId())+"</td>"+
						"<td>"+bn.getXpos()+"</td><td>"+bn.getYpos()+"</td></tr>");
			}
		}
		out.println("</table>");
		if(i>0) out.println("<input type=\"submit\" value=\"Insert good\"></form>");
		else out.println("<p><b>WARNING: There aren't free blind nodes to associate with a new good.</b></p>");
    	out.println(Html.footer);
    }
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Goods\">Goods</a>"+
    	"&gt;<a href=\"#\" class=\"current\">New good</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Inserting new good...</h1>");
    	String newGoodName=req.getParameter("newGoodName");
    	int NTWaddr=Integer.parseInt(req.getParameter("selected"));
    	int typeId=Integer.parseInt(req.getParameter("type"));
    	Good g=new Good(newGoodName,typeId);
    	BlindNode bn=PacketDecoder.getBlindNode(NTWaddr);
    	bn.associateGood(g);
    	if(PacketDecoder.insertGood(Converter.Vector2HexStringNoSpaces(bn.getIEEEaddr()),g))
    		out.println("<h3>New good inserted succesfully.</h3>");
    	else out.println("<h3>ERROR: There is another good with the same name!</h3>");
    	out.println(Html.footer);
    }
}