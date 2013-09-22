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

import it.ismb.locationService.Nodes.*;
import it.ismb.locationService.db.DBmanager;
import it.ismb.locationService.packets.*;
import it.ismb.locationService.uart.UARTmanager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

public class PacketDecoder {
	private static final int NWK_ANNOUNCE_REQUEST	= 0x40;
	public static final int IEEE_ANNOUNCE_REQUEST	= 0x45;
	private static final int CONFIG_REF_NODE		= 0xee;
	private static final int CONFIG_BLIND_NODE		= 0xcc;
	private static final int SENSORS_REQUEST		= 0x33;
	private static final int DIAGNOSTIC_REQUEST		= 0x99;
	private static final int RSSI_REQUEST			= 0x21;
	private static final int XY_REQUEST				= 0x13;
	private static HashMap<Integer, BlindNode> blindNodes;
	private static HashMap<Integer, Node> refNodes;
	private static HashMap<Integer, Room> rooms;
	private static HashMap<String, Good> goods;
	private static int numOfRefs, numOfBlinds, expectedNumOfRefNodes;;
	private static boolean systemStarted;
	private static LinkedList<PacketDescriptor> expectedACKqueue;
	private static final double MAX_RESIDUE=5.0; //residuo massimo ammesso per il calcolo di multilaterazione
	private static final int MAX_SIGMA2=10; //sigma² massimo ammesso per la selezione misure
	private static final double MIN_SIGMA=0.01; //valore di sigma da usare quando è zero x la matrice dei pesi

	public PacketDecoder() {
		systemStarted=false;
		refNodes=new HashMap<Integer, Node>();
		blindNodes=new HashMap<Integer, BlindNode>();
		rooms=new HashMap<Integer,Room>();
		goods=new HashMap<String,Good>();
		expectedACKqueue = new LinkedList<PacketDescriptor>();
		numOfRefs=0;
		numOfBlinds=0;
		expectedNumOfRefNodes=0;
	}
	
	public static void load() { //carica da DB
		DBmanager.loadRooms();
		DBmanager.loadTypes();
		DBmanager.loadGoods();
	}
	
	public static void loadRoom(int roomToLoadId, Room roomToLoad) {
		rooms.put(roomToLoadId,roomToLoad);
	}
	
	public static void loadGood(String IEEEaddr, Good good) {
		goods.put(IEEEaddr,good);
	}
	
	public static int insertRoom(Room newRoom) {
		int i=1; //0 è l'id per indicare nessuna stanza assegnata
		boolean trovato=false;
		if(!rooms.isEmpty()) { //se la lista è vuota non mi faccio problemi
			for(i=1;rooms.containsKey(i) && trovato==false;i++) { //controllo se c'è già uno stesso nome
				if(rooms.get(i).getName().compareTo(newRoom.getName())==0) trovato=true;
			} //i è il primo id libero
		}
		if(!trovato) {
			newRoom.setId(i); //associo l'id alla stanza
			rooms.put(i,newRoom);
			DBmanager.insertRoom(newRoom);
		} else i=-1; //caso in cui non ho inserito perchè c'era già una stanza con lo stesso nome
		return(i);
	}
	
	public static boolean insertGood(String longAddr, Good g) {
		if(goods.containsKey(longAddr)) return false;
		else {
			goods.put(longAddr,g);
			DBmanager.insertGood(longAddr,g);
			return true;
		}
	}
	
	public static void receivedPacket(byte[] received) { //gestisce cosa fare a seconda del pacchetto ricevuto
		if(received==null) return;
		Packet pkt=Packet.buildPacket(received);
		if(pkt==null) return;
		System.out.println("RCV: "+pkt.toString());
		int shortAddr=pkt.getNTWaddr();
		switch(pkt.getPacketType()) { //switch dei pacchetti ricevuti
			case Packet.ANNOUNCE_BLIND_PKT:
				AnnounceBlindPkt blindPkt = (AnnounceBlindPkt)pkt;
				int[] longAddr=blindPkt.getIEEEaddr();
				checkAnnounce(longAddr);
				if(!blindNodes.containsKey(shortAddr)) { //nodo sconosciuto
					boolean trovato=false;
					BlindNode n;
					for(BlindNode bn:blindNodes.values()) //lo cerco per IEEE addr
						if(Converter.compareIEEEaddr(bn.getIEEEaddr(),longAddr)) { //ha cambiato NTWaddr
							n=bn;
							trovato=true;
							blindNodes.remove(n.getNTWaddr());
							n.setNTWaddr(shortAddr);
							blindNodes.put(shortAddr,n);
							break;
						}
					if(!trovato) { // non lo conosco proprio: lo inserisco
						n=new BlindNode(blindPkt);
						blindNodes.put(shortAddr,n);
						DBmanager.insertBlindNode(n);
						numOfBlinds++;
						String longAddrString=n.getIEEEaddrString();
						if(goods.containsKey(longAddrString)) goods.get(longAddrString).associateBlindNode(n);
					}
				} else { //nodo conosciuto si riannuncia: controllo che l'IEEE sia corretto
					int[] IEEEpresent=blindNodes.get(shortAddr).getIEEEaddr();
					if(!Converter.compareIEEEaddr(longAddr,IEEEpresent)) {
						blindNodes.remove(shortAddr);
						requestIEEEannounce(longAddr);
						requestIEEEannounce(IEEEpresent);
					} else requestDiagnostic(shortAddr); //tutto regolare gli chiedo la diagnostica
				}
				break;
			case Packet.ANNOUNCE_REF_PKT:
				AnnounceRefPkt refPkt = (AnnounceRefPkt) pkt;
				longAddr=refPkt.getIEEEaddr();
				checkAnnounce(longAddr);
				if(!refNodes.containsKey(shortAddr)) { //nodo sconosciuto: lo inserisco
					Node n=new Node(refPkt);
					refNodes.put(refPkt.getNTWaddr(),n);
					DBmanager.insertRefNode(n);
					numOfRefs++;
					if(!systemStarted && numOfRefs==expectedNumOfRefNodes) systemStarted=true;
				} else { //nodo conosciuto si riannuncia: controllo che l'IEEE sia corretto
					int[] IEEEpresent=refNodes.get(shortAddr).getIEEEaddr();
					if(!Converter.compareIEEEaddr(longAddr,IEEEpresent)) {
						refNodes.remove(shortAddr);
						requestIEEEannounce(longAddr);
						requestIEEEannounce(IEEEpresent);
					} else requestDiagnostic(shortAddr); //tutto regolare gli chiedo la diagnostica
				}
				
				break;
			case Packet.ACK_CONFIG_REF:
				ACKconfigRefPkt ackConfigRefPkt = (ACKconfigRefPkt) pkt;
				if(!refNodes.containsKey(shortAddr)) { //nodo sconosciuto
					if(systemStarted) requestNwkAnnounce(shortAddr); //gli chiedo di annunciarsi
					System.out.println("WARNING: received ACK update ref position from an unknown node");
				} else { //nodo conosciuto
					checkACK(Packet.ACK_CONFIG_REF,CONFIG_REF_NODE,shortAddr);
					Node updatingNode=refNodes.get(shortAddr); //lo cerco e lo aggiorno
					updatingNode.setPosition(ackConfigRefPkt.getX(),ackConfigRefPkt.getY(),ackConfigRefPkt.getRoomId());
					updatingNode.setAutoMode(ackConfigRefPkt.isAutomode());
					updatingNode.setCycle(ackConfigRefPkt.getCycle());
				}
				break;
			case Packet.ACK_CONFIG_BLIND:
				ACKconfigBlindPkt ackConfigBlindPkt = (ACKconfigBlindPkt) pkt;
				if(!blindNodes.containsKey(shortAddr)) { //se non lo conosco
					if(systemStarted) requestNwkAnnounce(shortAddr); //gli chiedo di annunciarsi
					System.out.println("WARNING: received ACK poll RSSI from an unknown node");
				} else {
					checkACK(Packet.ACK_CONFIG_BLIND,CONFIG_BLIND_NODE,shortAddr);
					BlindNode bn=blindNodes.get(shortAddr);
					bn.setCycle(ackConfigBlindPkt.getCycle());
					bn.setTimeOut(ackConfigBlindPkt.getTimeOut());
					bn.setAutoMode(ackConfigBlindPkt.isAutoMode());
					bn.setDistribuiteLoc(ackConfigBlindPkt.isDistributtedLoc());
				}
				break;
			case Packet.RSSI_PKT:
				if(systemStarted) {
					RSSIpkt RSSIpkt = (RSSIpkt) pkt;
					longAddr=RSSIpkt.getIEEEaddr();
					if(!blindNodes.containsKey(shortAddr)) {
						boolean trovato=false;
						BlindNode n;
						for(BlindNode bn:blindNodes.values()) //lo cerco per IEEE addr
							if(Converter.compareIEEEaddr(bn.getIEEEaddr(),longAddr)) { //ha cambiato NTWaddr
								n=bn;
								trovato=true;
								blindNodes.remove(n.getNTWaddr());
								n.setNTWaddr(shortAddr);
								blindNodes.put(shortAddr,n);
								processRSSIpkt(RSSIpkt);
								n.setSensors(RSSIpkt.getSensPkt());
								DBmanager.updateSens(n);
								break;
							}
						if(!trovato) { // non lo conosco proprio: lo inserisco
							BlindNode bn=DBmanager.getBlindByIEEEfromDB(longAddr);
							if(bn==null) requestIEEEannounce(longAddr); //se non c'è nemmeno nel db
							else { //presente nel db ma non ancora in memoria
								numOfBlinds++;
								if(bn.getNTWaddr()!=shortAddr) { //se necessario
									bn.setNTWaddr(shortAddr); //aggiorno il NTWK address
									DBmanager.updateNWKaddr(longAddr, shortAddr); //anche nel DB
								}
								blindNodes.put(shortAddr,bn);
								processRSSIpkt(RSSIpkt);
								bn.setSensors(RSSIpkt.getSensPkt());
								DBmanager.updateSens(bn);
							}
						}
					} else { //ho già lo shortAddress come entry nei blindNodes
						int[] IEEEpresent=blindNodes.get(shortAddr).getIEEEaddr(); //IEEE già presente
						if(!Converter.compareIEEEaddr(longAddr,IEEEpresent)) { //controllo paranoico
							blindNodes.remove(shortAddr); //se l'indirizzo non torna
							requestIEEEannounce(longAddr); //se dormono non è detto che rispondano
							requestIEEEannounce(IEEEpresent);
						} else { //tutto regolare: lo processo
							BlindNode bn=blindNodes.get(shortAddr);
							checkACK(Packet.RSSI_PKT,RSSI_REQUEST,RSSIpkt.getNTWaddr());
							processRSSIpkt(RSSIpkt);
							bn.setSensors(RSSIpkt.getSensPkt());
							DBmanager.updateSens(bn);
						}
					}
				}
				break;
			case Packet.XY_PKT:
				if(systemStarted) {
					XYpkt xyPkt = (XYpkt) pkt;
					longAddr=xyPkt.getIEEEaddr();
					if(!blindNodes.containsKey(shortAddr)) {
						BlindNode bn=DBmanager.getBlindByIEEEfromDB(longAddr);
						if(bn==null) requestIEEEannounce(longAddr); //se non c'è nemmeno nel db
						else { //presente nel db ma non ancora in memoria
							numOfBlinds++;
							if(bn.getNTWaddr()!=shortAddr) {
								bn.setNTWaddr(shortAddr);
								DBmanager.updateNWKaddr(longAddr, shortAddr);
							}
							bn.setPosition(xyPkt.getX(),xyPkt.getY(),xyPkt.getRoomId());
							blindNodes.put(shortAddr,bn);
							DBmanager.updateBlindPos(longAddr,xyPkt.getX(),xyPkt.getY(),xyPkt.getRoomId(),bn.getTimestamp());
							bn.setSensors(xyPkt.getSensPkt());
							DBmanager.updateSens(bn);
						}
					} else {
						BlindNode updatingNode=blindNodes.get(shortAddr);
						if(!updatingNode.isAutoMode()) checkACK(Packet.XY_PKT,XY_REQUEST,xyPkt.getNTWaddr());
						updatingNode.setPosition(xyPkt.getX(),xyPkt.getY(),xyPkt.getRoomId());
						DBmanager.updateBlindPos(longAddr,xyPkt.getX(),xyPkt.getY(),xyPkt.getRoomId(),updatingNode.getTimestamp());
					}
				}
				break;
			case Packet.SENS_PKT:
				if(!systemStarted) break;
				SensPkt sensPkt = (SensPkt) pkt; //solo i reference node possono mandare sens pkt
				if(!refNodes.containsKey(shortAddr)) { //se è sconosciuto
					if(systemStarted) requestNwkAnnounce(shortAddr); //che si annunci...
					System.out.println("WARNING: received sensor packet from an unknown node");
				} else { //se è conosciuto
					Node updatingNode;
					updatingNode=refNodes.get(shortAddr);
					checkACK(Packet.SENS_PKT,SENSORS_REQUEST,sensPkt.getNTWaddr());
					updatingNode.setSensors(sensPkt);
					DBmanager.updateSens(updatingNode);
				}
				break;
			case Packet.DIAGNOSTIC_PKT:
				DiagnosticPkt diaPkt = (DiagnosticPkt) pkt;
				boolean isref=refNodes.containsKey(shortAddr);
				if(!isref && !blindNodes.containsKey(diaPkt.getNTWaddr())) {
					if(systemStarted) requestNwkAnnounce(shortAddr);
					System.out.println("WARNING: received diagnostic packet from an unknown node");
				} else {
					checkACK(Packet.DIAGNOSTIC_PKT,DIAGNOSTIC_REQUEST,diaPkt.getNTWaddr());
					Node updatingNode;
					if(isref) updatingNode=refNodes.get(shortAddr);
					else updatingNode=blindNodes.get(shortAddr);
					double vbatt=updatingNode.setDiagnostic(diaPkt);
					DBmanager.updateDiagnostic(updatingNode.getIEEEaddr(),vbatt,-diaPkt.getRSSIparent(),updatingNode.getTimestamp());
				}
				break;
		}
	}
	
	public static void checkACK(int receivedACKtype, int sentRequestType, int NTWaddr) { //gestione ACK
		PacketDescriptor expRep;
		boolean found=false;
		if(UARTmanager.isWaiting()) {
//			System.out.println("Ultima richiesta fatta:"+UARTmanager.getLastReqType()+"\n"+
//				"richiesta alla risposta ricevuta:"+sentRequestType+"\n"+
//				"ultimo dest: "+UARTmanager.getLastReqDest()+"\n "+
//				"mittente: "+NTWaddr);
			if(UARTmanager.getLastReqType()==sentRequestType && UARTmanager.getLastReqDest()==NTWaddr)
				UARTmanager.resetTimer();
		}
		for(int i=0;i<expectedACKqueue.size() && found==false;i++) {
			expRep=expectedACKqueue.get(i);
			if(expRep.compare(receivedACKtype,NTWaddr)) {
				found=true;
				expectedACKqueue.remove(i);
			}
		}	
	}
	
	public static void checkAnnounce(int[] IEEEaddr) { //gestione annunci richiesti
		if(UARTmanager.isWaiting()) {
			if(UARTmanager.getLastReqType()==IEEE_ANNOUNCE_REQUEST) {
				int[] lastIEEErequested=UARTmanager.getLastIEEEaddrAnnonunceRequest();
				if(Converter.compareIEEEaddr(IEEEaddr,lastIEEErequested)) UARTmanager.resetTimer();
			}
		}
		PacketDescriptor expRep;
		boolean found=false;
		for(int i=0;i<expectedACKqueue.size() && found==false;i++) {
			expRep=expectedACKqueue.get(i);
			if(expRep.isAnnceWithIEEE(IEEEaddr)) {
				found=true;
				expectedACKqueue.remove(i);
			}
		}	
	}
	
	private static void waitForACK(int requestType, int destinationNTWaddr) {
		PacketDescriptor frameDescr=new PacketDescriptor(requestType,destinationNTWaddr);
		expectedACKqueue.offer(frameDescr);
	}
	
	private static void waitForAnnounce(int destinationIEEEaddr[]) {
		PacketDescriptor frameDescr=new PacketDescriptor(destinationIEEEaddr);
		expectedACKqueue.offer(frameDescr);
	}
	
	private static void processRSSIpkt(RSSIpkt rssiPkt) { //processa un pacchetto RSSI
		int neighbors=rssiPkt.getNBrNum();
		if(neighbors==0) return; //Pacchetto RSSI senza vicini
		int bestAddr=rssiPkt.getNeighbor(0).getNTWaddr(); //il primo è sempre il più vicino
		int ImInRoom=refNodes.get(bestAddr).getRoomId();
		Room roomWhereIm;
		if(!rooms.containsKey(ImInRoom)) return;
		else roomWhereIm=rooms.get(ImInRoom);
		float RSSI;
		double A=-rooms.get(ImInRoom).getA(); //prendo A ed n della stanza in cui mi trovo
		double n=rooms.get(ImInRoom).getN();
		float xRef=(float)refNodes.get(bestAddr).getXpos();
		float yRef=(float)refNodes.get(bestAddr).getYpos();
		BlindNode updatingNode=blindNodes.get(rssiPkt.getNTWaddr());
		double d[]=new double[neighbors];
		double s[]=new double[neighbors];
		double x[]=new double[neighbors];
		double y[]=new double[neighbors];
		int i,count=0;
		double dist;
		for(i=0;i<neighbors;i++) if(rssiPkt.getNeighbor(i).getSigma2()<=MAX_SIGMA2) { //for delle misure
			RSSI=-(rssiPkt.getNeighbor(i).getRssiDownLink()+rssiPkt.getNeighbor(i).getRssiUpLink())/2; //media RSSI
			dist=Math.pow(10,(A-RSSI)/(10*n));
			if(dist<=roomWhereIm.getMaxDist()) { //controllo se è una distanza misurabile in quella stanza
				d[count]=dist;
				if(rssiPkt.getNeighbor(i).getSigma2()==0) s[count]=MIN_SIGMA; //peso forte alle misure buone
				else s[count]=rssiPkt.getNeighbor(i).getSigma2();
				x[count]=refNodes.get(rssiPkt.getNeighbor(i).getNTWaddr()).getXpos();
				y[count]=refNodes.get(rssiPkt.getNeighbor(i).getNTWaddr()).getYpos();
				count++;
			}
		}
		if(count>=3) { //ho almeno tre vicini: multilaterazione
			System.out.println("*******************Multilaterate:");
			for(i=0;i<count;i++)
				System.out.println("Vicino "+i+": pos("+x[i]+","+y[i]+") dist = "+d[i]);
			double m[]=Multilaterator.multilaterate(x,y,d);
			System.out.println("--> Stima coord:("+m[0]+","+m[1]+") - Residuo: "+m[2]);
			if(m[2]<MAX_RESIDUE) { //solo se non ho un residuo folle
				double delta[]=Multilaterator.correct(m[0],m[1],x,y,d,s);
				System.out.println("--> Delta: ("+delta[0]+","+delta[1]+")");
				m[0]=m[0]+delta[0];
				m[1]=m[1]+delta[1];
				System.out.println("*** Stanza: "+rooms.get(ImInRoom).getName()+" - pos:("+m[0]+","+m[1]+")");
				updatingNode.setPosition(m[0],m[1],ImInRoom);
				DBmanager.updateBlindPos(updatingNode.getIEEEaddr(),m[0],m[1],ImInRoom,updatingNode.getTimestamp());
			} else System.out.println("!!! Residuo troppo alto! Misure inconsistenti.");
		} else if(count==2) { //ho due vicini nella stessa stanza, faccio la media tra quei due
			double dst=Multilaterator.distance(x[0],y[0],x[1],y[1]);
			double m[]=Multilaterator.average(x[0],y[0],d[0],x[1],y[1],d[1],dst);
			System.out.println("*?* Stanza: "+rooms.get(ImInRoom).getName()+" - stima coord: ("+m[0]+","+m[1]+")");
			updatingNode.setPosition(m[0],m[1],ImInRoom);
			DBmanager.updateBlindPos(updatingNode.getIEEEaddr(),m[0],m[1],ImInRoom,updatingNode.getTimestamp());
		} else { //Ho solo un vicino o solo uno della stessa stanza gli assegno la sua stessa stanza e coord
			System.out.println("?*? Stanza: "+rooms.get(ImInRoom).getName());
			updatingNode.setPosition(xRef,yRef,ImInRoom);
			DBmanager.updateBlindPos(updatingNode.getIEEEaddr(),xRef,yRef,ImInRoom,updatingNode.getTimestamp());
		}
	}
	
	public static void requestDiagnostic(int NWKaddr) {
		System.out.println("Send: Diagnostic request for NTWaddr: "+NWKaddr);
		byte[] req=new byte[3];
		req[0]=(byte)DIAGNOSTIC_REQUEST;
		byte[] addr=new byte[2];
		addr=Converter.Encode16U(NWKaddr, false);
		req[1]=addr[0];
		req[2]=addr[1];
		waitForACK(Packet.DIAGNOSTIC_PKT,NWKaddr);
		UARTmanager.sendDataToUART(req);
	}
	
	public static void requestRSSI(int NWKaddr) {
		if(blindNodes.containsKey(NWKaddr)) {
			System.out.println("Send: RSSI request for NTWaddr: "+NWKaddr);
			byte[] req=new byte[3];
			req[0]=(byte)RSSI_REQUEST;
			byte[] addr=new byte[2];
			addr=Converter.Encode16U(NWKaddr, false);
			req[1]=addr[0];
			req[2]=addr[1];
			waitForACK(Packet.RSSI_PKT,NWKaddr);
			UARTmanager.sendDataToUART(req);
		}
	}
	
	public static void requestXY(int NWKaddr) {
		if(blindNodes.containsKey(NWKaddr)) {
			System.out.println("Send: XY request for NTWaddr: "+NWKaddr);
			byte[] req=new byte[3];
			req[0]=(byte)XY_REQUEST;
			byte[] addr=new byte[2];
			addr=Converter.Encode16U(NWKaddr, false);
			req[1]=addr[0];
			req[2]=addr[1];
			waitForACK(Packet.XY_PKT,NWKaddr);
			UARTmanager.sendDataToUART(req);
		}
	}
	
	public static void requestSensors(int NWKaddr) {
		if(refNodes.containsKey(NWKaddr)) {
			System.out.println("Send: Sensors request for NTWaddr: "+NWKaddr);
			byte[] req=new byte[3];
			req[0]=(byte)SENSORS_REQUEST;
			byte[] addr=new byte[2];
			addr=Converter.Encode16U(NWKaddr, false);
			req[1]=addr[0];
			req[2]=addr[1];
			waitForACK(Packet.SENS_PKT,NWKaddr);
			UARTmanager.sendDataToUART(req);
		}
	}
	
	public static void requestNwkAnnounce(int NWKaddr) {
		System.out.println("Send: Announce request for NTWaddr: "+NWKaddr);
		byte[] req=new byte[3];
		req[0]=(byte)NWK_ANNOUNCE_REQUEST;
		byte[] addr=new byte[2];
		addr=Converter.Encode16U(NWKaddr, false);
		req[1]=addr[0];
		req[2]=addr[1];
		if(blindNodes.containsKey(NWKaddr)) waitForACK(Packet.ANNOUNCE_BLIND_PKT,NWKaddr);
		else waitForACK(Packet.ANNOUNCE_REF_PKT,NWKaddr);
		UARTmanager.sendDataToUART(req);
	}
	
	public static void requestIEEEannounce(int[] IEEEaddr) {
		System.out.println("Send: Announce request for IEEEaddr: "+Converter.Vector2HexString(IEEEaddr));
		byte[] req=new byte[9];
		req[0]=(byte)IEEE_ANNOUNCE_REQUEST;
		for(int i=0;i<8;i++) req[i+1]=(byte)IEEEaddr[i];
		waitForAnnounce(IEEEaddr);
		UARTmanager.sendDataToUART(req);
	}
	
	public static void configureRefNode(int NTWaddr, double xPos, double yPos, int roomId, boolean automode,
			double sensTime, boolean updateDB) {
		if(rooms.containsKey(roomId) && refNodes.containsKey(NTWaddr)) {
			Room room=rooms.get(roomId);
			System.out.println("Send: configRef NTWaddr:"+NTWaddr+" x:"+xPos+" y:"+yPos+" roomId:"+roomId+
				" A:"+room.getParamA()+" n_index:"+room.getN_index()+" auto:"+automode+" sensCycleTime:"+sensTime);
			if(updateDB) DBmanager.updateRefConfig(NTWaddr, xPos, yPos, roomId, automode, sensTime);
			byte[] pkt=new byte[14];
			pkt[0]=(byte)CONFIG_REF_NODE;
			byte[] twobytes=new byte[2];
			twobytes=Converter.Encode16U(NTWaddr,false);
			pkt[1]=twobytes[0];
			pkt[2]=twobytes[1];
			twobytes=Converter.EncodeDistance(xPos);
			pkt[3]=twobytes[0];
			pkt[4]=twobytes[1];
			twobytes=Converter.EncodeDistance(yPos);
			pkt[5]=twobytes[0];
			pkt[6]=twobytes[1];
			twobytes=Converter.Encode16U(roomId,false);
			pkt[7]=twobytes[0];
			pkt[8]=twobytes[1];
			pkt[9]=(byte)room.getParamA();
			pkt[10]=(byte)room.getN_index();
			if(automode) pkt[11]=0x01;
			else pkt[11]=0x00;
			twobytes=Converter.EncodeTime(sensTime);
			pkt[12]=twobytes[0];
			pkt[13]=twobytes[1];
			waitForACK(Packet.ACK_CONFIG_REF,NTWaddr);
			UARTmanager.sendDataToUART(pkt);
		}
	}
	
	public static void configureBlindNode(int NTWaddr, boolean autoMode, boolean distribuitedLoc, double timeOut, double cycle, boolean updateDB) {
		if(cycle>=0 && timeOut>=0 && cycle>=timeOut && blindNodes.containsKey(NTWaddr)) {
			BlindNode bn=blindNodes.get(NTWaddr);
			System.out.println("Send: blindConfig NTWaddr:"+NTWaddr+" timeOut:"+timeOut+" Cycle:"+cycle);
			if(updateDB) DBmanager.updateBlindData(bn.getIEEEaddr(),autoMode,distribuitedLoc,(float)cycle,(float)timeOut);
			byte[] pkt=new byte[9];
			pkt[0]=(byte)CONFIG_BLIND_NODE;
			byte[] addr=new byte[2];
			addr=Converter.Encode16U(NTWaddr,false);
			pkt[1]=addr[0];
			pkt[2]=addr[1];
			if(autoMode) pkt[3]=(byte)0x01;
			else pkt[3]=(byte)0x00;
			if(distribuitedLoc) pkt[4]=(byte)0x01;
			else pkt[4]=(byte)0x00;			
			byte[] timeout,bcycle;
			timeout=Converter.EncodeTime(timeOut);
			pkt[5]=timeout[0];
			pkt[6]=timeout[1];
			bcycle=Converter.EncodeTime(cycle);
			pkt[7]=bcycle[0];
			pkt[8]=bcycle[1];
			waitForACK(Packet.ACK_CONFIG_BLIND,NTWaddr);
			UARTmanager.sendDataToUART(pkt);
		}
	}

//	public static void requestAllRefsAutoSens(float deltaTsec) {
//		for(Entry<Integer, Node> e : refNodes.entrySet())
//			requestAutoSens(e.getValue().getNTWaddr(),deltaTsec,true);
//	}
//	
//	public static void requestAllBlindsAutoSens(float deltaTsec) {
//		for(Entry<Integer, BlindNode> e : blindNodes.entrySet())
//			requestAutoSens(e.getValue().getNTWaddr(),deltaTsec,true);
//	}
	
	public static BlindNode getBlindNode(int NWKaddr) {
		return blindNodes.get(NWKaddr);
	}
	
	public static Node getRefNode(int NWKaddr) {
		return refNodes.get(NWKaddr);
	}
	
	public static HashMap<Integer, BlindNode> getAllBlindNodes() {
		return blindNodes;
	}
	
	public static HashMap<Integer, Node> getAllRefNodes() {
		return refNodes;
	}
	
	public static void requestAllBlindsNodesDiagnostic() {
		for(Entry<Integer, BlindNode> e : blindNodes.entrySet()) requestDiagnostic(e.getValue().getNTWaddr());
	}
	
	public static void requestAllRefNodesDiagnostic() {
		for(Entry<Integer, Node> e : refNodes.entrySet()) requestDiagnostic(e.getValue().getNTWaddr());
	}
	
	public static void setExpectedNumberOfRefNodes(int number) {
		expectedNumOfRefNodes=number;
	}
	
	public static boolean isSystemStarted() {
		return systemStarted;
	}
	
	public static String getRoomName(int roomID) {
		return rooms.get(roomID).getName();
	}

	public static HashMap<Integer, Room> getAllRooms() {
		return rooms;
	}
	
	public static HashMap<String, Good> getAllGoods() {
		return goods;
	}
	
	public static void updateNodeName(int NWKaddr, boolean isRef, String newName) {
		Node n;
		if(isRef) n=refNodes.get(NWKaddr);
		else n=blindNodes.get(NWKaddr);
		n.setName(newName);
		DBmanager.updateName(NWKaddr,newName);
	}
	
	public static int updateRoom(int roomID, String newName, double distMax, double A, double n) {
		if(rooms.containsKey(roomID)) {
			boolean trovato=false;
			for(Room r:rooms.values()) {//controllo se c'è già uno stesso nome
				if(r.getId()!=roomID && r.getName().compareTo(newName)==0) trovato=true;
				if(trovato) break;
			}
			if(!trovato) {//controllare se il nome non esite già
				Room updatingRoom=rooms.get(roomID);
				updatingRoom.setName(newName);
				updatingRoom.setMaxDist(distMax);
				DBmanager.updateRoom(updatingRoom);
				if(updatingRoom.getA()!=A || updatingRoom.getN()!=n) {
					updatingRoom.setA(A);
					updatingRoom.setN(n);
					for(Node r:refNodes.values()) //si riconfigurano A ed n
						if(r.getRoomId()==roomID) //nei refnodes di quella stanza
							configureRefNode(r.getNTWaddr(),r.getXpos(),r.getYpos(),roomID,r.isAutoMode(),r.getCycle(),false);
				}
				return(1); //tutto ok!
			} else return(-1); //il nome della stanza esiste già!
		} else return(0); //Stanza inesistente!
	}
	
	public static int updateGood(String origIEEEaddr, String newName, int newType, int NTWassociated) {
		if(goods.containsKey(origIEEEaddr)) {
			Good g=goods.get(origIEEEaddr);
			if(g.getName().compareTo(newName)!=0)
				if(DBmanager.existsGood(newName)) return(-1); //il nome del bene esiste già!
				else g.setName(newName);
				g.setTypeId(newType);
				if(g.isAssociated()) { //sono associato
					if(NTWassociated==0) { //e mi voglio disassociare
						g.disassociate();
						int i=0;
						while(goods.containsKey(""+i)) i++;
						goods.remove(origIEEEaddr);
						goods.put(""+i,g);
						DBmanager.deleteGood(origIEEEaddr);
						DBmanager.insertGood(""+i,g);
					} else if(NTWassociated!=g.getAssocitedBlindNode().getNTWaddr()) { //voglio cambiare blind
						BlindNode b=blindNodes.get(NTWassociated);
						g.disassociate(); //mi dissocio dal nodo corrente
						g.associateBlindNode(b); //e mi associo a quello nuovo
						goods.remove(origIEEEaddr); //cancello la vecchia ntry dalla memoria
						goods.put(b.getIEEEaddrString(),g); //inserisco quella nuova
						DBmanager.deleteGood(origIEEEaddr); //faccio lo stesso nel db
						DBmanager.insertGood(b.getIEEEaddrString(),g);
					} else { //rimango associato al mio fedele blindnode
						DBmanager.updateGood(origIEEEaddr,newName,newType);
					}
				} else { //non sono associato
					if(NTWassociated!=0) { //e mi voglio associare
						BlindNode b=blindNodes.get(NTWassociated);
						g.associateBlindNode(b);
						goods.remove(origIEEEaddr);
						goods.put(b.getIEEEaddrString(),g);
						DBmanager.deleteGood(origIEEEaddr);
						DBmanager.insertGood(b.getIEEEaddrString(),g);
					} else { //e voglio continuare a fare l'asociale
						DBmanager.updateGood(origIEEEaddr,newName,newType);
					}
				}
				return(1); //tutto ok!
		} else return(0); //Bene inesistente!
	}

	public static void Stop() {
		systemStarted=false;
		refNodes.clear();
		refNodes=null;
		blindNodes.clear();
		blindNodes=null;
		rooms.clear();
		rooms=null;
		goods.clear();
		goods=null;
		numOfRefs=0;
		numOfBlinds=0;
		expectedNumOfRefNodes=0;
	}

	public static void deleteRoom(int roomID) {
		rooms.remove(roomID);
		DBmanager.deleteRoom(roomID);
	}
	
	public static void deleteRoomAndMoveNodes(int deletingRoomId, int newRoomIdForNodes) {
		if(rooms.containsKey(deletingRoomId) && rooms.containsKey(newRoomIdForNodes)) {
			Room newRoom=rooms.get(newRoomIdForNodes);
			Collection<Node> refnodeslist=refNodes.values();
			Iterator<Node> itr=refnodeslist.iterator();
			while(itr.hasNext()) {
				Node n=itr.next();
				if(n.getRoomId()==deletingRoomId) n.setRoom(newRoom);
			}
			DBmanager.moveNodes(deletingRoomId,newRoomIdForNodes);
			deleteRoom(deletingRoomId);
		}
	}
	
	public int searchRefNodeByIEEEaddr(char ieeeaddr[]) {
		Collection<Node> refnodes=new LinkedList<Node>();
		refnodes=refNodes.values();
		Iterator<Node> itr=refnodes.iterator();
		while(itr.hasNext()) {
			Node currentNode=itr.next();
			boolean diffFound=false;
			for(int i=0;i<8 && !diffFound;i++) if(currentNode.getIEEEaddr()[i]!=ieeeaddr[i]) diffFound=true;
			if(!diffFound) return(currentNode.getNTWaddr());
		}
		return(-1);
	}
	
	public int searchBlindNodeByIEEEaddr(char ieeeaddr[]) {
		Collection<BlindNode> blindnodes=new LinkedList<BlindNode>();
		blindnodes=blindNodes.values();
		Iterator<BlindNode> itr=blindnodes.iterator();
		while(itr.hasNext()) {
			Node currentNode=itr.next();
			boolean diffFound=false;
			for(int i=0;i<8 && !diffFound;i++) if(currentNode.getIEEEaddr()[i]!=ieeeaddr[i]) diffFound=true;
			if(!diffFound) return(currentNode.getNTWaddr());
		}
		return(-1);
	}

	public static void deleteGood(String longAddr) {
		if(goods.containsKey(longAddr)) {
			Good g=goods.get(longAddr);
			if(g.isAssociated()) g.disassociate();
			goods.remove(longAddr);
			DBmanager.deleteGood(longAddr);
		}
	}

	public static void deleteNode(int NTWaddr, boolean isRef) {
		if(isRef) {
			if(refNodes.containsKey(NTWaddr)) refNodes.remove(NTWaddr);
		} else if(blindNodes.containsKey(NTWaddr)) blindNodes.remove(NTWaddr);
	}

	public static Room getRoomById(int roomId) {
		if(rooms.containsKey(roomId)) return rooms.get(roomId);
		else return rooms.get(0); //ritorna la stanza 0 nel caso roomId non esiste
	}

	public static Good getGood(String longAddr) {
		if(goods.containsKey(longAddr)) return goods.get(longAddr);
		else return null;
	}

}
