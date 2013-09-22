//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : RefNode.c
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
#include "OSAL_Nv.h"
#include "MTEL.h"
#include "AF.h"
#include "ZDApp.h"
#include "OnBoard.h"
#include "hal_key.h"
#if defined ( LCD_SUPPORTED )
  #include "hal_lcd.h"
#endif
#include "LocationApp.h"
#include "RefNode.h"
#include "SensorID.h"

//TYPEDEFS
typedef struct { //configurzaione del nodo verrà salvata nella NV memory
  uint16 x; //coordinata X
  uint16 y; //coordinata Y
  uint16 room; //identificativo della stanza
  uint16 cycle; //In decimi di secondo - tempo per inviare i sensori
  uint8  mode;  //auto o polled mode
  byte n; //0-31 identificativo del parametro n della stanza
  byte a; //30-50 RSSI(dBm) @ 1 m - parametro A per la stanza
} refNode_Config_t;

/* TBD - could put a timestamp in the blastAcc_t so timeout old blasts that
 *       were not followed up with an RSSI request. This happens when the
 *       unruly broadcast mechanism fails to head the request for 0 retries.
 *       After Blind Node has finished blasting and then collecting the RSSI's
 *       it is seen on the sniffer that some of the original blasts are getting
 *       re-transmitted.*/
typedef struct _blastAcc_t {
  struct _blastAcc_t *next;
  uint16 addr; //NTW addr del mittente
  uint8 rssiVect[BLINDNODE_BLAST_COUNT];
  byte cnt;
} blastAcc_t;

//GLOBAL VARIABLES
byte RefNode_TaskID;

//LOCAL VARIABLES
static const cId_t RefNode_InputClusterList[] = {
  RSSI_REQUEST,
  REF_CONFIG,
  LOCATION_RSSI_BLAST,
  GET_ANNOUNCE,
  GET_IEEE_ANNOUNCE,
  SENS_REQUEST,
  GET_DIAGNOSTIC
};

static const cId_t RefNode_OutputClusterList[] = {
  RSSI_RESPONSE,
  SENS_PKT,
  REF_ANNCE,
  DIAGNOSTIC_PKT,
  ACK_REF_CONFIG
};

static const SimpleDescriptionFormat_t RefNode_SimpleDesc = {
  LOCATION_REFNODE_ENDPOINT,
  LOCATION_PROFID,
  LOCATION_REFNODE_DEVICEID,
  LOCATION_DEVICE_VERSION,
  LOCATION_FLAGS,
  sizeof(RefNode_InputClusterList),(cId_t*)RefNode_InputClusterList,
  sizeof(RefNode_OutputClusterList),(cId_t*)RefNode_OutputClusterList
};

static const endPointDesc_t epDesc = {
  LOCATION_REFNODE_ENDPOINT,
  &RefNode_TaskID,
  (SimpleDescriptionFormat_t *)&RefNode_SimpleDesc,
  noLatencyReqs
};

static refNode_Config_t config;
static byte rispMsg[RSSI_RESP_LEN]; //risp con coord e RSSI medio misurato
static byte *annceMsg; //messaggio di annuncio
static byte AnnceMsgLen; //lunghezza messaggio di annuncio
static uint8* ieeeAddr; //IEEE addr
static uint8 numOfSens, sensIds[NUM_MAX_SENS];
static byte transId;
static blastAcc_t *blastPtr; //puntatore alla lista dei blast ricevuti
static afAddrType_t gatewayAddr; //indirizzo del gateway

//LOCAL FUNCTIONS
static void processMSGCmd(afIncomingMSGPacket_t *pkt);
static void parseConfig(byte *msg);
static void addBlast(uint16 addr, uint8 rssi);
static void rssiRsp(afIncomingMSGPacket_t *pkt);
static void processAnnounceRequest(byte *reqIEEEaddr);
static void sendSensPkt(void);
static void sendDiagnosticPkt(uint8 parentRSSI);

void RefNode_Init(byte task_id) { //Initialization function for this OSAL task.
  #if defined ( LCD_SUPPORTED )
    HalLcdWriteString("Location-RefNode", HAL_LCD_LINE_2);
  #endif
  RefNode_TaskID = task_id;
  if(ZSUCCESS==osal_nv_item_init(LOC_NV_REFNODE_CONFIG,sizeof(refNode_Config_t),&config))
    osal_nv_read(LOC_NV_REFNODE_CONFIG,0,sizeof(refNode_Config_t),&config);
  else { //se non c'è nella memoria NV inizializzo ai valori standard
    config.x = LOC_DEFAULT_X_Y;
    config.y = LOC_DEFAULT_X_Y;
    config.room = LOC_NO_ROOM_ASSIGNED;
    config.mode = NODE_MODE_POLLED;
    config.cycle = REFNODE_DEFAULT_CYCLE;
    config.a = LOC_DEFAULT_A;
    config.n = LOC_DEFAULT_N;
    osal_nv_write(LOC_NV_REFNODE_CONFIG,0, sizeof(refNode_Config_t),&config);
  }
  rispMsg[RSSI_RESP_X_HI_IDX]=HI_UINT16(config.x);
  rispMsg[RSSI_RESP_X_LO_IDX]=LO_UINT16(config.x);
  rispMsg[RSSI_RESP_Y_HI_IDX]=HI_UINT16(config.y);
  rispMsg[RSSI_RESP_Y_LO_IDX]=LO_UINT16(config.y);
  rispMsg[RSSI_RESP_RSSI_IDX]=0; //verrà aggiornato ogni volta
  rispMsg[RSSI_RESP_ROOM_HI_IDX]=HI_UINT16(config.room);
  rispMsg[RSSI_RESP_ROOM_LO_IDX]=LO_UINT16(config.room);
  rispMsg[RSSI_RESP_A_IDX]=config.a;
  rispMsg[RSSI_RESP_N_IDX]=config.n;
  afRegister((endPointDesc_t *)&epDesc);//Register the endpoint/interface to AF
  //RegisterForKeys(RefNode_TaskID); //Register for all key events
  numOfSens=findVectAvblSensId(sensIds);
  ieeeAddr = NLME_GetExtAddr();
  byte idx,i;
  AnnceMsgLen=20+numOfSens; //lunghezza annuncio:12+8 bytes + gli id dei sensori
  annceMsg=(byte*)osal_mem_alloc(AnnceMsgLen); //alloca l'annuncio
  annceMsg[0]=HI_UINT16(config.x); //x
  annceMsg[1]=LO_UINT16(config.x);
  annceMsg[2]=HI_UINT16(config.y); //y
  annceMsg[3]=LO_UINT16(config.y);
  annceMsg[4]=HI_UINT16(config.room); //room
  annceMsg[5]=LO_UINT16(config.room);
  annceMsg[6]=config.a;
  annceMsg[7]=config.n;
  annceMsg[8]=config.mode;
  annceMsg[9]=HI_UINT16(config.cycle);
  annceMsg[10]=LO_UINT16(config.cycle);
  annceMsg[11]=numOfSens;
  for(i=0,idx=12;i<numOfSens;i++,idx++) annceMsg[idx]=sensIds[i]; //sensors IDs
  for(i=0;i<8;i++,idx++) annceMsg[idx]=ieeeAddr[i]; //accodo l'IEEE address
  gatewayAddr.addrMode = afAddr16Bit;
  gatewayAddr.addr.shortAddr = 0;
  gatewayAddr.endPoint = LOCATION_GATEWAY_ENDPOINT;
  AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc,REF_ANNCE,
                 AnnceMsgLen, annceMsg, &transId, 0, AF_DEFAULT_RADIUS);
  if(config.mode==NODE_MODE_AUTO && config.cycle>REFNODE_MIN_CYCLE)
    osal_start_timerEx(RefNode_TaskID, SENS_EVT, config.cycle*TIME_UNIT);
}

uint16 RefNode_ProcessEvent(byte task_id, uint16 events) {//Task event processor
  if(events & SYS_EVENT_MSG) {
    afIncomingMSGPacket_t *MSGpkt;
    while((MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive(RefNode_TaskID))) {
      switch ( MSGpkt->hdr.event ) {
      case KEY_CHANGE:
        //LocationHandleKeys(((keyChange_t *)MSGpkt)->state,((keyChange_t *)MSGpkt)->keys);
        break;
      case AF_DATA_CONFIRM_CMD:
        break;
      case AF_INCOMING_MSG_CMD:
        processMSGCmd(MSGpkt);
        break;
      case ZDO_STATE_CHANGE:
        osal_start_timerEx(RefNode_TaskID, ANNCE_EVT, ANNCE_DELAY);
        break;
      default:
        break;
      }
      osal_msg_deallocate( (uint8 *)MSGpkt );
    }
    return(events^SYS_EVENT_MSG); //Return unprocessed events.
  }
  if(events&ANNCE_EVT) {//Broadcast the X,Y location in order to register this node
    afAddrType_t dstAddr;
    dstAddr.addrMode = afAddrBroadcast;
    dstAddr.addr.shortAddr = NWK_BROADCAST_SHORTADDR_DEVALL;
    dstAddr.endPoint = LOCATION_GATEWAY_ENDPOINT;
    AF_DataRequest(&dstAddr, (endPointDesc_t *)&epDesc,
                   RSSI_RESPONSE, RSSI_RESP_LEN, rispMsg,
                   &transId, 0, AF_DEFAULT_RADIUS);
    return(events ^ ANNCE_EVT);
  }
  if(events & SENS_EVT) { //è ora di mandare i valori dei sensori
    if(config.mode==NODE_MODE_AUTO) { //se sono ancora configuarto in automatico
      sendSensPkt(); //mando i sensori
      osal_start_timerEx(RefNode_TaskID, SENS_EVT, config.cycle*TIME_UNIT);
    } //e poi faccio ripartire il timer
    return (events ^ SENS_EVT);
  }
  return 0; //Discard unknown events
}

void LocationHandleKeys(byte keys) { //Handles key events for this device
  if(keys & HAL_KEY_SW_1) {}
  if(keys & HAL_KEY_SW_2) {}
  if(keys & HAL_KEY_SW_3) {}
  if(keys & HAL_KEY_SW_4) osal_set_event(RefNode_TaskID, ANNCE_EVT);
}

static void processMSGCmd(afIncomingMSGPacket_t *pkt) { //msg processor callback
  switch (pkt->clusterId) {
  case GET_IEEE_ANNOUNCE:
    processAnnounceRequest(pkt->cmd.Data);
    break;
  case GET_ANNOUNCE:
      AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc, REF_ANNCE,
                     AnnceMsgLen,annceMsg,&transId, 0, AF_DEFAULT_RADIUS);
    break;
  case RSSI_REQUEST: //richiesta di RSSI misurato dopo i blasts
    rssiRsp(pkt);
    break;
  case REF_CONFIG: //nuova configurazione
    parseConfig(pkt->cmd.Data);
    break;
  case LOCATION_RSSI_BLAST: //ricezione di un blast
    addBlast(pkt->srcAddr.addr.shortAddr, pkt->LinkQuality);
    break;
  case SENS_REQUEST:
    sendSensPkt();
    break;
  case GET_DIAGNOSTIC:
    sendDiagnosticPkt(pkt->LinkQuality);
    break;
  default:
    break;
  }
}

static void parseConfig(byte *msg) { //Populate the configuration struct
  config.x = BUILD_UINT16(msg[1],msg[0]);
  config.y = BUILD_UINT16(msg[3],msg[2]);
  config.room=BUILD_UINT16(msg[5],msg[4]);
  config.a=msg[6];
  if(config.a<30) { //controllo che a sia maggiore di 30
    config.a=30;    //sono specifiche del location ngine
    msg[6]=30;
  } else if(config.a>50) { //controllo che a sia minore di 50
    config.a=50;
    msg[6]=50;
  }
  config.n=msg[7];
  if(config.n>31) { //controllo che a sia minore di 31
    config.n=31;    //sono specifiche del location ngine
    msg[7]=31;
  }
  config.cycle=BUILD_UINT16(msg[10],msg[9]);
  if(msg[8]==NODE_MODE_AUTO) {
    if(config.cycle>REFNODE_MIN_CYCLE) config.mode=NODE_MODE_AUTO;
    else {
      config.mode=NODE_MODE_POLLED;
      msg[8]=NODE_MODE_POLLED;
      config.cycle=REFNODE_DEFAULT_CYCLE;
      msg[9]=HI_UINT16(config.cycle);
      msg[10]=LO_UINT16(config.cycle);
    }
  } else {
    config.mode=NODE_MODE_POLLED;
    msg[8]=NODE_MODE_POLLED;
  }
  osal_nv_write(LOC_NV_REFNODE_CONFIG, 0, sizeof(refNode_Config_t), &config);
  if(config.mode==NODE_MODE_AUTO) osal_set_event(RefNode_TaskID,SENS_EVT);
  int i;
  for(i=0;i<8;i++) rispMsg[i]=msg[i];
  for(i=0;i<11;i++) annceMsg[i]=msg[i];
  AF_DataRequest(&gatewayAddr,(endPointDesc_t *)&epDesc,ACK_REF_CONFIG,
                 11,msg,&transId, AF_TX_OPTIONS_NONE,AF_DEFAULT_RADIUS);
}

static void addBlast(uint16 addr, uint8 rssi) { //Adds a blast in the RSSI list
  blastAcc_t *ptr = blastPtr; //puntatore alla lista dei blast
  while(ptr) { //cerco se l'indirizzo del mittente è in lista
    if(ptr->addr==addr) break;
    ptr = ptr->next;
  }
  if(ptr) { //se c'è in lista
    ptr->rssiVect[ptr->cnt]=rssi; //memorizzo misura coorente
    ptr->cnt++; //incremento il conatore
  } else { //se non ho ancora addr in lista
    ptr=(blastAcc_t *)osal_mem_alloc(sizeof(blastAcc_t)); //nuovo elemento
    if(ptr) { //se l'allocazione è andata a buon fine
      ptr->next = blastPtr; //incateno il nuovo elemento alla testa lista
      blastPtr = ptr; //la nuova testa della lista è il nuovo elemento corrente
      ptr->addr = addr; //inserisco l'indirizzo
      ptr->rssiVect[0]=rssi; //inserisco l'RSSI misurato in posizione 0
      ptr->cnt=1; //setto il contatore a 1, c'è solo una misura per ora...
    }
  }
}

static void rssiRsp(afIncomingMSGPacket_t *pkt) {//risp al mitt con l'RSSI medio
  blastAcc_t *ptr = blastPtr; //puntatore alla testa della lista
  blastAcc_t *prev = NULL; //puntatore al precedente
  byte options, radius; //opzioni di trsmissione per la risposta
  while(ptr) { //cerco tra le misure collezionate quelle relative al mittente
    if(ptr->addr==pkt->srcAddr.addr.shortAddr) break; //trovato!
    prev=ptr; //l'elemento corrente diventa precedente
    ptr=ptr->next; //salto al prossimo elemento
  }
  if(ptr) { //se l'ho trovato, faccio la media sommando anche l'RSSI corrente
    uint16 somma=0; //sommatoria rssi
    for(int i=0;i<ptr->cnt;i++) somma=somma+ptr->rssiVect[i];
    somma=somma+pkt->LinkQuality; //aggiungo l'rssi dell richiesta dal blind
    uint8 media=somma/(ptr->cnt+1); //calcolo la media
    somma=0; //sommatoria di (rssi(i)-media)^2 per calcolare sigma^2
    for(int i=0;i<ptr->cnt;i++)
        somma=somma+(ptr->rssiVect[i]-media)*(ptr->rssiVect[i]-media);
    somma=somma+(pkt->LinkQuality-media)*(pkt->LinkQuality-media); //come prima
    rispMsg[RSSI_RESP_RSSI_IDX]=RF_POWER_MIN_DBM-(media/4); //converto in dBm
    rispMsg[RSSI_RESP_SIGMA2_IDX]=somma/ptr->cnt; //sigma^2 (dividere per n-1)
    if(prev) prev->next=ptr->next; //se c'era un precedente chiudo la catena
    else blastPtr = ptr->next; //altrimenti diventa il primo
    osal_mem_free(ptr); //libero la memoria occupata da tale elemento
    options = AF_SKIP_ROUTING; //niente routing
    radius = 1; //un solo salto
  } else { //se non lo trovo ho solo questo RSSI
    rispMsg[RSSI_RESP_RSSI_IDX]=RF_POWER_MIN_DBM-(pkt->LinkQuality/4);
    rispMsg[RSSI_RESP_SIGMA2_IDX]=0;
    options = AF_TX_OPTIONS_NONE;
    radius = AF_DEFAULT_RADIUS;
  }
  pkt->srcAddr.addrMode = afAddr16Bit; //indirizzamento singolo
  AF_DataRequest(&pkt->srcAddr, (endPointDesc_t *)&epDesc,
                         RSSI_RESPONSE, RSSI_RESP_LEN,
                         rispMsg, &transId, options, radius);
}

static void sendSensPkt(void) {
  byte *sensPkt;
  uint8 sensPktLen=1+3*numOfSens,i;
  uint16 sensVal;
  sensPkt=osal_mem_alloc(sizeof(uint8)*(sensPktLen));
  sensPkt[0]=numOfSens;
  for(i=0;i<numOfSens;i++) {
    sensPkt[i*3+1]=sensIds[i];
    sensVal=readSensValue(sensIds[i]);
    sensPkt[i*3+2]=HI_UINT16(sensVal);
    sensPkt[i*3+3]=LO_UINT16(sensVal);
  }
  AF_DataRequest(&gatewayAddr, (endPointDesc_t *)&epDesc, SENS_PKT, sensPktLen,
                 sensPkt, &transId, AF_TX_OPTIONS_NONE, AF_DEFAULT_RADIUS);
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
                 REF_ANNCE,AnnceMsgLen,annceMsg,&transId,0,
                 AF_DEFAULT_RADIUS);//rispondo se ho l'IEEE addr richiesto
}
