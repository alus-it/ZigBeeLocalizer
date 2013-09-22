//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : LocationApp.h
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#ifndef LOCATION_APP_H
#define LOCATION_APP_H

//Define solo per i nodi Reference e Blind
//#define LOC_NV_COORDS_REFNODES      0x1003   //FS
//#define SENS_NV_AUTO_MODE           0x1004   //FS

//Esempio cordinata x: 10,2 m
//Si hanno 2bit per i decimali quindi:
// 00 -> 0,0
// 01 -> 0,25
// 10 -> 0,5
// 11 -> 0,75
// 0,2 è più vicino a 0,25  -> 10,25 m
// 10,25*4=41 -HEX-> 0X29

//INCLUDES
#include "ZComDef.h"

//CONSTANTS
#define LOCATION_APP_VERSION                      2
#define MAIN_LED HAL_LED_3
#define SECONDARY_LED HAL_LED_2
#define LOCATION_GATEWAY_ENDPOINT                 203
#define LOCATION_REFNODE_ENDPOINT                 210
#define LOCATION_BLINDNODE_ENDPOINT               211
#define LOCATION_ALL_NODES_ENDPOINT               255

#define LOCATION_PROFID                           0xC003
#define LOCATION_REFNODE_DEVICEID                 0x0010
#define LOCATION_BLINDNODE_DEVICEID               0x0011
#define LOCATION_GATEWAY_DEVICEID                 0x0012
#define LOCATION_DEVICE_VERSION                   0
#define LOCATION_FLAGS                            0


#define TIME_UNIT                 100   //in ms Unità di tempo: 100ms=1 decimo
#define BLINDNODE_BLAST_COUNT       8   // Numero di Blast di ogni sequenza

#define RF_POWER_MIN_DBM  73 //83
#define RF_POWER_ZERO_DST 21 //30

// Cluster IDs
#define GET_ANNOUNCE                              0x0040
#define GET_IEEE_ANNOUNCE                         0x0045
#define BLIND_ANNCE                               0x0041
#define REF_ANNCE                                 0x0046
#define REF_CONFIG                                0x00EE
#define ACK_REF_CONFIG                            0x0011
#define BLIND_CONFIG                              0x00CC
#define ACK_BLIND_CONFIG                          0x00DD

#define SENS_REQUEST                              0x0033
#define SENS_PKT                                  0x0044
#define GET_DIAGNOSTIC                            0x0099
#define DIAGNOSTIC_PKT                            0x0088

#define RSSI_REQUEST                              0x0020
#define RSSI_RESPONSE                             0x0012
#define BLIND_XY_REQUEST                          0x0013
#define BLIND_XY_RESPONSE                         0x0014
#define BLIND_RSSI_REQUEST                        0x0021
#define BLIND_RSSI_RESPONSE                       0x0022


#define LOCATION_REFNODE_REQUEST_CONFIG           0x0017
#define LOCATION_BLINDNODE_REQUEST_CONFIG         0x0018
#define LOCATION_RSSI_BLAST                       0x0019
#define LOC_DEFAULT_X_Y                           0x0004 //1m
#define LOC_NO_ROOM_ASSIGNED                      0x0000 //no room

//ATTENZIONE! In origine LOC_DEFAULT_A era: 0x4E  //(39 << 1) //RSSI(dBm) at 1 m
#define LOC_DEFAULT_A                             40 //RSSI (dBm) at 1 m
#define LOC_DEFAULT_N                             16 // n_index parameter

// This profile uses the MSG AF service.
// LOCATION_XY_RSSI_REQUEST - message format
#define REFNODE_REQ_MSGS_IDX                      0

// Values for REFNODE_CONFIG_MODE_IDX and BLINDNODE_CONFIG_MODE_IDX
#define NODE_MODE_POLLED    0     // Only send responses to requests
#define NODE_MODE_AUTO      1     // Automatically send REFNODE response
#define RSP_TYPE_XY         1     // Sends the XY coord calculated with locEng
#define RSP_TYPE_RSSI       0     // Sends the collection of RSSI measurements

// RSSI_RESPONSE - message format
#define RSSI_RESP_X_HI_IDX                   0
#define RSSI_RESP_X_LO_IDX                   1
#define RSSI_RESP_Y_HI_IDX                   2
#define RSSI_RESP_Y_LO_IDX                   3
#define RSSI_RESP_ROOM_HI_IDX                4
#define RSSI_RESP_ROOM_LO_IDX                5
#define RSSI_RESP_A_IDX                      6
#define RSSI_RESP_N_IDX                      7
#define RSSI_RESP_RSSI_IDX                   8
#define RSSI_RESP_SIGMA2_IDX                 9
#define RSSI_RESP_LEN                       10

// BLIND_XY_RESPONSE - message format
#define BLIND_XY_RESPONSE_STATUS  0
#define BLIND_XY_RESPONSE_REFNUM  1
#define BLIND_XY_RESPONSE_HI_X    2
#define BLIND_XY_RESPONSE_LO_X    3
#define BLIND_XY_RESPONSE_HI_Y    4
#define BLIND_XY_RESPONSE_LO_Y    5
#define BLIND_XY_RESPONSE_HI_ROOM 6
#define BLIND_XY_RESPONSE_LO_ROOM 7
#define BLIND_XY_RESPONSE_LEN     8

// Values for Blind Node Response Status field
#define BLINDNODE_RSP_STATUS_SUCCESS                0
#define BLINDNODE_RSP_STATUS_NOT_ENOUGH_REFNODES    1
#define BLINDNODE_VERY_NEAR_REFNODE                 2

// LOCATION_BLINDNODE_BLAST
// NV Items
#define LOC_NV_REFNODE_CONFIG       0x1001
#define LOC_NV_BLINDNODE_CONFIG     0x1002

//GLOBAL VARIABLES
extern byte LoacationApp_TaskID;

//FUNCTIONS
void LoacationApp_NoACK( void );
void LoacationApp_Sleep( byte allow );
uint16 LoacationApp_ProcessEvent( uint8 task_id, uint16 events );

//Handles all key events for a LocationProfile device.
void LocationHandleKeys( unsigned char keys );

#endif  // #ifndef LOCATION_APP_H
