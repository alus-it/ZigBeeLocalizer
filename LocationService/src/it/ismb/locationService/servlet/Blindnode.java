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
//import java.util.Iterator;
//import java.util.Set;
//import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public class Blindnode extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
       
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("><a href=\"BlindNodes\">BlindNodes</a>"+
    	"><a href=\"#\" class=\"current\">EditBlindNode</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>BlindNode</h1>");
    	HttpSession sessione=req.getSession();
    	int NTWaddr=1;
    	BlindNode blindNode=null;
    	if(req.getParameterMap().containsKey("selected")) {
    		NTWaddr=Integer.valueOf(req.getParameter("selected")).intValue();
    		blindNode=PacketDecoder.getBlindNode(NTWaddr);
    		sessione.setAttribute("updatingNode",blindNode);
    	} else {
    		blindNode=(BlindNode)sessione.getAttribute("updatingNode");
    		NTWaddr=blindNode.getNTWaddr();
    	}
    	if(blindNode!=null) {
    		out.println("<form action=\"BlindNode\" method=\"post\">" );
    		out.println("<table><tr><td>Name:</td><td><input name=\"name\" size=\"20\" value=\""+blindNode.getName()+
    				"\" /></td><td rowspan=\"5\" valign=\"bottom\"><input type=\"submit\" value=\"Update BlindNode\"></td></tr>");
    		out.println("<tr><td>IEEE address:</td><td>"+Converter.Vector2HexStringNoSpaces(blindNode.getIEEEaddr())+"</td></tr>");
    		out.println("<tr><td>Short address:</td><td>"+blindNode.getNTWaddr()+"</td></tr>");
    		out.println("<tr><td>Auto mode:</td><td><input name=\"AutoMode\" type=\"radio\" ");
    		if(!blindNode.isAutoMode()) out.print("checked=\"checked\" ");
    		out.println("value=\"0\" />Polled - <input name=\"AutoMode\" type=\"radio\" ");
    		if(blindNode.isAutoMode()) out.print("checked=\"checked\" ");
    		out.println("value=\"1\" />Automatic</td></tr>");
    		out.println("<tr><td>Localization algorithm:</td><td><input name=\"DistrLoc\" type=\"radio\" ");
    		if(!blindNode.isDistribuitedLoc()) out.print("checked=\"checked\" ");
    		out.println("value=\"0\" />Centralized - <input name=\"DistrLoc\" type=\"radio\" ");
    		if(blindNode.isDistribuitedLoc()) out.print("checked=\"checked\" ");
    		out.println("value=\"1\" />Distribuited</td></tr>");
    		out.println("<tr><td>Timeout (sec)</td><td><input name=\"TimeOut\" size=\"6\" value=\""+blindNode.getTimeOut()+"\" /></td></tr>");
    		out.println("<tr><td>Cycle time (sec)</td><td><input name=\"Cycle\" size=\"6\" value=\""+blindNode.getCycle()+"\" /></td></tr>");
    		out.println("</table></form><h2>Current Location</h2><form action=\"HistoryLocations\" method=\"get\"><table>");
    		out.println("<tr><td>Room:</td><td>"+PacketDecoder.getRoomName(blindNode.getRoomId())+"</td>"+
    				"<td rowspan=\"2\" valign=\"bottom\"><input type=\"submit\" value=\"Localization history\"></td></tr>");
    		out.println("<tr><td>Position X,Y:</td><td>"+String.format("%.2f",blindNode.getXpos())+
    				" m  ;  "+String.format("%.2f",blindNode.getYpos())+" m</td></tr></table></form>");
    		out.println(/*"<form action=\"RequestPollRSSI\" method=\"get\"><table>"+
    				"<tr><td>Interval (sec):</td><td><input name=\"deltaTsec\" size=\"4\" value=\"1\" /></td>"+
    				"<td rowspan=\"2\" valign=\"bottom\"><input type=\"submit\" value=\"Request polling RSSI\"></td></tr>"+
    				"<tr><td>Samples:</td><td><input name=\"numPkt\" size=\"4\" value=\"10\" /></td></tr>"+		
					"<table></form>*/"<h2>Sensors</h2><table>");
    		int numsens=blindNode.getNumOfSens();
    		out.println("<tr><td>Sensors on node:</td><td>"+numsens+"</td><td></td></tr>");
    		for(Sensor s:blindNode.getSensors().values())
    			out.println("<tr><td>"+s.getSensorName()+":</td><td>"+String.format("%.2f",s.getValue())+" "+s.getSensorUnitHTML()+
    					"</td><td><form action=\"HistorySensor\" method=\"get\">"+
    				"<input type=\"hidden\" name=\"sensId\" value=\""+s.getId()+"\">"+
    				"<input type=\"submit\" value=\"Values History\"></form></td></tr>");
    		out.println("</table><h2>Diagnostic</h2><form action=\"RequestDiagnostic\" method=\"get\"><table>");
    		out.println("<tr><td>Battery voltage:</td><td>"+String.format("%.2f",blindNode.getVbatt())+
    				" V</td><td rowspan=\"3\" valign=\"bottom\"><input type=\"submit\" value=\"Request diagnostic now\"></td></tr>");
    		out.println("<tr><td>Parent RSSI:</td><td>"+blindNode.getParentRSSI()+" dBm</td></tr>");
    		out.println("<tr><td>Received last packet at:</td><td>"+Converter.TimeMillisToString(blindNode.getTimestamp())+"</td></tr></table>");
    		out.println("</table></form>");
    		out.println("<h2>Delete node</h2><form action=\"DeleteNode\" method=\"get\">"+
        			"<p>Ensure that the node is switched off before deleting.</p>"+
        			"<input type=\"submit\" value=\"Delete node\" "+
        		"onClick=\"return confirm('Are you sure? Please, switch off the node before confirm.');\" /></form><br/>");
    	} else out.println("<h3>Error: requested node not found!</h3>");
    	out.println(Html.footer);
    }
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("><a href=\"BlindNodes\">BlindNodes</a>"+
    	"><a href=\"#\" class=\"current\">UpdateNode</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>UpdateBlindNode</h1>");
    	BlindNode blindNode=(BlindNode)sessione.getAttribute("updatingNode");
    	String name=req.getParameter("name");
    	float timeout=Float.valueOf(req.getParameter("TimeOut")).floatValue();
    	float cycle=Float.valueOf(req.getParameter("Cycle")).floatValue();
    	boolean automode, distribuitedLoc;
    	if(Integer.valueOf(req.getParameter("AutoMode")).intValue()==1) automode=true;
    	else automode=false;
    	if(Integer.valueOf(req.getParameter("DistrLoc")).intValue()==1) distribuitedLoc=true;
    	else distribuitedLoc=false;
    	if(timeout!=blindNode.getTimeOut() || cycle!=blindNode.getCycle() || automode!=blindNode.isAutoMode() || distribuitedLoc!=blindNode.isDistribuitedLoc())
    		PacketDecoder.configureBlindNode(blindNode.getNTWaddr(),automode,distribuitedLoc,timeout,cycle,true);
    	if(blindNode.getName()==null) PacketDecoder.updateNodeName(blindNode.getNTWaddr(),false,name);
    	else if(blindNode.getName().compareTo(name)!=0)
    		PacketDecoder.updateNodeName(blindNode.getNTWaddr(),false,name);
    	out.println("<h3>Update BlindNode done!</h3>");
    	out.println("<br/><form action=\"BlindNode\" method=\"get\">");
    	out.println("<input type=\"submit\" value=\"Back to node\"></form>"+Html.footer);
    }
    
}
