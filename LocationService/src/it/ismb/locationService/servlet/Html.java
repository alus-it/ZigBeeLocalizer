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

public class Html {
public static final String header="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"+
"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" dir=\"ltr\" lang=\"en\">"+
"<head><title>Location Service</title>"+
"<meta name=\"generator\" content=\"LocationService\"/>"+
"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>"+
"<meta name=\"author\" content=\"Alberto Realis-Luc\"/>"+
"<meta name=\"keywords\" content=\"LocationService,localization,ZigBee,ISMB\"/>"+
"<meta name=\"description\" content=\"WSN Management\"/>"+
"<meta name=\"Content-Language\" content=\"en\"/>"+
"<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"css/standard.css\" />"+
"<style type=\"text/css\" media=\"print\">div#container{width: auto;}</style>"+
"<link rel=\"shortcut icon\" href=\"images/favicon.ico\" />"+
"<link href=\"rss/news.xml\" rel=\"alternate\" type=\"application/rss+xml\" title=\"LocationService RSS Feed\" />"+
"</head><body><div id=\"container\"><div id=\"navigation\"><table>"+
"<tr><td rowspan=\"2\"><img src=\"images/logo.png\" width=\"102\" height=\"49\" alt=\"ISMBLogo\"/></td>"+
"<td><b>LocationService</b></td></tr><tr><td><ul>";
	
public static final String footer="</div><div id=\"footer\">"+
"<a href=\"mailto:alberto.realisluc@gmail.com\"><em>Alberto Realis-Luc</em></a> "+
"<a href=\"http://www.ismb.it\" title=\"ISMB\">Istituto Superiore Mario Boella</a>&copy; 2008 - "+
"<a href=\"http://validator.w3.org/\" title=\"Valid XHTML 1.0\">"+
"<img src=\"images/button-xhtml.png\" alt=\"Valid XHTML 1.0 Transitional\" height=\"15\" width=\"80\" /></a>"+
"<a href=\"http://jigsaw.w3.org/css-validator/check/referer?profile=css3\" title=\"Valid CSS\">"+
"<img src=\"images/button-css.png\" alt=\"Valid CSS\" height=\"15\" width=\"80\" /></a>"+
"<a href=\"rss/news.xml\" rel=\"alternate\" type=\"application/rss+xml\" title=\"News RSS Feed\">"+
"<img src=\"images/button-rss.png\" alt=\"Recent changes RSS feed\" height=\"15\" width=\"80\" /></a>"+
"</div></div></body></html>";

public static final String defaultMenu="<li><a href=\"index.html\">Home</a></li>"+
"<li><a href=\"Start\">Start/Stop</a></li>"+
"<li><a href=\"Status\">Status</a></li>"+
"<li><a href=\"Goods\">Goods</a></li>"+
"<li><a href=\"BlindNodes\">BlindNodes</a></li>"+
"<li><a href=\"RefNodes\">RefNodes</a></li>"+
"<li><a href=\"Rooms\">Rooms</a></li>"+
"</ul></td></tr></table>"+
"</div><div id=\"position\">"+
"&gt;&gt;<a href=\"index.html\">LocationService</a>";

}
