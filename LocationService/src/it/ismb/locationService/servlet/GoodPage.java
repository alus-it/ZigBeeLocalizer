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
import it.ismb.locationService.Nodes.BlindNode;
import it.ismb.locationService.Nodes.Good;
import it.ismb.locationService.Nodes.Sensor;

import java.io.IOException;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class GoodPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
       
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	HttpSession sessione=req.getSession();
    	String longAddr=req.getParameter("selected");
    	sessione.setAttribute("GoodIEEE",longAddr);
    	Good g=PacketDecoder.getGood(longAddr);
    	out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"#\" class=\"current\">Edit good</a>"+
    		    	"</div><div id=\"content\">");
    	out.println("<h1>Edit good</h1>");
    	out.println("<form action=\"Good\" method=\"post\">" );
    	out.println("<table><tr><td>IEEEaddr:</td><td>"+longAddr+"</td></tr>");
    	out.println("<tr><td>Name:</td><td><input name=\"name\" size=\"20\" value=\""+g.getName()+"\" /></td></tr>");
    	int currentType=g.getTypeId();
    	out.println("<tr><td>Type:</td><td><select name=\"type\" size=\"1\">");    		
    	for(Entry<Integer,String> type:Good.getTypes().entrySet()) if(type.getKey()!=currentType)
    		out.println("<option value=\""+type.getKey()+"\">"+type.getValue()+"</option>");
    		else out.println("<option value=\""+type.getKey()+"\" selected=\"selected\">"+type.getValue()+"</option>");
    	out.println("</select><td/><tr/>");
    	if(g.isAssociated()) {
        	BlindNode bn=g.getAssocitedBlindNode();
        	out.println("<tr><td>Associated Node:</td><td><select name=\"blind\" size=\"1\">");
        	out.println("<option value=\"0\">Not associated</option>");
        	for(BlindNode n:PacketDecoder.getAllBlindNodes().values()) if(!n.hasGood() || n.getNTWaddr()==bn.getNTWaddr()) 
        		if(n.getNTWaddr()!=bn.getNTWaddr())
                	out.println("<option value=\""+n.getNTWaddr()+"\">"+n.getName()+"</option>");
                else out.println("<option value=\""+n.getNTWaddr()+"\" selected=\"selected\">"+n.getName()+"</option>");
        	out.println("</select><td/><tr/>");
        	out.println("<tr><td>Room:</td><td>"+bn.getRoom().getName()+"</td></tr>");
        	out.println("<tr><td>X pos:</td><td>"+String.format("%.2f",bn.getXpos())+"</td></tr>");
        	out.println("<tr><td>Y pos:</td><td>"+String.format("%.2f",bn.getYpos())+"</td></tr>");
        	out.println("<tr><td>Num of sensors:</td><td>"+bn.getNumOfSens()+"</td></tr>");
    		for(Sensor s:bn.getSensors().values())
    			out.println("<tr><td>"+s.getSensorName()+":</td><td>"+String.format("%.2f",s.getValue())+" "+s.getSensorUnitHTML()+"</td></tr>");
    		out.println("<tr><td>Received last packet at:</td><td>"+Converter.TimeMillisToString(bn.getTimestamp())+"</td></tr>");
    	} else {
    		out.println("<tr><td>Associated Node:</td><td><select name=\"blind\" size=\"1\">");
        	out.println("<option value=\"0\" selected=\"selected\">Not associated</option>");
        	for(BlindNode n:PacketDecoder.getAllBlindNodes().values()) if(!n.hasGood())
            	out.println("<option value=\""+n.getNTWaddr()+"\">"+n.getName()+"</option>");
        	out.println("</select><td/><tr/>");
    	}	
    	out.println("</table><input type=\"submit\" value=\"Edit good\"></form><br/>");
    	out.println("<h1>Delete good</h1>");
		if(PacketDecoder.isSystemStarted()) {
			out.println("<form action=\"DeleteGood\" method=\"get\">");
			out.println("<input type=\"submit\" value=\"Delete good\"" +
			"onClick=\"return confirm('Are you sure? Please confirm.');\" ></form>");
		} else out.println("<h3>ERROR: PacketDecoder is not active!</h3>");
    	out.println(Html.footer);
    	
    }
    
	public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"#\" class=\"current\">Edit good</a>"+
    		    	"</div><div id=\"content\">");
    	if(PacketDecoder.isSystemStarted()) {
    		out.println("<h1>Updating good...</h1>");
    		String longAddr=(String) sessione.getAttribute("GoodIEEE");
    		String name=req.getParameter("name");
    		int type=Integer.parseInt(req.getParameter("type"));
    		int NTWassociated=Integer.parseInt(req.getParameter("blind"));
    		switch(PacketDecoder.updateGood(longAddr,name,type,NTWassociated)) {
    			case 1: out.println("<h3>Good succesfully updated!</h3>"); break;
    			case 0: out.println("<h3>ERROR: Good not found!</h3>"); break;
    			case -1: out.println("<h3>ERROR: There is another good with the same name!</h3>"); break;
    			default: break;
    		}
		} else out.println("<h3>ERROR: PacketDecoder is not active!</h3>");
    	out.println(Html.footer);
    }
}
