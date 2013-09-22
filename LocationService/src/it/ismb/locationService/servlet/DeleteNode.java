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
import it.ismb.locationService.Nodes.BlindNode;
import it.ismb.locationService.Nodes.Node;
import it.ismb.locationService.db.DBmanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class DeleteNode extends HttpServlet {
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
    	out.println("&gt;<a href=\"#\" class=\"current\">DeleteNode</a>"+
    	"</div><div id=\"content\">");
    	out.println("<h1>Delete node</h1>");
    	Node n=(Node)sessione.getAttribute("updatingNode");
    	int NTWaddr=n.getNTWaddr();
    	boolean isRef=n.isReference();
    	boolean canDelete=true;
    	if(!isRef) {
    		BlindNode bn=(BlindNode)n;
    		if(bn.hasGood()) {
    			canDelete=false;
    			out.println("<p>A good is associated to this blind node, please choose if you prefere to "+
    				"delete also it or if you prefere to associate it to another (if available) "+
    				"blind node.<br/>Associated good:</p>");
    			out.println("<table><tr><td>Name:</td><td>"+bn.getGood().getName()+"</td></tr>"+
    				"<tr><td>Type:</td><td>"+bn.getGood().getType()+"</td></tr></table>");
    			HashMap<Integer,BlindNode> blinds=PacketDecoder.getAllBlindNodes();
    			int i=0;
        		Set<Entry<Integer, BlindNode>> blindNodesSet=blinds.entrySet();
        		Iterator<Entry<Integer, BlindNode>> itr=blindNodesSet.iterator();
        		BlindNode blind;
        		String selector="<select name=\"node\" size=\"1\">";
        		boolean first=true;
        		while(itr.hasNext()) {
        			blind=itr.next().getValue();
        			if(!blind.hasGood() && blind.getNTWaddr()!=NTWaddr) {
        				i++;
        				if(!first) selector+="<option value=\""+blind.getNTWaddr()+"\">"
        				+blind.getName()+"</option>";
        				else {
        					selector+="<option selected=\"selected\" value=\""+blind.getNTWaddr()+"\">"
            				+blind.getName()+"</option>";
        					first=false;
        				}
        			}
        		}
        		if(i>0) {
        			selector+="</select>";
        			out.println("<form action=\"DeleteNodeWithGood\" method=\"get\"><table><tr><td>"+
        				"<input name=\"action\" type=\"radio\" checked=\"checked\" value=\"1\" />"+
        				"Associate to node:"+selector+"</td><td rowspan=\"2\">"+
        				"<input type=\"submit\" value=\"Delete node\" /></td></tr><tr><td>"+
        				"<input name=\"action\" type=\"radio\" value=\"2\" />Delete also associated good"+
        				"</td></tr></table></form>");
        		} else out.println("<p><b>No free blind nodes found to associate to this good.</b></p>"+
        				"<form action=\"DeleteNodeWithGood\" method=\"get\"><table><tr><td>"+
        				"<input name=\"action\" type=\"radio\" disabled=\"disabled\" value=\"1\" />"+
        				"Associate to another node</td><td rowspan=\"2\">"+
        				"<input type=\"submit\" value=\"Delete node\" /></td></tr><tr><td>"+
        				"<input name=\"action\" type=\"radio\" checked=\"checked\" value=\"2\" />"+
        				"Delete also associated good</td></tr></table></form>");
    		}
    	}
    	if(canDelete) {
    		PacketDecoder.deleteNode(NTWaddr,isRef);
    		DBmanager.deleteNode(n.getIEEEaddr(),isRef);
    		out.println("<h3>Node deleted succesfully.</h3>");
    	}
    	out.println(Html.footer);
    }
}
