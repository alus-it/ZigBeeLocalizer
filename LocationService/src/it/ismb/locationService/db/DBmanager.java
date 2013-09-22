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

package it.ismb.locationService.db;
import it.ismb.locationService.Converter;
import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.Room;
import it.ismb.locationService.Nodes.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DBmanager {
	private static MysqlDatabase db;
	private static boolean isDBmanagerActive;

	public DBmanager(String hostname,String username,String password, String database) {
		isDBmanagerActive=false;
		db = new MysqlDatabase(hostname, username, password, database);
		if(!db.isavailable()) System.out.println("DB not available");
		else try {
			checkTableExistance();
			clearTables();
			isDBmanagerActive=true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void clearTables() throws IOException {
		String query = "DELETE FROM positions;";
		if(!db.queryNoResponse(query)) throw new IOException("clear failed: " + query);
		query = "DELETE FROM sensors_story;";
		if(!db.queryNoResponse(query)) throw new IOException("clear failed: " + query);
		System.out.println("I've just cleared table for a new session.");
	}

	private static void checkTableExistance() throws IOException {
		MysqlDatabaseResponse result = db.query("SHOW TABLES LIKE 'nodes';");
		if(result.nlines()!=1) throw new IOException("Table nodes does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'sensors';");
		if(result.nlines()!=1) throw new IOException("Table sensors does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'positions';");
		if(result.nlines()!=1) throw new IOException("Table positions does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'rooms';");
		if(result.nlines()!=1) throw new IOException("Table rooms does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'types';");
		if(result.nlines()!=1) throw new IOException("Table types does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'blindsdata';");
		if(result.nlines()!=1) throw new IOException("Table blindsdata does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'goods';");
		if(result.nlines()!=1) throw new IOException("Table goods does NOT exist in the database.");
		result = db.query("SHOW TABLES LIKE 'sensors_story';");
		if(result.nlines()!=1) throw new IOException("Table sensors_story does NOT exist in the database.");
	}

	public static void insertRefNode(Node rn) {
		double x=0,y=0,cycle;
		int room=0;
		boolean automode;
		String queryText;
		String longAddr=Converter.Vector2HexStringNoSpaces(rn.getIEEEaddr());
		MysqlDatabaseResponse result = db.query("SELECT * FROM nodes "+ //aggiorno all_nodes
				"WHERE IEEEaddr='"+longAddr+"';");
		if(result.success()) {
			if(result.nlines()>0) { //se è già presente nel DB
				try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(0));
					int NTWaddr=Integer.parseInt(iRow.get("NTWaddr"));
					int isRef=Integer.parseInt(iRow.get("isRef"));
					x=Double.parseDouble(iRow.get("xpos"));
					y=Double.parseDouble(iRow.get("ypos"));
					room=Integer.parseInt(iRow.get("roomID"));
					if(Integer.parseInt(iRow.get("automode"))==0) automode=false;
					else automode=true;
					cycle=Double.parseDouble(iRow.get("cycleTime"));
					try {
						rn.setParentRSSI(Integer.parseInt(iRow.get("parentRSSI")));
					} catch(java.lang.NumberFormatException e) {
						rn.setParentRSSI(0);
					}
					if(isRef!=1) { //Nel DB il nodo non risulta come di riferimento aggiornare tutto
						System.out.println("Il nodo presente in DB ha cambiato ruolo da mobile a riferimento");
						queryText="UPDATE nodes SET isRef=1,NTWaddr="+rn.getNTWaddr()+
						",timestamp="+rn.getTimestamp()+",numOfSens="+rn.getNumOfSens()+
						",name='New RefNode'"+
						"WHERE IEEEaddr='"+longAddr+"';";
						if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						queryText="DELETE FROM blindsdata WHERE IEEEaddr='"+longAddr+"';";//cancello i dati blind
						if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						queryText="DELETE FROM positions WHERE IEEEaddr='"+longAddr+"';";//cancello lo storico localizzazioni
						if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						queryText="DELETE FROM goods WHERE IEEEaddr='"+longAddr+"';"; //cancello il bene associato
						if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
					} else { //nodo corretto nel DB ma forse c'è da aggiornare NTWaddr
						if(rn.getNTWaddr()!=NTWaddr) { //aggiornare solo NTWaddr
							System.out.println("Nodo presente in nodes ma da aggiornare l'indirizzo di rete");
							queryText = "UPDATE nodes SET NTWaddr="+rn.getNTWaddr()+
							", timestamp="+rn.getTimestamp()+" WHERE IEEEaddr='"+longAddr+"';";
							if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						} else { //dati in db corretti solo da aggiornare il timestamp
							System.out.println("Nodo presente aggiorno il timestamp");
							queryText = "UPDATE nodes SET timestamp="+rn.getTimestamp()+" "+
							"WHERE IEEEaddr='"+longAddr+"';";
							if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						}
						rn.setName(iRow.get("name")); //gli imposto il nome	
						if(rn.getRoomId()==room &&
								Math.abs(rn.getXpos()-x)<0.125 && Math.abs(rn.getYpos()-y)<0.125 &&
								rn.isAutoMode()==automode && Math.abs(rn.getCycle()-cycle)<0.05)
							rn.setXYPosition(x,y); //memorizzo in node solo x e y con la massima precisione
						else { //riconfigurare
							System.out.println("RefNode da riconfigurare con le coordinate: ("+x+";"+y+")");
							PacketDecoder.configureRefNode(rn.getNTWaddr(),x,y,room,automode,cycle,false);
						}
						MysqlDatabaseResponse result2 = db.query("SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"';");
						if(result2.success() && result2.nlines()>0) for(int i=0;i<result2.nlines();i++) try {
							final Map<String, String> xRow = result2.getvaluesmap().get(new Integer(i));
							rn.setSensor(Integer.parseInt(xRow.get("sensId")),Double.parseDouble(xRow.get("sensValue")));
						} catch (Exception e) {
							System.out.println("Exception caught while query: SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"';");
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					System.out.println("Exception caught while query: SELECT * FROM nodes WHERE IEEEaddr='"+longAddr+"';");
					e.printStackTrace();
				}
			} else { //non presente in DB nodes da inserire
				System.out.println("Nodo nuovo da inserire anche in nodes");
				queryText = "INSERT INTO nodes"+
				"(IEEEaddr,isRef,NTWaddr,xpos,ypos,roomID,timestamp,numOfSens) "+
				"VALUES('"+longAddr+"',1,"+rn.getNTWaddr()+","+rn.getXpos()+","+rn.getYpos()+","+
				rn.getRoomId()+","+rn.getTimestamp()+","+rn.getNumOfSens()+");";
				if (!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
				for(Sensor s:rn.getSensors().values()) {
					queryText="INSERT INTO sensors (IEEEaddr,sensId) VALUES('"+longAddr+"',"+s.getId()+");";
					if(!db.queryNoResponse(queryText)) System.out.println("INSERT failed Query: "+queryText);
				}
			}
		} else System.out.println("Lookup query failed: SELECT FROM all_nodes...");
	}

	public static void insertBlindNode(BlindNode bn) {
		int shortAddr=bn.getNTWaddr();
		String longAddr=Converter.Vector2HexStringNoSpaces(bn.getIEEEaddr());
		MysqlDatabaseResponse result = db.query("SELECT * FROM nodes "+
				"WHERE IEEEaddr='"+longAddr+"';");
		if(result.success()) {
			if(result.nlines()>0) {
				try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(0));
					int NTWaddr=Integer.parseInt(iRow.get("NTWaddr"));
					int isRef=Integer.parseInt(iRow.get("isRef"));
					double cycle=Double.parseDouble(iRow.get("cycleTime"));
					boolean automode;
					if(Integer.parseInt(iRow.get("automode"))==1) automode=true;
					else automode=false;
					if(isRef!=0) { //Nel DB il nodo non risulta come mobile aggiornare tutto
						System.out.println("Il nodo presente in DB ha cambiato ruolo da riferimento a mobile");
						String queryText = "UPDATE nodes SET "+
						"isRef=0,NTWaddr="+shortAddr+",timestamp="+bn.getTimestamp()+", "+
						"numOfSens="+bn.getNumOfSens()+
						"WHERE IEEEaddr='"+longAddr+"';";
					if(!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
					} else { //il nodo è corretto ma forse c'è da aggiornare l'indirizzo di rete
						if(shortAddr!=NTWaddr) { //aggiornare solo NTWaddr
							System.out.println("Nodo presente in nodes ma da aggiornare l'indirizzo di rete");
							String queryText = "UPDATE nodes SET "+
							"NTWaddr="+shortAddr+", timestamp="+bn.getTimestamp()+
							" WHERE IEEEaddr='"+longAddr+"';";
							if(!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						} else { //dati in db corretti solo da settare attivo
							System.out.println("Nodo presente aggiorno il timestamp");
							String queryText = "UPDATE nodes SET timestamp="+bn.getTimestamp()+" "+
							"WHERE IEEEaddr='"+longAddr+"';";
							if(!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
						}
						bn.setName(iRow.get("name"));
						MysqlDatabaseResponse result3 = db.query("SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"';");
						if(result3.success() && result3.nlines()>0) for(int i=0;i<result3.nlines();i++) try {
							final Map<String, String> xRow = result3.getvaluesmap().get(new Integer(i));
							bn.setSensor(Integer.parseInt(xRow.get("sensId")),Double.parseDouble(xRow.get("sensValue")));	
						} catch (Exception e) {
							System.out.println("Exception caught while query: SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"';");
							e.printStackTrace();
						}
						MysqlDatabaseResponse result2=db.query("SELECT * FROM blindsdata WHERE IEEEaddr='"+longAddr+"';");
						if(result2.success() && result2.nlines()==1) try {
							final Map<String, String> iRow2 = result2.getvaluesmap().get(new Integer(0));
							double timeout=Double.parseDouble(iRow2.get("timeout"));
							boolean distribuitedLoc;
							if(Integer.parseInt(iRow2.get("distribuitedAlg"))==1) distribuitedLoc=true;
							else distribuitedLoc=false;
							if(Math.abs(bn.getCycle()-cycle)<0.05 ||
									Math.abs(bn.getTimeOut()-timeout)<0.05 ||
									bn.isAutoMode()!=automode ||
									bn.isDistribuitedLoc()!=distribuitedLoc) {
								System.out.println("BlindNode da riconfigurare.");
								PacketDecoder.configureBlindNode(shortAddr,automode,distribuitedLoc,timeout,cycle,false); //riconfiguro
							}
						} catch (Exception e) {
							System.out.println("Exception while query: SELECT * FROM blindsdata WHERE IEEEaddr='"+longAddr+"';");
							e.printStackTrace();
						}
					}
				} catch(Exception e) {
					System.out.println("Exception caught while query: "+
							"SELECT autoTxRSSItime FROM blindsdata WHERE IEEEaddr='"+longAddr+"';");
					e.printStackTrace();
				}
			} else { //non presente in nodes da inserire
				System.out.println("Nodo nuovo da inserire anche in nodes");
				String queryText = "INSERT INTO nodes (IEEEaddr,isRef,NTWaddr,timestamp,numOfSens,name,automode,cycleTime) "+
				"VALUES('"+longAddr+"',0,"+shortAddr+","+bn.getTimestamp()+","+bn.getNumOfSens()+",'"+bn.getName()+"',";
				if(bn.isAutoMode()) queryText=queryText+"1,";
				else queryText=queryText+"0,";
				queryText=queryText+bn.getCycle()+");";
				if(!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
				queryText="INSERT INTO blindsdata (IEEEaddr,distribuitedAlg,timeout) VALUES('"+
				longAddr+"',";
				if(bn.isDistribuitedLoc()) queryText=queryText+"1,";
				else queryText=queryText+"0,";
				queryText=queryText+bn.getTimeOut()+");";
				if(!db.queryNoResponse(queryText)) System.out.println("Failed Query: " + queryText);
				for(Sensor s:bn.getSensors().values()) {
					queryText="INSERT INTO sensors (IEEEaddr,sensId) VALUES('"+longAddr+"',"+s.getId()+");";
					if(!db.queryNoResponse(queryText)) System.out.println("INSERT failed Query: "+queryText);
				}
			}
		} else System.out.println("Lookup query failed: SELECT FROM all_nodes...");
	}
	
	public static BlindNode getBlindByIEEEfromDB(int[] IEEEaddr) {
		BlindNode bn=null;
		String longAddr=Converter.Vector2HexStringNoSpaces(IEEEaddr);
		MysqlDatabaseResponse result=db.query("SELECT * FROM nodes WHERE IEEEaddr='"+longAddr+"' AND isRef=0;");
		if(result.success() && result.nlines()==1) try {
			final Map<String, String> iRow = result.getvaluesmap().get(new Integer(0));
			boolean automode;
			if(Integer.parseInt(iRow.get("automode"))==0) automode=false;
			else automode=false;
			bn=new BlindNode(IEEEaddr,
					Integer.parseInt(iRow.get("NTWaddr")),
					Double.parseDouble(iRow.get("xpos")),
					Double.parseDouble(iRow.get("ypos")),
					PacketDecoder.getRoomById(Integer.parseInt(iRow.get("roomID"))),
					Integer.parseInt(iRow.get("numOfSens")),
					Long.parseLong(iRow.get("timestamp")),
					iRow.get("name"),
					automode,
					Double.parseDouble(iRow.get("cycleTime")));
			MysqlDatabaseResponse result2=db.query("SELECT * FROM blindsdata WHERE IEEEaddr='"+longAddr+"';");
			if(result2.success() && result2.nlines()==1) try {
				final Map<String, String> bRow = result2.getvaluesmap().get(new Integer(0));
				if(Integer.parseInt(bRow.get("distribuitedAlg"))==1) bn.setDistribuiteLoc(true);
				else bn.setDistribuiteLoc(false);
				bn.setTimeOut(Float.parseFloat(bRow.get("timeout")));
			} catch (Exception e) {
					System.out.println("Exception "+e.getClass().getName()+" caught while query:"
						+ "SELECT * FROM blindsdata WHERE IEEEaddr='"+longAddr+"';");
						e.printStackTrace();
			}
			if(PacketDecoder.getAllGoods().containsKey(longAddr)) {
				bn.associateGood(PacketDecoder.getGood(longAddr));
				System.out.println("Associated good name: "+PacketDecoder.getGood(longAddr).getName());
			}
			System.out.println("HASGOOD:"+bn.hasGood());
			MysqlDatabaseResponse result3=db.query("SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"';");
			if(result3.success() && result3.nlines()>0) for(int j=0;j<result3.nlines();j++) try {
				final Map<String, String> jRow = result3.getvaluesmap().get(new Integer(j));
				bn.setSensor(Integer.parseInt(jRow.get("sensId")),Double.parseDouble(jRow.get("sensValue")));
			} catch (Exception e) {
				System.out.println("Exception while query: SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"';");
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.out.println("Exception"+e.getClass().getName()+" caught while query for a specific blind");
			e.printStackTrace();
		}
		return(bn);
	}
	
	public static void loadRooms() {
		MysqlDatabaseResponse result = db.query("SELECT * FROM rooms;");
		if(result.success()) {
			int nres=result.nlines();
			if(nres>0) {
				for(int i=0;i<nres;i++) try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(i));
					int roomID=Integer.parseInt(iRow.get("roomID"));
					String roomName=new String(iRow.get("name"));
					double A=Double.parseDouble(iRow.get("A"));
					double n=Double.parseDouble(iRow.get("n"));
					double distMax=Double.parseDouble(iRow.get("distMax"));
					Room roomToLoad=new Room(roomID,roomName,distMax,n,A);
					PacketDecoder.loadRoom(roomID,roomToLoad);
				} catch (Exception e) {
					System.out.println("Exception " + e.getClass().getName()
							+ " caught while query: SELECT * FROM rooms");
				}
			}
		} else System.out.println("Lookup query failed: SELECT * FROM rooms");
	}
	
	public static void loadGoods() {
		MysqlDatabaseResponse result = db.query("SELECT * FROM goods;");
		if(result.success()) {
			int nres=result.nlines();
			if(nres>0) {
				for(int i=0;i<nres;i++) try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(i));
					Good good=new Good(iRow.get("name"),Integer.parseInt(iRow.get("type")));
					PacketDecoder.loadGood(iRow.get("IEEEaddr"),good);
				} catch (Exception e) {
					System.out.println("Exception " + e.getClass().getName()
							+ " caught while query: SELECT * FROM goods");
				}
			}
		} else System.out.println("Lookup query failed: SELECT * FROM rooms");
		
	}
	
	public static void loadTypes() {
		MysqlDatabaseResponse result = db.query("SELECT * FROM types;");
		if(result.success()) {
			int nres=result.nlines();
			if(nres>0) {
				for(int i=0;i<nres;i++) try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(i));
					int typeID=Integer.parseInt(iRow.get("typeID"));
					String typeName=new String(iRow.get("name"));
					Good.loadType(typeID, typeName);
				} catch (Exception e) {
					System.out.println("Exception " + e.getClass().getName()
							+ " caught while query: SELECT * FROM types");
				}
			}
		} else System.out.println("Lookup query failed: SELECT * FROM types");
	}
	
	public static List<String> getTypes() {
		MysqlDatabaseResponse result = db.query("SELECT * FROM types;");
		if(result.success()) {
			int nres=result.nlines();
			if(nres>0) {
				List<String> typesList=new LinkedList<String>();
				for(int i=0;i<nres;i++) try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(i));
					//typesList.add(Integer.parseInt(iRow.get("typeID")), new String(iRow.get("name")));
					typesList.add(new String(iRow.get("name")));
				} catch (Exception e) {
					System.out.println("Exception "+e.getClass().getName()+" while query: SELECT * FROM types;");
					return null;
				}
				return typesList;
			}
		} else System.out.println("Lookup query failed: SELECT * FROM types;");
		return null;
	}
	
	public static void checkAllRefNodesInDB() { //richiede a tutti i refNodes nel DB di annunciarsi
		MysqlDatabaseResponse result = db.query("SELECT * FROM nodes WHERE isRef=1;");
		if(result.success()) {
			int nres=result.nlines();
			if(nres>0) {
				for(int i=0;i<nres;i++) try {
					final Map<String, String> iRow = result.getvaluesmap().get(new Integer(i));
					int[] IEEEaddr=Converter.DecodeIEEEaddr(iRow.get("IEEEaddr"));
					PacketDecoder.requestIEEEannounce(IEEEaddr);
				} catch (Exception e) {
					System.out.println("Exception "+e.getClass().getName()+" while query: SELECT * FROM nodes;");
					e.printStackTrace();
					return;
				}
				PacketDecoder.setExpectedNumberOfRefNodes(nres);
			}
		} else System.out.println("Lookup query failed: SELECT * FROM nodes WHERE isRef=1;");	
	}
	
	public static void updateNWKaddr(int[] IEEEaddr, int newNWKaddr) {
		String queryText = "UPDATE nodes SET NTWaddr="+newNWKaddr+
		" WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
		}
	
	public static void updateDiagnostic(int[] IEEEaddr, double vBatt, int parentRSSI, long timestamp) {
		String longAddr=Converter.Vector2HexStringNoSpaces(IEEEaddr);
		String queryText = "UPDATE nodes SET parentRSSI="+parentRSSI+",timestamp="+timestamp+
		" WHERE IEEEaddr='"+longAddr+"';";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
		queryText = "UPDATE sensors SET sensValue="+vBatt+",timestamp="+timestamp+
		" WHERE IEEEaddr='"+longAddr+"' AND sensId="+Sensor.BATT_ID+";";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}

	public static void updateRefConfig(int NTWaddr, double x, double y, int roomId, boolean auto, double sensTime) {
		String queryText = "UPDATE nodes SET xpos="+x+", ypos="+y+", roomID="+roomId;
		if(auto) queryText=queryText+", automode=1";
		else queryText=queryText+", automode=0";
		queryText=queryText+", cycleTime="+sensTime+" WHERE NTWaddr="+NTWaddr+";";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}
	
	public static void updateBlindPos(int[] IEEEaddr, double x, double y, int roomID, long timestamp) {
		String queryText = "UPDATE nodes SET xpos="+x+", ypos="+y+", roomID="+roomID+
		", timestamp="+timestamp+" WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if (!db.queryNoResponse(queryText))
			System.out.println("UPDATE failed for query: " + queryText);
		queryText = "INSERT INTO positions (IEEEaddr,timestamp,xpos,ypos,roomID) "+
		"VALUES('"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"',"+timestamp+","+x+","+y+","+roomID+");";
		if (!db.queryNoResponse(queryText)) System.out.println("INSERT failed for query: " + queryText);
	}

	public static void updateSens(Node n) {
		String longAddr=n.getIEEEaddrString();
		String queryText;
		long timestamp=n.getTimestamp();
		for(Sensor s:n.getSensors().values()) {
			queryText="SELECT * FROM sensors WHERE IEEEaddr='"+longAddr+"' AND sensId="+s.getId()+";";
			MysqlDatabaseResponse result = db.query(queryText);
			if(result.nlines()==0) queryText="INSERT INTO sensors (IEEEaddr,sensId,timestamp,sensValue)"+
			" VALUES('"+longAddr+"',"+s.getId()+","+timestamp+","+s.getValue()+");";
			else queryText="UPDATE sensors SET sensValue="+s.getValue()+
			",timestamp="+timestamp+" WHERE IEEEaddr='"+longAddr+"' AND sensId="+s.getId()+";";
			if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
			queryText="INSERT INTO sensors_story (IEEEaddr,sensId,timestamp,sensValue) VALUES('"+
			longAddr+"',"+s.getId()+","+timestamp+","+s.getValue()+");";
			if (!db.queryNoResponse(queryText)) System.out.println("INSERT failed for query: "+queryText);
		}
	}

	public static void updateBlindData(int[] IEEEaddr, boolean automode, boolean distribuitedLoc, float cycle, float timeOut) {
		String queryText="UPDATE nodes SET cycleTime="+cycle;
		if(automode) queryText=queryText+", automode=1 ";
		else queryText=queryText+", automode=0 ";
		queryText=queryText+"WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: " + queryText);
		queryText = "UPDATE blindsdata SET timeout="+timeOut;
		if(distribuitedLoc) queryText=queryText+", distribuitedAlg=1 ";
		else queryText=queryText+", distribuitedAlg=0 ";
		queryText=queryText+"WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: " + queryText);
	}

	public static void updateCycleTime(int NTWaddr, float cycleTime) {
		String queryText = "UPDATE nodes SET cycleTime="+cycleTime+" WHERE NTWaddr="+NTWaddr+";";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for packet query: "+queryText);
	}

	public static void updateName(int NWKaddr, String newName) {
		String queryText = "UPDATE nodes SET name='"+newName+"' WHERE NTWaddr="+NWKaddr+";";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}

	public static void updateRoom(Room r) {
		String queryText = "UPDATE rooms SET name='"+r.getName()+"', n="+r.getN()+", A="+r.getA()+
		", distMax="+r.getMaxDist()+" WHERE roomID="+r.getId()+";";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}

	public static void Stop() {
		isDBmanagerActive=false;
		db.disconnect();
		db=null;
	}
	
	public static boolean isActive() {
		return isDBmanagerActive;
	}

	public static void deleteRoom(int roomID) {
		String queryText="DELETE FROM rooms WHERE roomID="+roomID+";";
		if (!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
	}

	public static void updateShortAddr(int[] IEEEaddr, int NTWaddr) {
		String queryText="UPDATE nodes SET NTWaddr="+NTWaddr+
			"WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}

	public static void insertRoom(Room r) {
		String queryText="INSERT INTO rooms (roomID,name,A,n,distMax) VALUES("+
			r.getId()+",'"+r.getName()+"',"+r.getA()+","+r.getN()+","+r.getMaxDist()+");";
		if (!db.queryNoResponse(queryText)) System.out.println("INSERT failed for query: "+queryText);
	}

	public static void moveNodes(int fromRoom, int toRoom) {
		String queryText="UPDATE nodes SET roomID="+toRoom+" WHERE roomID="+fromRoom+";";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}
	
	public static int countGoodsOfType(String type) {
		String queryText="SELECT COUNT(*) FROM goods WHERE type="+
		"(SELECT typeID FROM types WHERE name='"+type+"');";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(new Integer(0));
					return(Integer.parseInt(Row.get("COUNT(*)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(-1);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(-1);
	}
	
	public static boolean existsType(String type) {
		String queryText="SELECT COUNT(*) FROM types WHERE name='"+type+"';";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(new Integer(0));
					if(Integer.parseInt(Row.get("COUNT(*)"))>0) return(true);
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(false);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(false);
	}

	public static void updateTypeName(String currentName, String newName) {
		String queryText="UPDATE types SET name='"+newName+"' WHERE name='"+currentName+"';";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}
	
	public static void changeGoodsType(String currentType, String newType) {
		String queryText="UPDATE goods SET type=(SELECT typeID FROM types WHERE name='"+newType+"')"+
		"WHERE type=(SELECT typeID FROM types WHERE name='"+currentType+"');";
		if (!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}

	public static void deleteType(String type) {
		String queryText="DELETE FROM types WHERE name='"+type+"';";
		if (!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
	}

	public static void insertType(int typeID, String newTypeName) {
		String queryText="INSERT INTO types (typeID,name) VALUES("+typeID+",'"+newTypeName+"');";
		if (!db.queryNoResponse(queryText)) System.out.println("INSERT failed for query: "+queryText);
	}
	
	public static boolean existsGood(String name) {
		String queryText="SELECT COUNT(*) FROM goods WHERE name='"+name+"';";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(new Integer(0));
					if(Integer.parseInt(Row.get("COUNT(*)"))>0) return(true);
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(false);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(false);
	}

	public static void insertGood(String longAddr, Good g) {
		String queryText="INSERT INTO goods (IEEEaddr,name,type) "+
			"VALUES('"+longAddr+"','"+g.getName()+"',"+g.getTypeId()+");";
		if(!db.queryNoResponse(queryText)) System.out.println("INSERT failed for query: "+queryText);
	}

	public static void updateGood(String IEEEaddr, String newName, int newType) {
		String queryText="UPDATE goods SET name='"+newName+"', type="+newType+" WHERE IEEEaddr='"+IEEEaddr+"';";
		if(!db.queryNoResponse(queryText)) System.out.println("UPDATE failed for query: "+queryText);
	}

	public static void deleteGood(String IEEEaddr) {
		String queryText="DELETE FROM goods WHERE IEEEaddr='"+IEEEaddr+"';";
		if(!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
	}
	
	public static int countLocalization(int[] IEEEaddr) {
		String queryText="SELECT COUNT(*) FROM positions WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(0);
					return(Integer.parseInt(Row.get("COUNT(*)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(0);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(0);
	}
	
	public static long getFirstLocalizationTime(int[] IEEEaddr) {
		String queryText="SELECT MIN(timestamp) FROM positions WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(0);
					return (Long.parseLong(Row.get("MIN(timestamp)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(-1);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(-1);
	}
	
	public static long getLastLocalizationTime(int[] IEEEaddr) {
		String queryText="SELECT MAX(timestamp) FROM positions WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(0);
					return (Long.parseLong(Row.get("MAX(timestamp)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(-1);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(-1);
	}
	
	public static List<Map<String,String>> getAllLocalizationsOf(int[] IEEEaddr, long fromTime, long toTime) {
		String queryText="SELECT timestamp,xpos,ypos,roomID FROM positions WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"' AND timestamp>="+fromTime+" AND timestamp<="+toTime+";";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			int n=result.nlines();
			if(n>0) try {
				List<Map<String,String>> results=new LinkedList<Map<String, String>>();
				for(int i=0;i<n;i++) results.add(result.getvaluesmap().get(new Integer(i)));
				return(results);
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(null);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(null);
	}

	public static void deleteNode(int[] IEEEaddr, boolean isRef) {
		String queryText="DELETE FROM nodes WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if(!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
		queryText="DELETE FROM sensors WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
		if(!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
		if(!isRef) {
			queryText="DELETE FROM blindsdata WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
			if(!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
			queryText="DELETE FROM positions WHERE IEEEaddr='"+Converter.Vector2HexStringNoSpaces(IEEEaddr)+"';";
			if(!db.queryNoResponse(queryText)) System.out.println("DELETE failed for query: "+queryText);
		}
	}

	public static int countSensHistory(int[] IEEEaddr, int sensId) {
		String queryText="SELECT COUNT(*) FROM sensors_story WHERE IEEEaddr='"+
		Converter.Vector2HexStringNoSpaces(IEEEaddr)+"' AND sensId="+sensId+";";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
				final Map<String, String> Row = result.getvaluesmap().get(0);
				return(Integer.parseInt(Row.get("COUNT(*)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(0);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(0);
	}
	
	public static long getFirstSensorRecordTime(int[] IEEEaddr, int sensId) {
		String queryText="SELECT MIN(timestamp) FROM sensors_story WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"' AND sensId="+sensId+";";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(0);
					return (Long.parseLong(Row.get("MIN(timestamp)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(-1);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(-1);
	}
	
	public static long getLastSensorRecordTime(int[] IEEEaddr, int sensId) {
		String queryText="SELECT MAX(timestamp) FROM sensors_story WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"' AND sensId="+sensId+";";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			if(result.nlines()==1) try {
					final Map<String, String> Row = result.getvaluesmap().get(0);
					return (Long.parseLong(Row.get("MAX(timestamp)")));
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(-1);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(-1);
	}

	public static List<Map<String, String>> getAllSensValuesOf(int[] IEEEaddr, int sensId, long fromTime, long toTime) {
		String queryText="SELECT timestamp, sensValue FROM sensors_story WHERE IEEEaddr='"+
			Converter.Vector2HexStringNoSpaces(IEEEaddr)+"' AND sensId="+sensId+
			" AND timestamp>="+fromTime+" AND timestamp<="+toTime+";";
		MysqlDatabaseResponse result = db.query(queryText);
		if(result.success()) {
			int n=result.nlines();
			if(n>0) try {
				List<Map<String,String>> results=new LinkedList<Map<String, String>>();
				for(int i=0;i<n;i++) results.add(result.getvaluesmap().get(new Integer(i)));
				return(results);
			} catch (Exception e) {
				System.out.println("Exception caught while query: "+queryText);
				e.printStackTrace();
				return(null);
			}
		} else System.out.println("SELECT failed for query: "+queryText);
		return(null);
	}

	public static String getNodeName(int[] IEEEaddr) {
		String name=null;
		String longAddr=Converter.Vector2HexStringNoSpaces(IEEEaddr);
		MysqlDatabaseResponse result=db.query("SELECT name FROM nodes WHERE IEEEaddr='"+longAddr+"';");
		if(result.success() && result.nlines()==1) try {
			final Map<String, String> iRow = result.getvaluesmap().get(new Integer(0));
			name=iRow.get("name");
		} catch (Exception e) {
			System.out.println("Exception"+e.getClass().getName()+" caught while query for a specific blind");
			e.printStackTrace();
		}
		return(name);
	}
	
}
