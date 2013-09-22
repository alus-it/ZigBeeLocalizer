//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : BlindNode.c
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
#include "OSAL.h"
#include "OSAL_NV.h"
#include "MTEL.h"
#include "AF.h"
#include "ZDApp.h"
#include "OnBoard.h"
#include "hal_key.h"
#if defined ( LCD_SUPPORTED )
  #include "hal_lcd.h"
#endif
#include "LocationApp.h"
#include "BlindNode.h"
#include "LocationEngine.h"
#include "SensorID.h"

#if !defined ( CC2431 )
  #error    // Must be defined
#endif

//CONSTANTS
#define BLINDNODE_MAX_REF_NODES    20
#define BLINDNODE_MIN_REF_NODES     3
#define BLINDNODE_FIND_DELAY       20   // BN_TIME_INCR
#define BLINDNODE_WAIT_TIMEOUT      2   // BN_TIME_INCR
#define BLINDNODE_BLAST_DELAY      20   // Milliseconds

/* Location Engine requires RSSI as dBm limited to -95 dBm to -40 dBm.
 * Weaker than -95 dBm must not be used.
 * Stronger than -40 dBm must be truncated and then passed as -40 dBm.
 * Values must be passed as absolute value: 40 strong to 95 weak.*/
#define LOC_ENGINE_MIN_DBM  95
#define LOC_ENGINE_MAX_DBM  40

// [ Loc = (Loc * (N-1) + New-Calc) / N ]  Set to 1 for no filtering.
#define BLINDNODE_FILTER      2
// If newly calculated pos differs by more than M meters, flush filter and
// restart. 10m * 4 = 40
#define BLINDNODE_FLUSH      40

//TYPEDEFS
typedef struct {
  uint16 timeout; //In decimi di secondo - time to collect ref node responses
  uint16 cycle;   //In decimi di secondo - For auto mode
  uint8  mode; //auto o polled mode
  uint8 localizationType; //Calculate and sends the XY or only the measured RSSI
  LocDevCfg_t loc; //Data for the location engine
} BN_Cfg_t;

typedef enum {
  eBnIdle,
  eBnBlastOut,
  eBnBlastIn,
  eBnBlastOff
} eBN_States_t;

typedef struct {
  uint16 x;
  uint16 y;
  uint16 addr;
  uint8 rssiUp; //average of RSSI measured on blasts from blind to ref
  uint8 sigma2; //sigma^2 of the RSSI average
  uint8 rssiDown; //RSSI of the answer from the ref to blind
  uint16 room;
  byte a;
  byte n;
} RefNode_t;

//GLOBAL VARIABLES
uint8 BlindNode_TaskID;

//LOCAL VARIABLES
static const cId_t BlindNode_InputClusterList[] = {
  RSSI_RESPONSE,
  BLIND_RSSI_REQUEST,
  BLIND_XY_REQUEST,
  BLIND_CONFIG,
  GET_ANNOUNCE,
  GET_IEEE_ANNOUNCE,
  GET_DIAGNOSTIC
};

static const cId_t BlindNode_OutputClusterList[] = {
  RSSI_REQUEST,
  BLIND_XY_RESPONSE,
  BLIND_RSSI_RESPONSE,
  LOCATION_RSSI_BLAST,
  BLIND_ANNCE,
  DIAGNOSTIC_PKT,
  ACK_BLIND_CONFIG
};

static const SimpleDescriptionFormat_t BlindNode_SimpleDesc = {
  LOCATION_BLINDNODE_ENDPOINT,
  LOCATION_PROFID,
  LOCATION_BLINDNODE_DEVICEID,
  LOCATION_DEVICE_VERSION,
  LOCATION_FLAGS,
  sizeof(BlindNode_InputClusterList),(cId_t*)BlindNode_InputClusterList,
  sizeof(BlindNode_OutputClusterList),(cId_t*)BlindNode_OutputClusterList
};

static const endPointDesc_t epDesc = {
  LOCATION_BLINDNODE_ENDPOINT,
  &BlindNode_TaskID,
  (SimpleDescriptionFormat_t *)&BlindNode_SimpleDesc,
  noLatencyReqs
};

static BN_Cfg_t config;
static eBN_States_t state;
static RefNode_t refNodes[BLINDNODE_MAX_REF_NODES];
static uint8 transId;
static afAddrType_t gatewayAddr, broadcastRefsAddr;
#if defined( BN_DISPLAY_TEST ) && defined( MT_TASK )
  #define PRINTBUFSIZE 80
  uint8 printBuf[PRINTBUFSIZE];
#endif
static uint8 blastCnt; //blast rimanenti
static byte *annceMsg; //messaggio di annuncio
static byte AnnceMsgLen; //lunghezza messaggio di annuncio
static uint8* ieeeAddr; //IEEE address
static uint8 rspCnt;   // Count of unique XY_RSSI_RESPONSE messages after blast.
static uint32 xOld, yOld;
static uint8 singleRequest, numOfSens, sensIds[NUM_MAX_SENS];
static uint8 newConfig, newTempConfig[6]; //conf temporanea

//LOCAL FUNCTIONS PROTOTYPES
static void processMSGCmd(afIncomingMSGPacket_t *pckt);
static void parseConfig(uint8 *msg);
static afStatus_t sendXYrsp(void);
static afStatus_t sendRSSIrsp(void);
static void finishCollection(void);
static void calcOffsets(RefNode_t *ref, uint16 *xOff, uint16 *yOff);
static RefNode_t *findBestRSSI(RefNode_t *ref);
static uint8 sortNodes(RefNode_t *refList, uint16 ofRoom);
static void setLogicals(LocRefNode_t *loc,RefNode_t *ref,uint16 xOff,uint16 yOff);
static void startBlast(void);
static void processAnnounceRequest(byte *reqIEEEaddr);
static uint8 checkRSSIforLocationEngine(uint8 rssi);
static void sendDiagnosticPkt(uint8 parentRSSI);

void BlindNode_Init(uint8 task_id) { //Initialization function for the Task
  BlindNode_TaskID = task_id;
  state = eBnIdle;
  if(ZSUCCESS == osal_nv_item_init(LOC_NV_BLINDNODE_CONFIG,sizeof(BN_Cfg_t),&config))
    osal_nv_read( LOC_NV_BLINDNODE_CONFIG, 0, sizeof( BN_Cfg_t ), &config );
  else { //se non c'è la config nella memoria NV imposto la config di default
    config.mode = NODE_MODE_POLLED;
    config.timeout = BLINDNODE_WAIT_TIMEOUT;
    config.cycle = BLINDNODE_FIND_DELAY;
    config.localizationType=RSP_TYPE_RSSI;
    config.loc.param_a = LOC_DEFAULT_A;
    config.loc.param_n = LOC_DEFAULT_N;
    osal_nv_write( LOC_NV_BLINDNODE_CONFIG, 0, sizeof( BN_Cfg_t ), &config );
  }
  ieeeAddr = NLME_GetExtAddr();
  afRegister((endPointDesc_t *)&epDesc);
  //RegisterForKeys( BlindNode_TaskID );
  #if defined ( LCD_SUPPORTED )
    HalLcdWriteString( "Location-Blind", HAL_LCD_LINE_1 );
  #endif
  numOfSens=findVectAvblSensId(sensIds);
  byte idx,i;
  AnnceMsgLen=15+numOfSens;
  annceMsg=(byte*)osal_mem_alloc(AnnceMsgLen);
  annceMsg[0]=config.mode;
  annceMsg[1]=HI_UINT16(config.timeout);
  annceMsg[2]=LO_UINT16(config.timeout);
  annceMsg[3]=HI_UINT16(config.cycle);
  annceMsg[4]=LO_UINT16(config.cycle);
  annceMsg[5]=config.localizationType;
  annceMsg[6]=numOfSens;
  for(i=0,idx=7;i<numOfSens;i++,idx++) annceMsg[idx]=sensIds[i];
  for(i=0;i<8;i++,idx++) annceMsg[idx]=ieeeAddr[i];
  broadcastRefsAddr.addrMode = afAddrBroadcast;
  broadcastRefsAddr.addr.shortAddr = NWK_BROADCAST_SHORTADDR_DEVALL;
  broadcastRefsAddr.endPoint = LOCATION_REFNODE_ENDPOINT;
  gatewayAddr.addrMode = afAddr16Bit;
  gatewayAddr.addr.shortAddr = 0; //Il gateway è anche coordinatore
  gatewayAddr.endPoint = LOCATION_GATEWAY_ENDPOINT;
  AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc, BLIND_ANNCE,
                 AnnceMsgLen,annceMsg, &transId, 0, AF_DEFAULT_RADIUS);
  if(config.mode==NODE_MODE_AUTO && config.cycle>0)
    osal_start_timerEx(BlindNode_TaskID, BLINDNODE_FIND_EVT, config.cycle*TIME_UNIT);
}

uint16 BlindNode_ProcessEvent(uint8 task_id, uint16 events) { //Task event proc
  if(events & SYS_EVENT_MSG) {
    afIncomingMSGPacket_t *MSGpkt;
    MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive(BlindNode_TaskID);
    while( MSGpkt ) {
      switch ( MSGpkt->hdr.event ) {
      case KEY_CHANGE:
        //handleKeys( ((keyChange_t *)MSGpkt)->state, ((keyChange_t *)MSGpkt)->keys );
        break;
      case AF_DATA_CONFIRM_CMD:
        #if !defined( RTR_NWK )
        { //This message is received as a confirmation of a data packet sent.
          //The status is of ZStatus_t type [defined in ZComDef.h]
        afDataConfirm_t *afDataConfirm = (afDataConfirm_t *)MSGpkt;
        if ( afDataConfirm->hdr.status != ZSuccess )
          /* No ACK from the MAC layer implies that mobile device is out of
           * range of most recent parent. Therefore, begin an orphan scan
           * to try to find a former parent.
           * NOTE: To get the fastest action in the process of finding a new
           * parent, set the MAX_JOIN_ATTEMPTS in ZDApp.c to 1.*/
          if(afDataConfirm->hdr.status == ZMacNoACK) LoacationApp_NoACK();
          else{} // Some other error -- Do something.
        }
        #endif
        break;
      case AF_INCOMING_MSG_CMD:
        processMSGCmd(MSGpkt);
        break;
      case ZDO_STATE_CHANGE:
        #if defined( RTR_NWK )
        NLME_PermitJoiningRequest(0);
        #endif
        /* Broadcast the X,Y location for any passive listeners in order to
         * register this node.*/
        osal_start_timerEx(BlindNode_TaskID,BLINDNODE_FIND_EVT,BLINDNODE_FIND_DELAY);
        break;
      default:
        break;
      }
      osal_msg_deallocate( (uint8 *)MSGpkt );
      MSGpkt=(afIncomingMSGPacket_t *)osal_msg_receive(BlindNode_TaskID);
    }
    return(events ^ SYS_EVENT_MSG);  // Return unprocessed events.
  }
  if(events & BLINDNODE_BLAST_EVT) { //è ora di mandare un blast
    if(blastCnt==0) { //blast terminati
      state = eBnBlastOff; //stato fine blast
      finishCollection(); //mi riorganizzo i dati ricevuti
    } else { //altrimenti blast ancora da terminare
      uint8 stat, delay;
      if(--blastCnt==0) { //se era l'ultimo chiedo i blast ai refNodes
        stat=AF_DataRequest(&broadcastRefsAddr, (endPointDesc_t *)&epDesc,
          RSSI_REQUEST, 0, NULL, &transId, AF_SKIP_ROUTING, 1);
        state = eBnBlastIn; //stato blast in ingresso dai refNodes
        delay = config.timeout*TIME_UNIT; //timeout in ms di attesa dei blast
      } else { //altrimenti blast
        stat = AF_DataRequest(&broadcastRefsAddr, (endPointDesc_t *)&epDesc,
          LOCATION_RSSI_BLAST, 0, NULL, &transId, AF_SKIP_ROUTING, 1);
        delay = BLINDNODE_BLAST_DELAY;
      }
      if(stat!=afStatus_SUCCESS) blastCnt++; //se fallisce devo rifare
      osal_start_timerEx(BlindNode_TaskID, BLINDNODE_BLAST_EVT, delay); //next
    }
    return(events^BLINDNODE_BLAST_EVT);
  }
  if(events & BLINDNODE_FIND_EVT) { //richiesta misure e calcolo posizione
    if(state==eBnIdle) startBlast(); //inizio invio blast
    return (events ^ BLINDNODE_FIND_EVT);
  }
  if(events & BLINDNODE_WAIT_EVT) {
    LoacationApp_Sleep(TRUE);
    return (events ^ BLINDNODE_WAIT_EVT);
  }
  return 0; //Discard unknown events.
}

void LocationHandleKeys( uint8 keys ) { //Handles all key events for this device
  if(keys & HAL_KEY_SW_1) {}
  if(keys & HAL_KEY_SW_2) {}
  if(keys & HAL_KEY_SW_3) osal_set_event(BlindNode_TaskID,BLINDNODE_FIND_EVT);
  if(keys & HAL_KEY_SW_4) if(state == eBnIdle) startBlast();
}

static void processMSGCmd(afIncomingMSGPacket_t *pkt) { //msg processor callback
  switch ( pkt->clusterId ) {
    case GET_IEEE_ANNOUNCE: //richiesta annuncio tramite IEEEaddr
      processAnnounceRequest(pkt->cmd.Data);
      break;
    case GET_ANNOUNCE: //richiesta annuncio
        AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc,
                 BLIND_ANNCE,AnnceMsgLen,annceMsg,&transId,0,AF_DEFAULT_RADIUS);
      break;
    case BLIND_XY_REQUEST: //richiesta di localizzazione XY
      if(state==eBnIdle) {
        singleRequest=RSP_TYPE_XY;
        startBlast(); //inizio a mandare blast
      }
      break;
    case BLIND_RSSI_REQUEST: //richiesta di misure RSSI
      if(state==eBnIdle) {
        singleRequest=RSP_TYPE_RSSI;
        startBlast(); //inizio a mandare blast
      }
      break;
    case BLIND_CONFIG: //nuova configurazione
      if(state==eBnIdle) parseConfig(pkt->cmd.Data);//in idle riconfiguro subito
      else { //altrimenti riconfiguro alla fine delle misure correnti
        newConfig=1;
        for(int i=0;i<6;i++) newTempConfig[i]=pkt->cmd.Data[i];
      } //sarebbe il caso di fermare il tutto...
      break;
    case RSSI_RESPONSE: //RSSI ricevuto da RefNode
      if(state == eBnBlastIn) //se mi aspetto le risposte RSSI dai refNodes
        for(uint8 idx=0;idx<BLINDNODE_MAX_REF_NODES;idx++) //for per cercare
          if(refNodes[idx].addr==INVALID_NODE_ADDR || //se indirizzo INVALID
            refNodes[idx].addr==pkt->srcAddr.addr.shortAddr) { //o trovato
            refNodes[idx].addr=pkt->srcAddr.addr.shortAddr; //indirizzo
            refNodes[idx].x=BUILD_UINT16( //X
                                  pkt->cmd.Data[RSSI_RESP_X_LO_IDX],
                                  pkt->cmd.Data[RSSI_RESP_X_HI_IDX]);
            refNodes[idx].y=BUILD_UINT16( //Y
                                  pkt->cmd.Data[RSSI_RESP_Y_LO_IDX],
                                  pkt->cmd.Data[RSSI_RESP_Y_HI_IDX]);
            refNodes[idx].rssiUp=pkt->cmd.Data[RSSI_RESP_RSSI_IDX]; //RSSI
            refNodes[idx].sigma2=pkt->cmd.Data[RSSI_RESP_SIGMA2_IDX]; //sigma^2
            refNodes[idx].rssiDown=RF_POWER_MIN_DBM-(pkt->LinkQuality/4);
            refNodes[idx].room=BUILD_UINT16( //ROOM
                                  pkt->cmd.Data[RSSI_RESP_ROOM_LO_IDX],
                                  pkt->cmd.Data[RSSI_RESP_ROOM_HI_IDX]);
            refNodes[idx].a=pkt->cmd.Data[RSSI_RESP_A_IDX]; //A
            refNodes[idx].n=pkt->cmd.Data[RSSI_RESP_N_IDX]; //n_index
            if(rspCnt<=idx) rspCnt=idx+1;
            break; //ho trovato e fatto quindi esco dal for
          }
      break;
    case GET_DIAGNOSTIC:
      sendDiagnosticPkt(pkt->LinkQuality);
      break;
    default:
      break;
  }
}

static void parseConfig(uint8 *msg) { //fill the configuration struct
  if(msg[1]==RSP_TYPE_XY) config.localizationType=RSP_TYPE_XY;
  else {
    config.localizationType=RSP_TYPE_RSSI;
    msg[1]=RSP_TYPE_RSSI;
  }
  config.timeout=BUILD_UINT16(msg[3],msg[2]);
  config.cycle=BUILD_UINT16(msg[5],msg[4]);
      if(msg[0]==NODE_MODE_AUTO) {
        if(config.timeout>=BLINDNODE_WAIT_TIMEOUT && config.cycle>config.timeout)
          config.mode=NODE_MODE_AUTO;
        else {
          config.mode=NODE_MODE_POLLED;
          msg[0]=NODE_MODE_POLLED;
          config.timeout=BLINDNODE_WAIT_TIMEOUT;
          msg[2]=HI_UINT16(config.timeout);
          msg[3]=LO_UINT16(config.timeout);
          config.cycle=BLINDNODE_FIND_DELAY;
          msg[4]=HI_UINT16(config.cycle);
          msg[5]=LO_UINT16(config.cycle);
        }
      } else {
        config.mode=NODE_MODE_POLLED;
        msg[0]=NODE_MODE_POLLED;
      }
  osal_nv_write(LOC_NV_BLINDNODE_CONFIG,0,sizeof( BN_Cfg_t ),&config);
  if(config.mode==NODE_MODE_AUTO)
    osal_set_event(BlindNode_TaskID, BLINDNODE_FIND_EVT);
  annceMsg[0]=msg[0]; //mode
  annceMsg[1]=msg[2]; //timeout HI
  annceMsg[2]=msg[3]; //timeout LO
  annceMsg[3]=msg[4]; //cycle HI
  annceMsg[4]=msg[5]; //cycle LO
  annceMsg[5]=msg[1]; //locType
  AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc,ACK_BLIND_CONFIG,6,msg,
                            &transId, AF_TX_OPTIONS_NONE,AF_DEFAULT_RADIUS);
}

static uint8 checkRSSIforLocationEngine(uint8 rssi) {
  if(rssi>LOC_ENGINE_MIN_DBM) return(0);
  if(rssi<LOC_ENGINE_MAX_DBM) return(LOC_ENGINE_MAX_DBM);
  else return(rssi);
}

static afStatus_t sendXYrsp(void) { //Build and send the XY message
  uint8 *msg, idx, size=BLIND_XY_RESPONSE_LEN+1+3*numOfSens;
  msg=osal_mem_alloc(sizeof(uint8)*(size+8));
  RefNode_t *bestRef=findBestRSSI(refNodes);
  msg[BLIND_XY_RESPONSE_HI_ROOM]=HI_UINT16(bestRef->room);
  msg[BLIND_XY_RESPONSE_LO_ROOM]=LO_UINT16(bestRef->room);
  if(bestRef->rssiUp<=RF_POWER_ZERO_DST) {//sono attaccato a un RefNode
    msg[BLIND_XY_RESPONSE_STATUS]=BLINDNODE_VERY_NEAR_REFNODE;
    msg[BLIND_XY_RESPONSE_REFNUM]=1;
    msg[BLIND_XY_RESPONSE_HI_X]=HI_UINT16(bestRef->x);
    msg[BLIND_XY_RESPONSE_LO_X]=LO_UINT16(bestRef->x);
    msg[BLIND_XY_RESPONSE_HI_Y]=HI_UINT16(bestRef->y);
    msg[BLIND_XY_RESPONSE_LO_Y]=LO_UINT16(bestRef->y);
  } else { //devo calcolare la posizione
    LocRefNode_t locNodes[BLINDNODE_MAX_REF_NODES];
    uint16 xOff, yOff;
    uint8 cnt=0;
    rspCnt=sortNodes(refNodes,bestRef->room); //Sort the ref nodes by RSSI
    for(idx=0;idx<rspCnt;idx++) {
      refNodes[idx].rssiUp=checkRSSIforLocationEngine(refNodes[idx].rssiUp);
      if(refNodes[idx].rssiUp!=0) cnt++;
    }
    if(cnt>=BLINDNODE_MIN_REF_NODES) {
      msg[BLIND_XY_RESPONSE_STATUS] = BLINDNODE_RSP_STATUS_SUCCESS;
      calcOffsets(refNodes, &xOff, &yOff);
      setLogicals(locNodes,refNodes,xOff,yOff); //Convert to logical coordinates
      config.loc.param_a = bestRef->a; //setto a della stanza in cui mi trovo
      config.loc.param_n = bestRef->n; //setto n della stanza in cui mi trovo
      locationCalculatePosition(locNodes,&(config.loc)); //Run the location calc
      xOff += config.loc.x;  //Convert results to real coordinates...
      yOff += config.loc.y;  //... and average over several samples.
      if((xOff>xOld && (xOff-xOld)>BLINDNODE_FLUSH) ||
         (xOff<xOld && (xOld-xOff)>BLINDNODE_FLUSH) ||
         (yOff>yOld && (yOff-yOld)>BLINDNODE_FLUSH) ||
         (yOff<yOld && (yOld-yOff)>BLINDNODE_FLUSH)) {
        xOld = xOff;
        yOld = yOff;
      } else {
        xOld=((xOld *(BLINDNODE_FILTER-1))+xOff)/BLINDNODE_FILTER;
        yOld=((yOld *(BLINDNODE_FILTER-1))+yOff)/BLINDNODE_FILTER;
      }
      xOff=(uint16)xOld;
      yOff=(uint16)yOld;
    } else {
      msg[0] = BLINDNODE_RSP_STATUS_NOT_ENOUGH_REFNODES;
      xOff=(uint16)xOld;
      yOff=(uint16)yOld;
    }
    msg[BLIND_XY_RESPONSE_REFNUM] = cnt;
    msg[BLIND_XY_RESPONSE_HI_X]=HI_UINT16(xOff);
    msg[BLIND_XY_RESPONSE_LO_X]=LO_UINT16(xOff);
    msg[BLIND_XY_RESPONSE_HI_Y]=HI_UINT16(yOff);
    msg[BLIND_XY_RESPONSE_LO_Y]=LO_UINT16(yOff);
    #if defined ( LCD_SUPPORTED )
    {
      uint8 hex[]="0123456789ABCDEF";
      uint8 buf[32];
      uint16 tmp;
      osal_memset(buf,' ', 16);
      tmp=xOff/4;
      if(tmp>99) tmp=99;
      if(tmp<10) _ltoa(tmp, buf+1, 10);
      else _ltoa(tmp, buf, 10);
      buf[2]=',';
      tmp=yOff/4;
      if(tmp>99) tmp=99;
      if(tmp<10) _ltoa(tmp, buf+4, 10);
      else _ltoa(tmp, buf+3, 10);
      buf[5]=' ';
      if(cnt > 16) buf[6] = 'X';
      else buf[6]=hex[cnt];
      HalLcdWriteString((char*)buf, HAL_LCD_LINE_1);
      osal_memset(buf, ' ', 16);
      if(rspCnt != 0) {
        tm =refNodes->x/4;
        if(tmp>99) tmp = 99;
        if(tmp<10) _ltoa(tmp, buf+1, 10);
        else _ltoa(tmp, buf, 10);
        buf[2]=',';
        tmp=refNodes->y/4;
        if(tmp>99) tmp=99;
        if(tmp<10) _ltoa(tmp, buf+4, 10);
        else _ltoa(tmp, buf+3, 10);
        buf[5]=' ';
        tmp=refNodes->addr;
        buf[9]=hex[tmp & 0xf];
        tmp /= 16;
        buf[8]=hex[tmp & 0xf];
        tmp /= 16;
        buf[7]=hex[tmp & 0xf];
        tmp /= 16;
        buf[6]=hex[tmp & 0xf];
      }
      HalLcdWriteString((char*)buf, HAL_LCD_LINE_2);
    }
    #endif
  }
  idx=BLIND_XY_RESPONSE_LEN;
  msg[idx++]=numOfSens; //metto anche i valori dei sensori
  uint16 sensVal;
  for(int i=0;i<numOfSens;i++) {
    msg[idx++]=sensIds[i];
    sensVal=readSensValue(sensIds[i]);
    msg[idx++]=HI_UINT16(sensVal);
    msg[idx++]=LO_UINT16(sensVal);
  }
  for(idx=0;idx<8;idx++) msg[size+idx]=ieeeAddr[idx]; //accodo IEEE
  osal_start_timerEx(BlindNode_TaskID, BLINDNODE_WAIT_EVT, 1000);
  return(AF_DataRequest(&gatewayAddr, (endPointDesc_t *)&epDesc,
                           BLIND_XY_RESPONSE, BLIND_XY_RESPONSE_LEN+8, msg,
                           &transId, 0, AF_DEFAULT_RADIUS));
}

static afStatus_t sendRSSIrsp(void) { //Build and send the RSSI collection
  RefNode_t *bestRef=findBestRSSI(refNodes);
  rspCnt=sortNodes(refNodes,bestRef->room); //solo nodi della stessa stanza!
  uint8 idx=1, i, *msg, size=2+5*rspCnt+3*numOfSens;
  msg=osal_mem_alloc(sizeof(uint8)*(size+8));
  msg[0]=rspCnt;
  for(i=0;i<rspCnt;i++) {
    msg[idx++]=HI_UINT16(refNodes[i].addr);
    msg[idx++]=LO_UINT16(refNodes[i].addr);
    msg[idx++]=refNodes[i].rssiUp;
    msg[idx++]=refNodes[i].sigma2;
    msg[idx++]=refNodes[i].rssiDown;
  }
  msg[idx++]=numOfSens; //metto anche i valori dei sensori
  uint16 sensVal;
  for(i=0;i<numOfSens;i++) {
    msg[idx++]=sensIds[i];
    sensVal=readSensValue(sensIds[i]);
    msg[idx++]=HI_UINT16(sensVal);
    msg[idx++]=LO_UINT16(sensVal);
  }
  for(idx=0;idx<8;idx++) msg[size+idx]=ieeeAddr[idx]; //accodo IEEEaddr
  osal_start_timerEx(BlindNode_TaskID, BLINDNODE_WAIT_EVT, 1000);
  afStatus_t status=AF_DataRequest(&gatewayAddr, (endPointDesc_t *)&epDesc,
                           BLIND_RSSI_RESPONSE, size+8, msg,
                           &transId, 0, AF_DEFAULT_RADIUS);
  osal_mem_free(msg);
  return(status);
}

static void finishCollection(void) { //Sends the next bindNode Response message
  if((ZDO_Config_Node_Descriptor.CapabilityFlags & CAPINFO_RCVR_ON_IDLE)==0) {
    uint8 x=false;
    ZMacSetReq(ZMacRxOnIdle, &x); //Turn the receiver back off while idle
  }
  if(config.mode==NODE_MODE_AUTO) {  //set up next auto response
    if(config.localizationType==RSP_TYPE_XY) sendXYrsp();
    else sendRSSIrsp();
    osal_start_timerEx(BlindNode_TaskID, BLINDNODE_FIND_EVT, config.cycle*TIME_UNIT);
  } else { //se non sono in automode c'è una richiesta singola da esaudire
    if(singleRequest==RSP_TYPE_XY) sendXYrsp();
    else sendRSSIrsp();
  }
  if(newConfig==1) { //se è arrivata una nuova config mentre lavorava
    newConfig=0;
    parseConfig(newTempConfig); //riconfigurare
  }
  state=eBnIdle; //stato idle, fine blast, disponibile per nuova sequenza
}

/* @fn      calcOffsets
 * @brief   Calculates the XY offsets.
 * INPUTS:
 * @param   ref - Array of reference nodes, pre-sorted on RSSI, best to worst.
 * OUTPUTS:
 * @param   xOff - pointer to X offset
 * param    yOff - pointer to Y offset*/
static void calcOffsets(RefNode_t *ref, uint16 *xOff, uint16 *yOff) {
  RefNode_t *rnP = ref;
  uint16 xMax = 0;
  uint16 yMax = 0;
  for(uint8 idx=0;idx<rspCnt;idx++,rnP++) {
    if(xMax<rnP->x) xMax=rnP->x;
    if(yMax<rnP->y) yMax=rnP->y;
  }
  if(xMax<256 && yMax<256) *xOff = *yOff=0; // No need for conversion.
  else { //Force reference node with the best RSSI to sit at logical (32,32).
    *xOff=(ref->x & 0xFFFC)-128;
    *yOff=(ref->y & 0xFFFC)-128;
  }
}

static RefNode_t *findBestRSSI(RefNode_t *ref) {//Finds the node with best RSSI
  RefNode_t *bestRef = NULL;
  uint8 idx;
  for(idx=0;idx<rspCnt;idx++,ref++)
    if(ref->addr!=INVALID_NODE_ADDR && ref->rssiUp!=0)
      if(bestRef==NULL || ref->rssiUp<bestRef->rssiUp) bestRef=ref;
  return bestRef;
}

static uint8 sortNodes(RefNode_t *refList, uint16 ofRoom) {
  RefNode_t *workNodes;
  workNodes=osal_mem_alloc(sizeof(RefNode_t)*rspCnt);
  if(workNodes==NULL) return 0;
  osal_memcpy(workNodes, refList, sizeof(RefNode_t)*rspCnt);
  uint8 idx,count=0;
  for(idx=0;idx<rspCnt;idx++,refList++) {
    RefNode_t *node = findBestRSSI(workNodes);
    if(node==NULL) break;
    else if(node->room==ofRoom){ //Scelgo i nodi della mia stanza
      count++;
      osal_memcpy(refList, node, sizeof(RefNode_t));
      node->addr = INVALID_NODE_ADDR;
    }
  }
  osal_mem_free(workNodes);
  return count;
}

/* @fn      setLogicals
 * @brief   Sets the reference node's logical coordinates & RSSI for the
 *          required number of inputs to the location engine.
 * INPUTS:
 * @param   ref - array of reference nodes
 * @param   offsetX - X offset used to make logical numbers
 * param    offsetY - Y offset used to make logical numbers
 *
 * @return  none*/
static void setLogicals(LocRefNode_t *loc,RefNode_t *ref,uint16 xOff,uint16 yOff) {
  // Rev-B Chip have LocEng Ver 1.0 w/ cap=8, Rev-C have LocEng Ver 2.0 w/ 16.
  const uint8 stop = ( ( CHVER == 0x01 ) ? LOC_ENGINE_NODE_CAPACITY_REVB :
                                          LOC_ENGINE_NODE_CAPACITY_REVC);
  uint16 xTmp, yTmp;
  uint8 idx;
  for (idx=0;idx<rspCnt;idx++,loc++,ref++) { //Set the logical coordinates
    xTmp = ref->x - xOff;
    yTmp = ref->y - yOff;
    if (xTmp<256 && yTmp<256) {
      loc->x = (uint8)xTmp;
      loc->y = (uint8)yTmp;
      loc->rssi = ref->rssiUp;
    }
    else loc->x=loc->y=loc->rssi=0; //Out of bounds, so feed zero to loc engine.
  }
  for(;idx<stop;idx++) { //Feed zero to locengine to meet the required number of inputs.
    loc->x=loc->y =0;
    loc->rssi=0;
    loc++;
  }
}

static void startBlast(void) { //Starts a sequence of blasts
  uint8 idx;
  if((ZDO_Config_Node_Descriptor.CapabilityFlags & CAPINFO_RCVR_ON_IDLE)==0) {
    idx = true; // Turn the receiver on while idle - temporarily.
    ZMacSetReq(ZMacRxOnIdle, &idx);
  }
  LoacationApp_Sleep(FALSE); //svegliati!
  for(idx=0;idx<BLINDNODE_MAX_REF_NODES;idx++) refNodes[idx].addr=INVALID_NODE_ADDR;
  AF_DataRequest(&broadcastRefsAddr, (endPointDesc_t *)&epDesc,
                         LOCATION_RSSI_BLAST, 0, NULL, &transId,
                         AF_SKIP_ROUTING, 1);
  rspCnt = 0; //contatore risposte
  blastCnt = BLINDNODE_BLAST_COUNT; //contatore blast rimanenti
  state = eBnBlastOut; //stato blast verso refNodes, si inzia a inviare
  osal_start_timerEx(BlindNode_TaskID,BLINDNODE_BLAST_EVT,BLINDNODE_BLAST_DELAY);
}

static void sendDiagnosticPkt(uint8 parentRSSI) { //invia valore di tensione batteria e RSSI parent
  uint8 diagnosticPkt[3];
  uint16 battVal=readSensValue(SENS_BATT_ID);
  diagnosticPkt[0] = HI_UINT16(battVal);
  diagnosticPkt[1] = LO_UINT16(battVal);
  diagnosticPkt[2] = RF_POWER_MIN_DBM-(parentRSSI/4);
  AF_DataRequest(&gatewayAddr, (endPointDesc_t *)&epDesc, //manda il pacchetto
                           DIAGNOSTIC_PKT, 3, diagnosticPkt,
                           &transId, AF_TX_OPTIONS_NONE,
                             AF_DEFAULT_RADIUS ); //numero massimo di hop
}

static void processAnnounceRequest(byte *reqIEEEaddr) {
   int equal=1,i,j;
   for(i=0,j=7;i<8 && equal==1;i++,j--) if(reqIEEEaddr[j]!=ieeeAddr[i]) equal=0;
   if(equal==1) AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc,
                 BLIND_ANNCE,AnnceMsgLen,annceMsg,&transId,0,
                 AF_DEFAULT_RADIUS);//rispondo se ho l'IEEE addr richiesto
}
