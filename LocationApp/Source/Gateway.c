//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : Gateway.c
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

//INCLUDES
#include "AF.h"
#include "LocationApp.h"
#include "hal_key.h"
#if defined ( LCD_SUPPORTED )
  #include "hal_lcd.h"
#endif
#include "hal_led.h"
#include "Gateway.h"
#if !defined ( MT_TASK )
  #error      // No need for this module if MT_TASK isn't defined
#endif
#if defined ( ZBIT )
  #error //ZBIT non lo vogliamo
#endif
#include "SPIMgr.h"

//CONSTANTS
#define LOCGATEWAY_TX_OPTIONS          AF_MSG_ACK_REQUEST

static const cId_t Gateway_InputClusterList[] = {
  BLIND_XY_RESPONSE,
  BLIND_RSSI_RESPONSE,
  SENS_PKT,
  ACK_REF_CONFIG,
  ACK_BLIND_CONFIG,
  REF_ANNCE,
  BLIND_ANNCE
};

static const cId_t Gateway_OutputClusterList[] = {
  BLIND_XY_REQUEST,
  BLIND_RSSI_REQUEST,
  SENS_REQUEST,
  REF_CONFIG,
  BLIND_CONFIG,
  LOCATION_REFNODE_REQUEST_CONFIG,
  LOCATION_BLINDNODE_REQUEST_CONFIG,
  GET_ANNOUNCE,
  GET_IEEE_ANNOUNCE
};

static const SimpleDescriptionFormat_t Gateway_SimpleDesc = {
  LOCATION_GATEWAY_ENDPOINT,
  LOCATION_PROFID,
  LOCATION_GATEWAY_DEVICEID,
  LOCATION_DEVICE_VERSION,
  LOCATION_FLAGS,
  sizeof(Gateway_InputClusterList),
  (cId_t *)Gateway_InputClusterList,
  sizeof(Gateway_OutputClusterList),
  (cId_t *)Gateway_OutputClusterList
};

static const endPointDesc_t epDesc = {
  LOCATION_GATEWAY_ENDPOINT,
  &Gateway_TaskID,
  (SimpleDescriptionFormat_t *)&Gateway_SimpleDesc,
  noLatencyReqs
};

//GLOBAL VARIABLES
uint8 Gateway_TaskID;

//LOCAL VARIABLES
uint8 Gateway_TransID; //This is the unique message ID (counter)

//LOCAL FUNCTIONS
static void Gateway_ProcessRFmsg( afIncomingMSGPacket_t *pckt );
static void rxCB(uint8 port, uint8 event);
uint8 insertDelimitersAndEscChar(byte sPktByte[], byte sPktByteFinal[], byte numBytesIn);
void processSerialMsg(uint8 len, uint8 *msg);

/* @fn      Gateway_Init
 * @brief   Initialization function for the Generic App Task.
 *          This is called during initialization and should contain
 *          any application specific initialization (ie. hardware
 *          initialization/setup, table initialization, power up
 *          notificaiton ... ).
 * @param   task_id - the ID assigned by OSAL.  This ID should be
 *                    used to send messages and set timers.
 * @return  none*/
void Gateway_Init( uint8 task_id ) {
  Gateway_TaskID = task_id;
  Gateway_TransID = 0;
  afRegister((endPointDesc_t *)&epDesc); // Register the endpoint/interface
  // Register for all key events - This app will handle all key events
  // RegisterForKeys( Gateway_TaskID );
  halUARTCfg_t uartConfig; //si configura la porta seriale
  uartConfig.configured           = TRUE;            // 2430 don't care.
  uartConfig.baudRate             = GATEWAY_BAUD;
  uartConfig.flowControl          = 0;//TRUE;
  uartConfig.flowControlThreshold = GATEWAY_THRESH;
  uartConfig.rx.maxBufSize        = GATEWAY_RX_MAX;
  uartConfig.tx.maxBufSize        = GATEWAY_TX_MAX;
  uartConfig.idleTimeout          = GATEWAY_IDLE;   // 2430 don't care.
  uartConfig.intEnable            = TRUE;           // 2430 don't care.
  uartConfig.callBackFunc         = rxCB; //funzione che riceve i dati
  HalUARTOpen (SERIAL_PORT, &uartConfig); //si apre la porta seriale
  #if defined ( LCD_SUPPORTED ) // Update the display
    HalLcdWriteString("Location-Gateway", HAL_LCD_LINE_2);
  #endif
}

/* @fn      Gateway_ProcessEvent
 * @brief   Generic Application Task event processor.  This function
 *          is called to process all events for the task.  Events
 *          include timers, messages and any other user defined events.
 * @param   task_id  - The OSAL assigned task ID.
 * @param   events - events to process.  This is a bit map and can
 *                   contain more than one event.
 * @return  none*/
UINT16 Gateway_ProcessEvent( uint8 task_id, UINT16 events ) {
  afIncomingMSGPacket_t *MSGpkt;
  if(events & SYS_EVENT_MSG) {
    MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive(Gateway_TaskID);
    while(MSGpkt) {
      switch(MSGpkt->hdr.event) {
        case AF_INCOMING_MSG_CMD: //Messaggo da etere
          Gateway_ProcessRFmsg(MSGpkt);
          break;
        case ZDO_CB_MSG:
          //Gateway_ProcessZDOmsg((zdoIncomingMsg_t *)MSGpkt);
          break;
        case KEY_CHANGE:
          //keyChange( ((keyChange_t *)MSGpkt)->state, ((keyChange_t *)MSGpkt)->keys );
          break;
        default:
          break;
      }
      osal_msg_deallocate((uint8 *)MSGpkt);
      MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive( Gateway_TaskID );
    }
    return ( events ^ SYS_EVENT_MSG ); //Return unprocessed events
  }
  return 0; // Discard unknown events
}

//Event Generation Functions
/* @fn      keyChange
 * @brief   Handles all key events for this device.
 * @param   keys - bit field for key events. Valid entries:
 *                 EVAL_SW4
 *                 EVAL_SW3
 *                 EVAL_SW2
 *                 EVAL_SW1
 * @return  none*/
void LocationHandleKeys( uint8 keys ) {
  if(keys & HAL_KEY_SW_1) {}
  if(keys & HAL_KEY_SW_2) {}
  if(keys & HAL_KEY_SW_3) {}
  if(keys & HAL_KEY_SW_4) {}
}

static void Gateway_ProcessRFmsg(afIncomingMSGPacket_t *pkt) {
  uint8 toSerial=0;
  switch(pkt->clusterId) { //Identifies a message
  case REF_ANNCE: //send node announce to uart (to PC)
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: RefNode","announce rcvd");
    #endif
    break;
  case BLIND_XY_RESPONSE:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: XY packet","received");
    #endif
    break;
  case BLIND_RSSI_RESPONSE:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: RSSI packet","received");
    #endif
    break;
  case BLIND_ANNCE:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: BlindNode","announce rcvd");
    #endif
    break;
  case ACK_REF_CONFIG:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: ACK Config","RefNode rcvd");
    #endif
    break;
  case ACK_BLIND_CONFIG:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: ACK Config","BlindNode rcvd");
    #endif
    break;
  case SENS_PKT:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: Sens packet","received");
    #endif
    break;
  case DIAGNOSTIC_PKT:
    toSerial=1;
    #if defined ( LCD_SUPPORTED )
    HalLcdWriteScreen("RF: Diagnostic","packet rcvd");
    #endif
    break;
  default:
    //#if defined ( LCD_SUPPORTED )
    //HalLcdWriteScreen("RF: Unknown","packet rcvd");
    //#endif
    break;
  }
  if(toSerial==1) { //If the data has to be sent over the serial line
    uint8 i;
    byte numBytesIn, numBytesOut;
    byte vectByteIn[NUM_MAX_OF_BYTES];
    byte vectByteOut[NUM_MAX_OF_BYTES*2+6];
    numBytesIn=(pkt->cmd.DataLength)+3; //add the NetwAddr and type: 3 byte
    vectByteIn[0]=LO_UINT16(pkt->clusterId);
    vectByteIn[1]=HI_UINT16(pkt->srcAddr.addr.shortAddr); //insert the netw addr
    vectByteIn[2]=LO_UINT16(pkt->srcAddr.addr.shortAddr); //insert the netw addr
    for(i=0;i<pkt->cmd.DataLength;i++) vectByteIn[i+3]=pkt->cmd.Data[i];
    numBytesOut=insertDelimitersAndEscChar(vectByteIn,vectByteOut,numBytesIn);
    HalUARTWrite(SERIAL_PORT,vectByteOut,numBytesOut);
  }
}

uint8 insertDelimitersAndEscChar(byte sPktByte[], byte sPktByteFinal[], byte numBytesIn) {
  uint8 idx, idxF;
  sPktByteFinal[0] = INIT_CHAR_PKT; //insert initial delimiter
  idxF=1;
  for(idx=0; idx < numBytesIn; idx++) {
    if(sPktByte[idx]==INIT_CHAR_PKT || sPktByte[idx]==END_CHAR_PKT || sPktByte[idx]==ESC_CHAR_PKT) {
      sPktByteFinal[idxF] = ESC_CHAR_PKT;
      idxF++;
      sPktByteFinal[idxF] = sPktByte[idx];
    } else sPktByteFinal[idxF] = sPktByte[idx];
    idxF++;
  }
  sPktByteFinal[idxF] = END_CHAR_PKT; //insert final delimiter
  idxF++;
  return idxF; //return the total number of bytes contained in the pkt
}

void processSerialMsg(uint8 len, uint8 *msg) {
  afAddrType_t dstAddr;
  uint8 typeOfCommand=msg[0];
  switch(typeOfCommand) { // Switch de comandi ricevuti da seriale
    case TYPE_GET_IEEE_ANNOUNCE: //Request the announce for a specific IEEE node
      if(len==9) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: IEEE Annce","request rcvd");
        #endif
        byte searchKey[8];
        for(int i=0;i<8;i++) searchKey[i]=msg[i+1];
        dstAddr.endPoint = LOCATION_ALL_NODES_ENDPOINT;
        dstAddr.addrMode = afAddrBroadcast;
        dstAddr.addr.shortAddr = NWK_BROADCAST_SHORTADDR_DEVALL;
        AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,GET_IEEE_ANNOUNCE,8,
                       searchKey,&Gateway_TransID, 0, AF_DEFAULT_RADIUS);
      }
      break;
    case TYPE_GET_ANNOUNCE: //Request the announce for a specific NWK node
      if(len==3) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: Announce","request rcvd");
        #endif
        dstAddr.endPoint = LOCATION_ALL_NODES_ENDPOINT;
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,GET_ANNOUNCE,0,
                       NULL,&Gateway_TransID, 0, AF_DEFAULT_RADIUS);
      }
      break;
    case TYPE_REF_CONFIG:
      if(len==14) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: RefNode","Config rcvd");
        #endif
        dstAddr.endPoint = LOCATION_REFNODE_ENDPOINT;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        byte refConfigMsg[11];
        for(int i=0;i<11;i++) refConfigMsg[i]=msg[i+3];
        if(AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,REF_CONFIG,11,
              refConfigMsg,&Gateway_TransID,0,AF_DEFAULT_RADIUS)== ZSuccess) {
          HalLedBlink(SECONDARY_LED, 1, 90, 90); //indication that msg went out
          osal_start_timerEx(Gateway_TaskID,GATEWAY_RSP_LOST_EVT,GATEWAY_RSP_LOST_TIMEOUT); //Wait for reply from receiving device
        }
        else osal_start_timerEx(Gateway_TaskID,GATEWAY_MSG_RTRY_EVT, GATEWAY_MSG_RTRY_TIMEOUT); //Wait to retry sending OTA message
      }
      break;
    case TYPE_BLIND_CONFIG:
      if(len==9) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: BlindNode","Config rcvd");
        #endif
        dstAddr.endPoint = LOCATION_BLINDNODE_ENDPOINT;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        byte blindConfigMsg[6];
        for(int i=0;i<6;i++) blindConfigMsg[i]=msg[i+3];
        if(AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,BLIND_CONFIG,6,
             blindConfigMsg,&Gateway_TransID,0,AF_DEFAULT_RADIUS)== ZSuccess) {
        HalLedBlink(SECONDARY_LED, 1, 90, 90); //Visual indication that message went out
        osal_start_timerEx(Gateway_TaskID,GATEWAY_RSP_LOST_EVT,GATEWAY_RSP_LOST_TIMEOUT); //Wait for reply from receiving device
        }
        else osal_start_timerEx(Gateway_TaskID,GATEWAY_MSG_RTRY_EVT, GATEWAY_MSG_RTRY_TIMEOUT); //Wait to retry sending OTA message
      }
      break;
    case TYPE_GET_DIAGNOSTIC: //Request battery value and RSSI parent value for a specific node
      if(len==3) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: Diagnostic","request rcvd");
        #endif
        dstAddr.endPoint = LOCATION_ALL_NODES_ENDPOINT;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        if(AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,GET_DIAGNOSTIC,0,
               NULL,&Gateway_TransID, 0, AF_DEFAULT_RADIUS)== ZSuccess) {
          HalLedBlink(SECONDARY_LED, 1, 90, 90); //Visual indication that message went out
          osal_start_timerEx(Gateway_TaskID,GATEWAY_RSP_LOST_EVT,GATEWAY_RSP_LOST_TIMEOUT); //Wait for reply from receiving device
        }
        else osal_start_timerEx(Gateway_TaskID,GATEWAY_MSG_RTRY_EVT, GATEWAY_MSG_RTRY_TIMEOUT); //Wait to retry sending OTA message
      }
      break;
    case TYPE_GET_PING: //Request a ping on the serial line
      if(len==2) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: Ping","request rcvd");
        #endif
        byte pingReply[4];
        uint8 num;
        uint16 num2;
        num=msg[1];
        num2=num*2;//si fa qualcosa di definito con il numero ricevuto
        pingReply[0] = TYPE_PING_REPLY; // copy the type of the msg
        pingReply[1] = LOCATION_APP_VERSION;
        pingReply[2] = HI_UINT16(num2);
        pingReply[3] = LO_UINT16(num2);
        byte vectByteOut[10];
        uint8 numBytesOut;
        numBytesOut = insertDelimitersAndEscChar(pingReply, vectByteOut, 4);
        HalUARTWrite(SERIAL_PORT,vectByteOut,numBytesOut);
        HalLedBlink(SECONDARY_LED, 1, 90, 90); //Visual indication that message went out
      }
      break;
    case TYPE_RSSI_REQ:
      if(len==3) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: RSSI","request rcvd");
        #endif
        dstAddr.endPoint = LOCATION_BLINDNODE_ENDPOINT;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        if(AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,BLIND_RSSI_REQUEST,0,
                       NULL,&Gateway_TransID,0,AF_DEFAULT_RADIUS)==ZSuccess) {
          HalLedBlink(SECONDARY_LED, 1, 90, 90); //Visual indication that message went out
          osal_start_timerEx(Gateway_TaskID,GATEWAY_RSP_LOST_EVT,GATEWAY_RSP_LOST_TIMEOUT); //Wait for reply from receiving device
        }
        else osal_start_timerEx(Gateway_TaskID,GATEWAY_MSG_RTRY_EVT, GATEWAY_MSG_RTRY_TIMEOUT); //Wait to retry sending OTA message
      }
      break;
    case TYPE_XY_REQ:
      if(len==3) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: XY","request rcvd");
        #endif
        dstAddr.endPoint = LOCATION_BLINDNODE_ENDPOINT;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        if(AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,BLIND_XY_REQUEST,0,
                       NULL,&Gateway_TransID,0,AF_DEFAULT_RADIUS)==ZSuccess) {
          HalLedBlink(SECONDARY_LED, 1, 90, 90); //Visual indication that message went out
          osal_start_timerEx(Gateway_TaskID,GATEWAY_RSP_LOST_EVT,GATEWAY_RSP_LOST_TIMEOUT); //Wait for reply from receiving device
        }
        else osal_start_timerEx(Gateway_TaskID,GATEWAY_MSG_RTRY_EVT, GATEWAY_MSG_RTRY_TIMEOUT); //Wait to retry sending OTA message
      }
      break;
    case TYPE_GET_SENS:
      if(len==3) {
        #if defined ( LCD_SUPPORTED )
          HalLcdWriteScreen("UART: Sensors","request rcvd");
        #endif
        dstAddr.endPoint = LOCATION_REFNODE_ENDPOINT;
        dstAddr.addr.shortAddr=BUILD_UINT16(msg[2],msg[1]);
        dstAddr.addrMode = (afAddrMode_t)Addr16Bit;
        if(AF_DataRequest(&dstAddr,(endPointDesc_t*)&epDesc,SENS_REQUEST,0,NULL,
                          &Gateway_TransID, 0, AF_DEFAULT_RADIUS)== ZSuccess) {
          HalLedBlink(SECONDARY_LED, 1, 90, 90); //Visual indication that message went out
          osal_start_timerEx(Gateway_TaskID,GATEWAY_RSP_LOST_EVT,GATEWAY_RSP_LOST_TIMEOUT); //Wait for reply from receiving device
        }
        else osal_start_timerEx(Gateway_TaskID,GATEWAY_MSG_RTRY_EVT, GATEWAY_MSG_RTRY_TIMEOUT); //Wait to retry sending OTA message
      }
      break;
    default:
      #if defined ( LCD_SUPPORTED )
        HalLcdWriteScreen("UART: Unknown","packet rcvd");
      #endif
      break;
  }
}

/* @fn      rxCB
 * @brief   Process UART Rx event handling.
 *          May be triggered by an Rx timer expiration - less than max
 *          Rx bytes have arrived within the Rx max age time.
 *          May be set by failure to alloc max Rx byte-buffer for the DMA Rx -
 *          system resources are too low, so set flow control?
 * @param   none
 * @return  none*/
static void rxCB(uint8 port, uint8 event) {
  uint8 *buf, len;
  if (!(buf = osal_mem_alloc(GATEWAY_RX_CNT))) return;
  /* HAL UART Manager will turn flow control back on if it can after read.
   * Reserve 1 byte for the 'sequence number'.*/
  len = HalUARTRead(port,buf,GATEWAY_RX_CNT);
  if(!len) { // Length is not expected to ever be zero.
    osal_mem_free( buf );
    return;
  }
  processSerialMsg(len,buf);
}
