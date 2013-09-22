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
import it.ismb.locationService.Nodes.Node;
import it.ismb.locationService.db.DBmanager;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class HistoryLocations extends HttpServlet {
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
		out.println("&gt;<a href=\"#\" class=\"current\">LocationsHistory</a>"+
		"</div><div id=\"content\">");
		out.println("<h1>Locations history</h1>");
		Node n=(Node)sessione.getAttribute("updatingNode");
		if(DBmanager.countLocalization(n.getIEEEaddr())>0) {
			long firstTimeMillis=DBmanager.getFirstLocalizationTime(n.getIEEEaddr());
			Calendar firstTime=Converter.TimeMillisToCalendar(firstTimeMillis);
			long lastTimeMillis=DBmanager.getLastLocalizationTime(n.getIEEEaddr());
			Calendar lastTime=Converter.TimeMillisToCalendar(lastTimeMillis);
			out.println("<b>Foud history from: "+Converter.TimeCalendarToString(firstTime)+
					" to: "+Converter.TimeCalendarToString(lastTime)+"</b><br/>");
			out.println("<form action=\"HistoryLocations\" method=\"post\">");
			out.println("<p>Select a time interval:</p><table><tr><td>From:</td><td>");
			out.println("<select name=\"day1\" size=\"1\" >");
			for(int i=1;i<=31;i++) if(i==firstTime.get(Calendar.DAY_OF_MONTH))
				out.println("<option selected=\"selected\" value=\""+i+"\">"+i+"</option>");
			else out.println("<option value=\""+i+"\">"+i+"</option>");
			out.println("</select>/<select name=\"month1\" size=\"1\">");
			for(int i=1;i<=12;i++) if(i==firstTime.get(Calendar.MONTH)+1)
				out.println("<option selected=\"selected\" value=\""+i+"\">"+i+"</option>");
			else out.println("<option value=\""+i+"\">"+i+"</option>");
			out.println("</select>/<input name=\"year1\" size=\"4\" maxlength=\"4\" value=\""+
					firstTime.get(Calendar.YEAR)+"\" />");
			out.println(" <input name=\"hour1\" size=\"2\" maxlength=\"2\" value=\""+
					firstTime.get(Calendar.HOUR_OF_DAY)+"\" />");
			out.println(":<input name=\"min1\" size=\"2\" maxlength=\"2\" value=\""+
					firstTime.get(Calendar.MINUTE)+"\" />");
			out.println(":<input name=\"sec1\" size=\"2\" maxlength=\"2\" value=\""+
					firstTime.get(Calendar.SECOND)+"\" />");
			out.println(".<input name=\"millis1\" size=\"3\" maxlength=\"3\" value=\""+
					firstTime.get(Calendar.MILLISECOND)+"\" /></br></tr>");
			out.println("<tr><td>To:</td><td>");
			out.println("<select name=\"day2\" size=\"1\" >");
			for(int i=1;i<=31;i++) if(i==lastTime.get(Calendar.DAY_OF_MONTH))
				out.println("<option selected=\"selected\" value=\""+i+"\">"+i+"</option>");
			else out.println("<option value=\""+i+"\">"+i+"</option>");
			out.println("</select>/<select name=\"month2\" size=\"1\">");
			for(int i=1;i<=12;i++) if(i==lastTime.get(Calendar.MONTH)+1)
				out.println("<option selected=\"selected\" value=\""+i+"\">"+i+"</option>");
			else out.println("<option value=\""+i+"\">"+i+"</option>");
			out.println("</select>/<input name=\"year2\" size=\"4\" maxlength=\"4\" value=\""+
					lastTime.get(Calendar.YEAR)+"\" />");
			out.println(" <input name=\"hour2\" size=\"2\" maxlength=\"2\" value=\""+
					lastTime.get(Calendar.HOUR_OF_DAY)+"\" />");
			out.println(":<input name=\"min2\" size=\"2\" maxlength=\"2\" value=\""+
					lastTime.get(Calendar.MINUTE)+"\" />");
			out.println(":<input name=\"sec2\" size=\"2\" maxlength=\"2\" value=\""+
					lastTime.get(Calendar.SECOND)+"\" />");
			out.println(".<input name=\"millis2\" size=\"3\" maxlength=\"3\" value=\""+
					lastTime.get(Calendar.MILLISECOND)+"\" /></br></tr></table>");
			out.println("<input type=\"submit\" value=\"List locations\"></form>");
		} else out.println("<h3>No history found in the DB</h3>");
		out.println("<br/><form action=\"BlindNode\" method=\"get\">"+
				"<input type=\"submit\" value=\"Back to node\"></form>"+Html.footer);
	}
    
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"#\" class=\"current\">LocationsHistory</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Locations history</h1>");
    	Node n=(Node)sessione.getAttribute("updatingNode");
    	int day1=Integer.valueOf(req.getParameter("day1")).intValue();
    	int month1=Integer.valueOf(req.getParameter("month1")).intValue();
    	int year1=Integer.valueOf(req.getParameter("year1")).intValue();
    	int hour1=Integer.valueOf(req.getParameter("hour1")).intValue();
    	int min1=Integer.valueOf(req.getParameter("min1")).intValue();
    	int sec1=Integer.valueOf(req.getParameter("sec1")).intValue();
    	int millis1=Integer.valueOf(req.getParameter("millis1")).intValue();
    	int day2=Integer.valueOf(req.getParameter("day2")).intValue();
    	int month2=Integer.valueOf(req.getParameter("month2")).intValue();
    	int year2=Integer.valueOf(req.getParameter("year2")).intValue();
    	int hour2=Integer.valueOf(req.getParameter("hour2")).intValue();
    	int min2=Integer.valueOf(req.getParameter("min2")).intValue();
    	int sec2=Integer.valueOf(req.getParameter("sec2")).intValue();
    	int millis2=Integer.valueOf(req.getParameter("millis2")).intValue();
    	long startTime=Converter.DateTimeToMillis(day1,month1,year1,hour1,min1,sec1,millis1);
    	long stopTime=Converter.DateTimeToMillis(day2,month2,year2,hour2,min2,sec2,millis2);
    	out.println("<h5>Localizations history for blindNode: "+n.getName()+" from: "+
    			Converter.TimeMillisToString(startTime)+" to: "+Converter.TimeMillisToString(stopTime)+"</h5>");
    	out.println("<table border=\"1\"><tr><td><b>Time</b></td><td><b>X</b></td><td><b>Y</b></td><td><b>Room</b></td></tr>");
    	List<Map<String,String>> locs=DBmanager.getAllLocalizationsOf(n.getIEEEaddr(),startTime,stopTime);
    	Iterator<Map<String, String>> itr=locs.iterator();
    	while(itr.hasNext()) {
    		Map<String,String> iRow=(Map<String, String>) itr.next();	    	
    		long timestamp=Long.parseLong(iRow.get("timestamp"));
    		float x=Float.parseFloat(iRow.get("xpos"));
    		float y=Float.parseFloat(iRow.get("ypos"));
    		int room=Integer.parseInt(iRow.get("roomID"));
    		out.println("<tr><td>"+Converter.TimeMillisToString(timestamp)+"</td><td>"+
    				String.format("%.2f",x)+"</td><td>"+String.format("%.2f",y)+"</td><td>"+
    				PacketDecoder.getRoomName(room)+"</td></tr>");
    	}
    	out.println("</table><br/><form action=\"BlindNode\" method=\"get\">"+
				"<input type=\"submit\" value=\"Back to node\"></form>"+Html.footer);
    }

}
