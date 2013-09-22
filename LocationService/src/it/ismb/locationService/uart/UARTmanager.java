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

package it.ismb.locationService.uart;

import it.ismb.locationService.Converter;
import it.ismb.locationService.PacketDecoder;
import it.ismb.locationService.db.DBmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TooManyListenersException;
import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

public class UARTmanager extends Thread implements SerialPortEventListener {
	private static final int PING_PKT           = 0x42;
	private static final int PING_REPLY_PKT     = 0x43;
	private static InputStream inputStream;
	private static OutputStream outputStream;
	protected static SerialPort port;
	private static final int READBUFFER_SIZE = 64;
	private static final int MAX_BUFFER_SIZE = 1024;
	private static final int TIMEOUT_MAX = 5000;
	protected static final byte B_ESCAPE = 0x2f;
	protected static final byte B_BEGIN = 0x3c;
	protected static final byte B_END = 0x3e;
	private static final int TIMEOUT_MILLIS = 3000; //timeout in millisecondi 
	private static final int MAX_ATTEMPTS=10;
	private static int attempts;
	private static ToxicTimer timer;
	private static int numSentInPing;
	private static boolean trasmitting, receiving, expectingPingReply, pingJustReceived;
	private static Queue<byte []> outgoingQueue;
	private static byte[] lastSentData;
	private static boolean isUARTmanagerActive;
	
	public UARTmanager(CommPortIdentifier comPortId) throws IOException {
		isUARTmanagerActive=false;
		trasmitting=false;
		receiving=false;
		expectingPingReply=true;
		pingJustReceived=false;
		outgoingQueue = new LinkedList<byte []>();
		timer = new ToxicTimer(TIMEOUT_MILLIS);
		attempts=0;
		if(openSerialPort(comPortId)) {
			System.out.println("Port opened, now tring to ping the gateway.");
			numSentInPing=11;
			pingGateway(numSentInPing);
		} else {
			System.out.println("Can't operate on port "+comPortId.getName()+" closing UARTmanager...");
			System.gc();
		}
	}

	private synchronized boolean openSerialPort(CommPortIdentifier portId) {
		if(portId.getPortType()!=CommPortIdentifier.PORT_SERIAL) {
			System.out.println("Error: port: "+portId.getName()+" is not a serial port!");
			return false;
		}
		try {
			port=(SerialPort)portId.open("locationService",TIMEOUT_MAX);
		} catch (PortInUseException e) {
			System.out.println("Warning: The port "+portId.getName()+" was already in use by: "+ e.currentOwner);
			return false;
		}
		if(port==null) {
			System.out.println("Error: port request timed out");
			return false;
		}
		try {
			port.enableReceiveTimeout(TIMEOUT_MILLIS);
		} catch (UnsupportedCommOperationException e) {
			System.out.println("ERROR: Impossibile settare il timeout sulla seriale.");
			e.printStackTrace();
		}
		try {
			inputStream = port.getInputStream();
		} catch (IOException e1) {
			System.out.println("Error while opening the input stream to serial port "+portId.getName());
			e1.printStackTrace();
			port.close();
			return false;
		}
		try {
			outputStream = port.getOutputStream();
		} catch (IOException e1) {
			System.out.println("Error while opening the output stream to serial port "+portId.getName());
			e1.printStackTrace();
			port.close();
			try {
				inputStream.close();
			} catch (IOException e) {
				System.out.println("Error while reclosing input stream to serial port "+portId.getName());
				e.printStackTrace();
			}
			return false;
		}
		try {
			port.addEventListener(this); //c'è un this quindi questo metodo non può essere static
		} catch (TooManyListenersException e) {
			System.out.println("Too many listeners on port "+portId.getName()+": "+e.getMessage());
			try {
				inputStream.close();
				outputStream.close();
			} catch (IOException e1) {
				System.out.println("Error while reclosing IO streams on serial port "+portId.getName());
				e1.printStackTrace();
			}
			port.close();
			return false;
		}
		port.notifyOnDataAvailable(true);
		try {
			port.setSerialPortParams(38400,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
			//port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN|SerialPort.FLOWCONTROL_RTSCTS_OUT);
		} catch (UnsupportedCommOperationException e) {
			System.out.println("Operation not supported on port "+portId.getName()+": "+e.getMessage());
			port.close();
			try {
				inputStream.close();
				outputStream.close();
			} catch (IOException e1) {
				System.out.println("Error while reclosing IO streams on serial port "+portId.getName());
				e1.printStackTrace();
			}
			return false;
		}
		this.start();
		return true;
	}
	
	public static synchronized void pingGateway(int randomNum) {
		byte[] pingPkt=new byte[2];
		pingPkt[0]=PING_PKT;
		pingPkt[1]=(byte)randomNum;
		expectingPingReply=true;
		pingJustReceived=false;
		trasmitting=true;
		try {
			outputStream.write(pingPkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
		trasmitting=false;
	}

	private static byte[] enlargeBuffer(byte[] source, int suggestedMinSize) throws IOException {	
		int newBufferSize = source.length; // calculate optimal size
		do { // size growth algorithm
			newBufferSize*=2;
			if (newBufferSize > MAX_BUFFER_SIZE)
				throw new IOException("Trying to allocate buffer of size "
						+newBufferSize+" or bigger, but limit is "+MAX_BUFFER_SIZE+" bytes!");
		} while (newBufferSize < suggestedMinSize);
		System.out.println("Internal buffer enlarged from "+source.length+" bytes to "+newBufferSize+" bytes.");
		byte[] newBuffer = new byte[newBufferSize]; // new buffer creation
		System.arraycopy(source, 0, newBuffer, 0, source.length);
		return newBuffer;
	}

	public synchronized void serialEvent(SerialPortEvent event) {
			switch (event.getEventType()) {
			case SerialPortEvent.BI: //Break Interrupt
			case SerialPortEvent.OE: //Overrun Error
			case SerialPortEvent.FE: //Framing Error
			case SerialPortEvent.PE: //Parity Error
			case SerialPortEvent.CD: //Carrier Detect
			case SerialPortEvent.DSR: //Data Sent Ready
			case SerialPortEvent.RI: //Ring Indicator
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY: //Dati trasmessi output buffer vuoto
			case SerialPortEvent.CTS: break; //Clear To Send
			case SerialPortEvent.DATA_AVAILABLE: //Dati ricevuti nel buffer da leggere
				receiving=true;
				byte[] readBuffer = new byte[READBUFFER_SIZE];
				byte[] outBuffer = new byte[READBUFFER_SIZE];
				int readBytes = 0; // number of bytes read from serial port
				int outBytes = 0; // number of bytes written on the output
				boolean escapeNext = false;
				boolean insidePacket = false;
				try { // states machine implementation
					while (inputStream.available() > 0 || insidePacket) {
						readBytes=inputStream.read(readBuffer); //leggo il buffer
						//System.out.println("Read " + readBytes + " bytes of data from serial port");
						if(readBytes+outBytes>outBuffer.length) outBuffer=enlargeBuffer(outBuffer,readBytes+outBytes);
						for(int i=0;i<readBytes; i++) { // packet separation and unescaping
							if(!insidePacket) { //sono fuori dal pacchetto
								if(readBuffer[i] == B_BEGIN) { //mi aspetto il carattere di BEGIN
									outBytes = 0;
									insidePacket = true; //vado dentro
								} else System.out.println("Start ex but: "+readBuffer[i]+" found");
									//if(!searchingComPort)
									//throw new IOException("Starting token expected, but "+readBuffer[i]+" found");
							} else if(escapeNext){ // sono dentro e devo fare escape
								outBuffer[outBytes++] = readBuffer[i]; //leggo senza paranoie
								escapeNext = false; //dunque ora non dovrà più fare escape
							} else switch(readBuffer[i]) { //sono dentro e devo controllare
							case B_END: insidePacket = false; break; //end: vado fuori (non come un balcone)
							case B_ESCAPE: escapeNext = true; break; //esc: rimango dentro ma devrò fare escape
							case B_BEGIN: throw new IOException("Unexpected and unescaped starting token while still inside the packet");
							default: outBuffer[outBytes++] = readBuffer[i]; //nessun probl, leggo
							}
							if(!insidePacket) { //caso in cui ho appena finito di ricevere un paccatto
								byte[] received = Converter.subarray(outBuffer, 0, outBytes-1);
								//System.out.println("Just received packet "+ Converter.Vector2HexString(received));
								if(!expectingPingReply) {
									//System.out.println(">>>"+Converter.Vector2HexString(outBuffer));
									//System.out.println(">>>"+Converter.Vector2HexString(received));
									if(pingJustReceived) pingJustReceived=false;
									PacketDecoder.receivedPacket(received); //lo passo a PacketDecoder che saprà cosa farne
								}
								else isPingReceived(received);
							}
						} // end of the 'for' loop that cycles on the received bytes
					} // end of the 'while' loop
				} catch (Exception e) { //catch, warn, ignore better to catch ANY exception to avoid ANY kind of error
					e.printStackTrace();
					System.out.println(e.getClass().getName()+" caugth in the source: "+e.getMessage());
				}
				receiving=false;
				goAhead();
				break;
			} // end of switch
	}
	
	private static void isPingReceived(byte[] received) {
		if(received[0]==PING_REPLY_PKT) {
			int version=received[1];
			int recNum=Converter.Decode16U(received,2,false);
			if(version==2 && recNum/2==numSentInPing) {
				expectingPingReply=false;
				pingJustReceived=true;
				System.out.println("Ping received, Gateway found!");
				isUARTmanagerActive=true;
			}
		}
	}
	
	private static synchronized void sendDataToUARTnow(byte[] dataToSend) { //invia immediatamente
		trasmitting=true;
		try {
			System.out.println("Data to UART: "+Converter.Vector2HexString(dataToSend));
			outputStream.write(dataToSend);
			lastSentData=dataToSend;
			timer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		trasmitting=false;
	}
	
	public static void sendDataToUART(byte[] dataToSend) { //mette in coda per inviare
		if(!trasmitting && !receiving && isUARTmanagerActive && !pingJustReceived) {
			System.out.println("####senData trasm:"+trasmitting+" rec:"+receiving+" isActive:"+isUARTmanagerActive+" expectingPingReply:"+expectingPingReply);
			if(!outgoingQueue.isEmpty()) {
				System.out.println("Data to QUEUE: "+Converter.Vector2HexString(dataToSend));
				outgoingQueue.add(dataToSend);
			} else sendDataToUARTnow(dataToSend);
		}
		else {
			if(pingJustReceived) pingJustReceived=false;
			System.out.println("Data to QUEUE: "+Converter.Vector2HexString(dataToSend));
			outgoingQueue.add(dataToSend);
		}
	}
	
	public static void goAhead() {
		if(!trasmitting && !receiving && isUARTmanagerActive)
			if(!outgoingQueue.isEmpty()) {
				System.out.println("####GOAHEAD!! trasm:"+trasmitting+" rec:"+receiving+" isActive:"+isUARTmanagerActive+
						" expectingPingReply:"+expectingPingReply+" queueEmpty:"+outgoingQueue.isEmpty());
				sendDataToUARTnow(outgoingQueue.poll());
			}
	}
	
	public static void resend() { //da sistemare
		attempts++;
		if(attempts<MAX_ATTEMPTS) {
			sendDataToUARTnow(lastSentData);
			
		} else {
			System.out.println("ERRORE: Ho provato a mandare: "+Converter.byte2hex(lastSentData)+" per "+MAX_ATTEMPTS+
					"volte senza successo, ci rinuncio...");
			attempts=0;
			goAhead();
		}
	}
	
	public static void skip() {
		int lastReq=getLastReqType();
		if(lastReq==PacketDecoder.IEEE_ANNOUNCE_REQUEST) {
			int[] addr=getLastIEEEaddrAnnonunceRequest();
			String name=DBmanager.getNodeName(addr);
			System.err.println("ERROR: node '"+name+"' [IEEE:"+Converter.Vector2HexStringNoSpaces(addr)+"] doesn't reply to announce request, please check it!");
		} else 
			System.out.println("WARNING: no reply received for request type: "+lastReq+" from NTWaddr: "+getLastReqDest());
		goAhead();
	}
	
	public static boolean isWaiting() {
		return timer.isWaiting();
	}
	
	public static int getLastReqType() {
		if(lastSentData.length>=1) return Converter.byte2int(lastSentData[0],false);
		else return 0;
	}
	
	public static int getLastReqDest() {
		if(lastSentData.length>=3) return Converter.Decode16U(lastSentData,1,false);
		else return 0;
	}
	
	public static int[] getLastIEEEaddrAnnonunceRequest() {
		if(lastSentData.length==9){
			int[] ieeeAddr =new int[8];
			for(int i=0;i<8;i++) ieeeAddr[i]=(int)lastSentData[i+1];
			return ieeeAddr;
		}
		else return null;
	}
	
	public static void resetTimer() {
		timer.interrupt();
	}
	
	public static synchronized void Stop() {
		isUARTmanagerActive=false;
		expectingPingReply=false;
		pingJustReceived=false;
		outgoingQueue.clear();
		try {
			outputStream.close();
			inputStream.close();
		} catch (IOException e) {
			System.out.println("Error while reclosing IO streams on serial port.");
			e.printStackTrace();
		}
		trasmitting=false;
		receiving=false;
		port.close();
	}

	public static boolean isActive() {
		return isUARTmanagerActive;
	}
	
	private class ToxicTimer extends Thread {
		private long length; // Length of timeout
		private volatile Thread timerThread;

		public ToxicTimer(long length) {
			this.length = length; // Assign to member variable
		}
		
		public void start() {
			this.timerThread = new Thread(this);
			timerThread.start();
		}
		
		public void interrupt() {
			if(this.timerThread!=null) this.timerThread.interrupt();
		}
		
		public boolean isWaiting() {
			return(this.timerThread!=null);
		}
		
		public void run() {
	        while(this.timerThread==Thread.currentThread()) {
	        	try { // Put the timer to sleep
	        		Thread.sleep(this.length); //aspetta
	        		synchronized (this) { // Use 'synchronized' to prevent conflicts
	        			System.out.println("WARNING: Network timeout occurred.");
	        			UARTmanager.skip();
	        			timerThread=null;
	        			//UARTmanager.resend(); //fare qualcosa di più furbo!!!!!!!
	        		}
	        	} catch (InterruptedException ioe) {
	        		timerThread=null;
	        	}
	        }
		}

	}

}
