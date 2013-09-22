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

import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.Room;
import it.ismb.locationService.Nodes.Node;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class RoomPage extends HttpServlet {
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
    	Room r=null;
    	int roomID=0;
    	if(req.getParameterMap().containsKey("selected")) {
    		roomID=Integer.valueOf(req.getParameter("selected")).intValue();
    		r=PacketDecoder.getRoomById(roomID);
    		sessione.setAttribute("updatingRoom",r);
    	} else {
    		r=(Room)sessione.getAttribute("updatingRoom");
    		roomID=r.getId();
    	}
    	if(r!=null) {
    		out.println("&gt;<a href=\"Rooms\">Rooms</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Edit room</a>"+
    		    	"</div><div id=\"content\">");
    		out.println("<h1>Edit room</h1>");
    		sessione.setAttribute("updatingRoom",r);
    		out.println("<form action=\"Room\" method=\"post\">" );
    		out.println("<table><tr><td>RoomID:</td><td>"+roomID+"</td></tr>");
    		out.println("<tr><td>Name:</td><td><input name=\"name\" size=\"20\" value=\""+r.getName()+"\" /></td></tr>");
    		out.println("<tr><td>Max distance:</td><td><input name=\"dmax\" size=\"6\" value=\""+r.getMaxDist()+"\" /></td></tr>");
    		out.println("<tr><td>A value:</td><td><input name=\"A\" size=\"6\" value=\""+r.getA()+"\" /></td></tr>");
    		out.println("<tr><td>n value:</td><td><input name=\"n\" size=\"6\" value=\""+r.getN()+"\" /></td></tr>");
    		out.println("</table><input type=\"submit\" value=\"Update room\"></form>");
    		if(roomID!=0) {
    			out.println("<h2>Delete room</h2>");
    			if(PacketDecoder.isSystemStarted()) {
    				sessione.setAttribute("deletingRoomID",roomID);
    				int numOfNodesInRoom=0;
    				out.println("<form action=\"DeleteRoom\" method=\"get\">");
    				Collection<Node> refNodes=PacketDecoder.getAllRefNodes().values();
    				Iterator<Node> itr=refNodes.iterator();
    				while(itr.hasNext()) if(itr.next().getRoomId()==roomID) numOfNodesInRoom++;
    				sessione.setAttribute("numOfNodesInRoom",numOfNodesInRoom);
    				if(numOfNodesInRoom>0) { //se ci sono refNodes da aggiornare
    					out.println("<p>There are "+numOfNodesInRoom+" reference nodes in this room, please assign them to another room.<p>");
    					out.println("Room: <select name=\"newRoom\" size=\"1\">");
    					Collection<Room> rooms=PacketDecoder.getAllRooms().values();
    					Iterator<Room> itr2=rooms.iterator();
    					Room room;
    					while(itr2.hasNext()) {
    						room=itr2.next();
    						if(room.getId()!=roomID) //escludo la stanza che voglio cancellare
    							out.println("<option value=\""+room.getId()+"\">"+room.getName()+"</option>");
    					}
    					out.println("</select>");
    				}
    				out.println("<input type=\"submit\" value=\"Delete room\"" +
    				"onClick=\"return confirm('Are you sure? Please confirm.');\" ></form>");
    			} else out.println("<h3>ERROR: PacketDecoder is not active!</h3>"+
    			"<p>Can't check if there are any reference node in the deleting room.</p>");
    		} else out.println("<h3>Info:</h3>"+
    		"<p>This is the default room for new nodes so can't be deleted.</p>");
    	}  else out.println("<h3>Error: requested node not found!</h3>");
    	out.println(Html.footer);
    }
    
	public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	out.println("&gt;<a href=\"Rooms\">Rooms</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Update room</a>"+
    		    	"</div><div id=\"content\">");
    	out.println("<h1>Updating room...</h1>");
    	Room r=(Room)sessione.getAttribute("updatingRoom");
    	String name=req.getParameter("name");
    	double dmax=Double.parseDouble(req.getParameter("dmax"));
    	double A=Double.parseDouble(req.getParameter("A"));
    	double n=Double.parseDouble(req.getParameter("n"));
    	switch(PacketDecoder.updateRoom(r.getId(),name,dmax,A,n)) {
    		case 1: out.println("<h3>Room succesfully updated!</h3>"); break;
    		case 0: out.println("<h3>ERROR: Room not found!</h3>"); break;
    		case -1: out.println("<h3>ERROR: There is another room with the same name!</h3>"); break;
    		default: break;
    	}
    	out.println(Html.footer);
    }
}
