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

package it.ismb.locationService;

public class Tester {

	public static void main(String[] args) {
		int n=3; //numero dei punti di rifermento
		double x[]= new double[n];
		double y[]= new double[n];
		double d[]= new double[n];
		double sigma2[]= new double[n];
		
		double px=10; //coordinate punto reale
		double py=19.32;
	
		x[0]=0;
		y[0]=0;
		
		x[1]=20;
		y[1]=0;
		
		x[2]=10;
		y[2]=17.32;
			
		d[0]=21.763230510272038;
		d[1]=21.763230510272038;
		d[2]=1.401068272464261;
		
		sigma2[0]=0.01;
		sigma2[1]=0.01;
		sigma2[2]=2;
		
//		double px=7; //coordinate punto reale
//		double py=6;
//		
//		x[0]=1;
//		y[0]=1;
//		
//		x[1]=13;
//		y[1]=1;
//		
//		x[5]=1;
//		y[5]=11;
//		
//		x[3]=13;
//		y[3]=11;
//		
//		x[4]=7;
//		y[4]=1;
//		
//		x[2]=7;
//		y[2]=11;
//		
//		d[0]=6.61;
//		d[1]=9.01;
//		d[5]=6.61;
//		d[3]=9.01;
//		d[4]=4.4;
//		d[2]=5.6;
//		
//		sigma2[0]=0.16;
//		sigma2[1]=0.16;
//		sigma2[5]=0.16;
//		sigma2[3]=0.16;
//		sigma2[4]=0.04;
//		sigma2[2]=0.04;
		
		
		double stima[]=Multilaterator.multilaterate(x,y,d);
		System.out.println("Stima: ("+stima[0]+","+stima[1]+") diff:"+Multilaterator.distance(stima[0],stima[1],px,py));
		double delta[]=Multilaterator.correct(stima[0],stima[1],x,y,d,sigma2);
		System.out.println("Delta: ("+delta[0]+","+delta[1]+")");
		stima[0]=stima[0]+delta[0];
		stima[1]=stima[1]+delta[1];
		System.out.println("Nuova stima: ("+stima[0]+","+stima[1]+") diff:"+Multilaterator.distance(stima[0],stima[1],px,py));
	}

}
