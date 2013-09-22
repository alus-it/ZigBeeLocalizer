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
import javax.servlet.http.HttpSession;

public class Type extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public void init( ServletConfig config ) throws ServletException{
		super.init( config );
	}
       
    public void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	HttpSession sessione=req.getSession();
    	String type=req.getParameter("selected");
    	int action=Integer.valueOf(req.getParameter("action")).intValue();
    	sessione.setAttribute("action",action);
    	if(action==1) { //update type
    		out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"Types\">Types</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Rename type</a>"+
    		    	"</div><div id=\"content\">");
    		out.println("<h1>Rename type</h1>");
    		sessione.setAttribute("updatingType",type);
    		out.println("<form action=\"Type\" method=\"get\">" );
    		out.println("<table><tr><td>Type name:</td><td><input name=\"name\" size=\"20\" value=\""+type+"\" /></td></tr></table>");
    		out.println("<input type=\"submit\" value=\"Update type name\"></form>");
    		out.println(Html.footer);
    	} else { //delete type
    		out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"Types\">Types</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Delete type</a>"+
    		    	"</div><div id=\"content\">");
    		out.println("<h1>Delete type</h1>");
    		sessione.setAttribute("deletingType",type);
    		out.println("<form action=\"Type\" method=\"get\">");
    		out.println("<table><tr><td>Type name:</td><td>"+type+"</td></tr></table>");
    		int numOfGoods=DBmanager.countGoodsOfType(type);
    		sessione.setAttribute("numOfGoods",numOfGoods);
    		if(numOfGoods>0) { //se ci sono beni da aggiornare
    			out.println("<p>There are "+numOfGoods+" goods of this type, please assign them another type.<p>");
    			out.println("Type: <select name=\"newType\" size=\"1\">");
    			List<String> types=DBmanager.getTypes();
    			Iterator<String> itr=types.iterator();
    			while(itr.hasNext()) {
    				String currentType=itr.next();
    				if(currentType.compareTo(type)!=0) //escludo il tipo che voglio cancellare
    					out.println("<option value=\""+currentType+"\">"+currentType+"</option>");
    			}
    			out.println("</select>");
    		}
    		out.println("<input type=\"submit\" value=\"Delete type\"></form>");
    	}
    }
    
	public void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
    	res.setContentType("text/html");
    	ServletOutputStream out = res.getOutputStream();
    	HttpSession sessione=req.getSession();
    	int action=(Integer)sessione.getAttribute("action");
    	out.println(Html.header);
    	out.println(Html.defaultMenu);
    	if(action==1) { //rinomina tipo
    		out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"Types\">Types</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Rename type</a>"+
    		    	"</div><div id=\"content\">");
    		out.println("<h1>Renaming type...</h1>");
    		String type=(String)sessione.getAttribute("updatingType");
    		String name=req.getParameter("name");
    		if(type.compareTo(name)!=0)
    			if(!DBmanager.existsType(name)) {
    				DBmanager.updateTypeName(type,name);
    				//PacketDecoder.////
    			}
    			else out.println("<h3>ERROR: There is another type with the same name!</h3>");
    		else out.println("<h3>WARNING: Type currently has the same name!</h3>");
    	} else { //cancella tipo
    		out.println("&gt;<a href=\"Goods\">Goods</a>&gt;<a href=\"Types\">Types</a>"+
    		    	"&gt;<a href=\"#\" class=\"current\">Delete type</a>"+
    		    	"</div><div id=\"content\">");
    		out.println("<h1>Deleting type...</h1>");
    		String type=(String)sessione.getAttribute("deletingType");
    		int numOfGoods=(Integer)sessione.getAttribute("numOfGoods");
    		if(numOfGoods>0) {
    			String newType=req.getParameter("newType");
    			DBmanager.changeGoodsType(type,newType);
    		}
    		DBmanager.deleteType(type);
    		out.println("<h3>Type deleted.</h3>");
    	}
    	out.println(Html.footer);
    }
}
