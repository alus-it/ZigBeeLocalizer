//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : LocationApp.c
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
#include "ZComDef.h"
#include "LocationApp.h"
#include "AF.h"
#if !defined( NONWK )
  #include "APS.h"
  #include "nwk.h"
  #include "ZDApp.h"
#endif
#include "OSAL.h"
#include "OSAL_Custom.h"
#include "OSAL_Nv.h"
#if defined( POWER_SAVING )
  #include "OSAL_PwrMgr.h"
#endif
#include "OSAL_Tasks.h"
#include "hal_drivers.h"
#include "hal_key.h"
#if defined ( LCD_SUPPORTED )
  #include "hal_lcd.h"
#endif
#include "hal_led.h"
#include "OnBoard.h"
#if defined ( MT_TASK )
  #include "MTEL.h"
#endif

#if defined ( LOCATION_GATEWAY )
 #include "Gateway.h"
#endif

#if defined ( LOCATION_REFNODE )
 #include "RefNode.h"
#endif

#if defined ( LOCATION_BLINDNODE )
 #include "BlindNode.h"
#endif

//CONSTANTS
#define LOCATION_APP_ENDPOINT                 10
#define LOCATION_APP_PROFID                   0x2001
#define LOCATION_APP_DEVICEID                 13
#define LOCATION_APP_FLAGS                    0
#define LOCATION_APP_GENERIC_ID               0x0001
#define LOCATION_APP_ON_OFF_ID                0x0010
#define LOCATION_APP_ON                       0xFF
#define LOCATION_APP_OFF                      0x00
#define LOCATION_APP_TOGGLE                   0xF0

//GLOBAL VARIABLES
uint8 LoacationApp_TaskID;

// The order in this table must be identical to the task initialization calls below in osalInitTask.
const pTaskEventHandlerFn tasksArr[] = {
#if defined( ZMAC_F8W )
  macEventLoop,
#endif
#if !defined( NONWK )
  nwk_event_loop,
#endif
  Hal_ProcessEvent,
#if defined( MT_TASK )
  MT_ProcessEvent,
#endif
#if !defined( NONWK )
  APS_event_loop,
  ZDApp_event_loop,
#endif
  LoacationApp_ProcessEvent,
#if defined ( LOCATION_REFNODE )
  RefNode_ProcessEvent
#endif
#if defined ( LOCATION_BLINDNODE )
  BlindNode_ProcessEvent
#endif
#if defined ( LOCATION_GATEWAY )
  Gateway_ProcessEvent
#endif
};
const uint8 tasksCnt = sizeof( tasksArr ) / sizeof( tasksArr[0] );
uint16 *tasksEvents;

// LOCAL VARIABLES
#if defined ( LOCATION_REFNODE )
static const cId_t LoacationApp_InputClusterList[] =
  {LOCATION_APP_GENERIC_ID,LOCATION_APP_ON_OFF_ID};
static const cId_t LoacationApp_OutputClusterList[] =
  {LOCATION_APP_GENERIC_ID};
#endif
#if defined ( LOCATION_BLINDNODE )
static const cId_t LoacationApp_InputClusterList[] =
  {LOCATION_APP_GENERIC_ID};

static const cId_t LoacationApp_OutputClusterList[] =
  {LOCATION_APP_GENERIC_ID,LOCATION_APP_ON_OFF_ID};
#endif

#if defined ( LOCATION_GATEWAY )
static const cId_t LoacationApp_InputClusterList[] =
  {LOCATION_APP_GENERIC_ID,LOCATION_APP_ON_OFF_ID};

static const cId_t LoacationApp_OutputClusterList[] =
  {LOCATION_APP_GENERIC_ID};
#endif

static const SimpleDescriptionFormat_t LoacationApp_SimpleDesc = {
  LOCATION_APP_ENDPOINT,
  LOCATION_APP_PROFID,
  LOCATION_APP_DEVICEID,
  LOCATION_APP_VERSION,
  LOCATION_APP_FLAGS,
  sizeof( LoacationApp_InputClusterList ),
  (cId_t*)LoacationApp_InputClusterList,
  sizeof( LoacationApp_OutputClusterList ),
  (cId_t*)LoacationApp_OutputClusterList
};

static const endPointDesc_t LoacationApp_epDesc = {
  LOCATION_APP_ENDPOINT,
  &LoacationApp_TaskID,
  (SimpleDescriptionFormat_t *)&LoacationApp_SimpleDesc,
  noLatencyReqs
};

#if defined( POWER_SAVING )
/* The ZDO_STATE_CHANGE event will be received twice after initiating the
 * rejoin process, only act on one of them.*/
static uint8 rejoinPending;
#endif

//LOCAL FUNCTIONS PROTOTYPES
static void LoacationApp_Init( uint8 task_id );
static void handleKeys( uint8 shift, uint8 keys );
static void processMSGCmd( afIncomingMSGPacket_t *pkt );

//LOCAL FUNCTIONS IMPLEMENTATION
/* @fn      osalInitTasks
 * @brief   This function invokes the initialization function for each task.
 * @param   void
 * @return  none*/
void osalInitTasks( void ) {
  uint8 taskID = 0;
  tasksEvents = (uint16 *)osal_mem_alloc( sizeof( uint16 ) * tasksCnt);
  osal_memset( tasksEvents, 0, (sizeof( uint16 ) * tasksCnt));
#if defined( ZMAC_F8W )
  macTaskInit( taskID++ );
#endif
#if !defined( NONWK )
  nwk_init( taskID++ );
#endif
  Hal_Init( taskID++ );
#if defined( MT_TASK )
  MT_TaskInit( taskID++ );
#endif
#if !defined( NONWK )
  APS_Init( taskID++ );
  ZDApp_Init( taskID++ );
#endif
  LoacationApp_Init( taskID++ );
#if defined ( LOCATION_REFNODE )
  RefNode_Init( taskID );
#endif
#if defined ( LOCATION_BLINDNODE )
  BlindNode_Init( taskID );
#endif
#if defined ( LOCATION_GATEWAY )
  Gateway_Init( taskID );
#endif
}

/* @fn      LoacationApp_Init
 * @brief   Initialization function for the LoacationApp OSAL task.
 * @param   task_id - the ID assigned by OSAL.
 * @return  none*/
static void LoacationApp_Init( uint8 task_id ) {
  LoacationApp_TaskID = task_id;
  afRegister((endPointDesc_t *)&LoacationApp_epDesc); //Register for AF
  RegisterForKeys(LoacationApp_TaskID); //Register for all key events
#if defined ( LCD_SUPPORTED )
  HalLcdWriteString( "<-LoacationApp->", HAL_LCD_LINE_1 );
#endif
}

/* @fn      LoacationApp_ProcessEvent
 * @brief   Generic Application Task event processor.
 * @param   task_id  - The OSAL assigned task ID.
 * @param   events - Bit map of events to process.
 * @return  none*/
uint16 LoacationApp_ProcessEvent( uint8 task_id, uint16 events ) {
  if ( events & SYS_EVENT_MSG ) {
    afIncomingMSGPacket_t *MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive(
                                                             LoacationApp_TaskID );
    while ( MSGpkt != NULL ) {
      switch ( MSGpkt->hdr.event ) {
      case KEY_CHANGE:
        handleKeys( ((keyChange_t *)MSGpkt)->state, ((keyChange_t *)MSGpkt)->keys );
        break;
      case AF_DATA_CONFIRM_CMD:
#if !defined( RTR_NWK )
        {
        // This message is received as a confirmation of a data packet sent.
        // The status is of ZStatus_t type [defined in ZComDef.h]
        afDataConfirm_t *afDataConfirm = (afDataConfirm_t *)MSGpkt;
        /* No ACK from the MAC layer implies that mobile device is out of
         * range of most recent parent. Therefore, begin an orphan scan
         * to try to find a former parent.
         * NOTE: To get the fastest action in the process of finding a new
         * parent, set the MAX_JOIN_ATTEMPTS in ZDApp.c to 1.*/
        if(afDataConfirm->hdr.status == ZMacNoACK) LoacationApp_NoACK();
        else{}// Some other error -- Do something.
        }
#endif
        break;
      case AF_INCOMING_MSG_CMD:
        processMSGCmd( MSGpkt );
        break;
      case ZDO_STATE_CHANGE:
#if defined( POWER_SAVING )
        if(rejoinPending) {
          rejoinPending = FALSE;
          LoacationApp_Sleep(TRUE); //Ok to resume power saving ops.
        }
#endif
        break;
      default:
        break;
      }
      osal_msg_deallocate( (uint8 *)MSGpkt );
      MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive(LoacationApp_TaskID);
    }
    return (events ^ SYS_EVENT_MSG);  // Return unprocessed events.
  }
  return 0;  // Discard unknown events
}

/* @fn      LoacationApp_NoACK
 * @brief   Sample Application recovery from getting a ZMacNoAck.
 * @return  none*/
void LoacationApp_NoACK( void ) {
#if defined( POWER_SAVING )
  rejoinPending = TRUE;
  ZDApp_SendEventMsg( ZDO_NWK_JOIN_REQ, 0, NULL );
  LoacationApp_Sleep( FALSE );
#endif
}

/* @fn      LoacationApp_Sleep
 * @brief   Sample Application set allow/disallow sleep mode.
 * @return  none*/
void LoacationApp_Sleep( uint8 allow ) {
#if defined( POWER_SAVING )
  if ( allow ) {
    osal_pwrmgr_task_state( NWK_TaskID, PWRMGR_CONSERVE );
    NLME_SetPollRate( 0 );
  } else {
    osal_pwrmgr_task_state( NWK_TaskID, PWRMGR_HOLD );
    NLME_SetPollRate( 1000 );
  }
#endif
}

/* @fn      handleKeys
 * @brief   Handles all key events for this device.
 * @param   shift - true if in shift/alt.
 * @param   keys - bit field for key events. Valid entries:
 * @return  none*/
static void handleKeys( uint8 shift, uint8 keys ) {
/* POWER_SAVING key press interrupt uses shift key to awaken from deep sleep,
 * so it is not available.*/
#if !defined( POWER_SAVING )
  /* Give the LocationProfile access to 4 keys by sending any Shif-Key
   * sequence to the profile as a normal Key.*/
  if ( shift ) LocationHandleKeys( keys );
  else
#endif
  {
    if(keys & HAL_KEY_SW_1) {}
    if(keys & HAL_KEY_SW_2) {}
    if(keys & HAL_KEY_SW_3) {}
    if(keys & HAL_KEY_SW_4) {}
  }

#if defined ( LOCATION_BLINDNODE )
  {
    static uint8 transId;
    uint8 actionId = LOCATION_APP_TOGGLE;
    afAddrType_t dstAddr;
    dstAddr.addrMode = afAddrBroadcast;
    dstAddr.addr.shortAddr = NWK_BROADCAST_SHORTADDR_DEVALL;
    dstAddr.endPoint = LOCATION_APP_ENDPOINT;
    // Control all lights within 1-hop radio range.
    AF_DataRequest( &dstAddr, (endPointDesc_t *)&LoacationApp_epDesc,
            LOCATION_APP_ON_OFF_ID, 1, &actionId, &transId, AF_SKIP_ROUTING, 1);
  }
#endif
}

/* @fn      processMSGCmd
 * @brief   Data message processor callback.
 * @param   none
 * @return  none*/
static void processMSGCmd( afIncomingMSGPacket_t *pkt ) {
  switch ( pkt->clusterId ) {
  case LOCATION_APP_ON_OFF_ID:
    switch ( pkt->cmd.Data[0] ) {
    case LOCATION_APP_OFF:
      HalLedSet( MAIN_LED, HAL_LED_MODE_OFF );
      break;
    case LOCATION_APP_TOGGLE:
      HalLedSet( MAIN_LED, HAL_LED_MODE_TOGGLE );
      break;
    case LOCATION_APP_ON:
      HalLedSet( MAIN_LED, HAL_LED_MODE_ON );
      break;
    }
    break;

  default:
    break;
  }
}

