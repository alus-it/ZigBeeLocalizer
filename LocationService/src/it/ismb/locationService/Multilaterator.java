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

package it.ismb.locationService;
import Jama.Matrix;

public class Multilaterator {

	public static double[] multilaterate(double[] x, double[] y, double[] d) {
		double[] res = new double[3];
		res[0]=0; //x_j
		res[1]=0; //y_j
		res[2]=-10; //residuo del calocolo di multilaterazione
		int n=x.length;
		if(n>=3 && y.length==n && d.length==n) {
			double[][] Avect = new double[n-1][2];
			double[][] bvect = new double[n-1][1];
			double xn2=x[n-1]*x[n-1];
			double yn2=y[n-1]*y[n-1];
			double dn2=d[n-1]*d[n-1];
			for(int i=0;i<n-1;i++) {
				Avect[i][0]=2*(x[i]-x[n-1]);
				Avect[i][1]=2*(y[i]-y[n-1]);
				bvect[i][0]=x[i]*x[i]-xn2+y[i]*y[i]-yn2+dn2-d[i]*d[i];
			}
			Matrix A=new Matrix(Avect); //crazione matrice A (n-1 x 2)
			Matrix b=new Matrix(bvect); //creazione vettore colonna b
			Matrix Atran=A.transpose();
			Matrix m= Atran.times(A).inverse().times(Atran).times(b); //m vettore dei risultati
			res[0]=m.get(0,0);
			res[1]=m.get(1,0);
			double somma=0; //sommatoria per il calcolo del residuo
			for(int i=0;i<n;i++) somma+=Math.sqrt(Math.pow(x[i]-res[0],2)+Math.pow(y[i]-res[1],2))-d[i];
			res[2]=somma/n;
		}
		return res;
	}
	
//	private static void printMatrix(Matrix M) {
//		for(int r=0;r<M.getRowDimension();r++) {
//			System.out.print("[");
//			for(int c=0;c<M.getColumnDimension();c++) System.out.print(M.get(r,c)+",");
//			System.out.println("]");
//		}
//	}
	
	public static double[] correct(double x0, double y0, double[] x, double[] y, double[] d, double[] sigma2) {
		double[] deltaVect = new double[2];
		deltaVect[0]=0; //delta x
		deltaVect[1]=0; //delta y
		int n=x.length;
		if(n>=3 && y.length==n && d.length==n && sigma2.length==n) {
			double[][] A1vect = new double[n][2];
			double[][] lvect = new double[n][1];
			Matrix P=new Matrix(n,n);
			for(int i=0;i<n;i++) {
				double den=Math.pow(-2*d[i]*Math.sqrt(sigma2[i]),2);
				if(den==0) return(deltaVect);
				P.set(i,i,1/den);
				A1vect[i][0]=2*(x0-x[i]);
				A1vect[i][1]=2*(y0-y[i]);
				lvect[i][0]=Math.pow(x[i]-x0,2)+Math.pow(y[i]-y0,2)-Math.pow(d[i],2);
			}
			Matrix A1=new Matrix(A1vect);
			Matrix l=new Matrix(lvect); //creazione vettore colonna l
			Matrix A1tran=A1.transpose();
			Matrix delta=A1tran.times(P).times(A1).inverse().times(A1tran).times(P).times(l);
			delta.set(0,0, 0-delta.get(0,0)); //c'è da cabiare di segno
			delta.set(1,0, 0-delta.get(1,0));
			//calcError(n,A1,l,delta,P); //si prova a calcolare i parametri dell'ellisse d'errore
			deltaVect[0]=delta.get(0,0);
			deltaVect[1]=delta.get(1,0);
		}
		return(deltaVect);
	}
	
	public static void calcError(int n, Matrix A1, Matrix l, Matrix delta, Matrix P) { //ellisse d'errore
		Matrix nu=A1.times(delta).plus(l);		
		double sigma02=nu.transpose().times(P).times(nu).get(0,0)/(n-2);
		//System.out.println("Sigma0="+Math.sqrt(sigma02));
		Matrix N=A1.transpose().times(P).times(A1).inverse();
		//printMatrix(N);
		double sigmax2=sigma02*N.get(0,0);
		//System.out.println("SigmaX="+Math.sqrt(sigmax2));
		double sigmay2=sigma02*N.get(1,1);
		//System.out.println("SigmaY="+Math.sqrt(sigmay2));
		double Cxy=Math.abs(sigma02*N.get(0,1));
		//System.out.println("Cxy="+Cxy);
		double a=Math.abs(Math.sqrt(0.5*(sigmax2+sigmay2)+0.5*Math.sqrt(Math.pow(sigmax2-sigmay2,2)+4*Cxy)));
		double b=Math.abs(Math.sqrt(0.5*(sigmax2+sigmay2)-0.5*Math.sqrt(Math.pow(sigmax2-sigmay2,2)+4*Cxy)));
		double phy=0.5*Math.atan((-2*Cxy)/(sigmay2-sigmax2));
		System.out.println("a="+a+" b="+b+" phy="+phy);
	}
	
	
	public static double distance(double x1, double y1, double x2, double y2) {
		return(Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2,2)));
	}
	
	public static double[] average(double x1, double y1, double d1, double x2, double y2, double d2, double dst) {
		double M[]=new double[2]; //trova il punto medio in proprzione con d1 e d2
		double dstM1,dstM2,rapp;
		if(d1<d2) {
			rapp=d1/d2;
		    dstM1=dst/(rapp+1);
		    dstM2=dst-dstM1;
		} else {
			rapp=d2/d1;
		    dstM2=dst/(rapp+1);
		    dstM1=dst-dstM2;
		}
		if(x1!=x2) { //controllo di non dividere per zero
			double m=(y2-y1)/(x2-x1);
			double alfa=Math.atan(Math.abs(m));
			if(x1<x2) M[0]=Math.cos(alfa)*dstM1+x1;
			else M[0]=Math.cos(alfa)*dstM2+x2;
			if(y1<y2) M[1]=Math.sin(alfa)*dstM1+y1;
			else M[1]=Math.sin(alfa)*dstM2+y2;
		} else { //retta verticale
			M[0]=x1;
			if(y1<y2) M[1]=dstM1+y1;
			else M[1]=dstM2+y2;
		}
		return(M);
	}

}