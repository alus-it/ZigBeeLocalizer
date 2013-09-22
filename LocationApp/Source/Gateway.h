//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : Gateway.h
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// Git Repository : https://github.com/alus-it/ZigBeeLocalizer.git
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#ifndef GATEWAY_H
#define GATEWAY_H

#if !defined( ZDO_COORDINATOR )
  #define INT_HEAP_LEN=4608//Per il coordinatore è già definito in OnBoard.h
#endif
#define HAL_UART_BIG_TX_BUF=1
#define SPI_MGR_DEFAULT_MAX_TX_BUFF=1024
#define SPI_MGR_DEFAULT_OVERFLOW=1

#ifdef __cplusplus
extern "C"
{
#endif

//INCLUDES
#include "ZComDef.h"

#if defined (CC2430EB)
  #define SERIAL_PORT HAL_UART_PORT_0
#elif defined (CC2430DB)
  #define SERIAL_PORT HAL_UART_PORT_1
#elif defined (CC2430BB)
  #error //Serial port needed!!!
#endif

#if !defined( GATEWAY_BAUD )
  // CC2430 only allows 38.4k or 115.2k.
  #define GATEWAY_BAUD  HAL_UART_BR_38400
  //#define SERIAL_APP_BAUD  HAL_UART_BR_115200
#endif

// When the Rx buf space is less than this threshold, invoke the Rx callback.
#if !defined( GATEWAY_THRESH )
  #define GATEWAY_THRESH  48
#endif

#if !defined( GATEWAY_RX_MAX )
  #if (defined( HAL_UART_DMA )) && HAL_UART_DMA
    #define GATEWAY_RX_MAX  128
  #else
    /* The generic safe Rx minimum is 48, but if you know your PC App will not
     * continue to send more than a byte after receiving the ~CTS, lower max
     * here and safe min in _hal_uart.c to just 8.*/
    #define GATEWAY_RX_MAX  64
  #endif
#endif

#if !defined( GATEWAY_TX_MAX )
  #if (defined( HAL_UART_DMA )) && HAL_UART_DMA
  #define GATEWAY_TX_MAX  128
  #else
    #define GATEWAY_TX_MAX  64
  #endif
#endif

// Millisecs of idle time after a byte is received before invoking Rx callback.
#if !defined( GATEWAY_IDLE )
  #define GATEWAY_IDLE  6
#endif

// This is the desired byte count per OTA message.
#if !defined( GATEWAY_RX_CNT )
  #if (defined( HAL_UART_DMA )) && HAL_UART_DMA
    #define GATEWAY_RX_CNT  80
  #else
    #define GATEWAY_RX_CNT  6
  #endif
#endif

#define GATEWAY_RSP_CNT  4


// Delimiter
#define NUM_MAX_OF_BYTES  35 //See file "PktFormat.h" in workSpace
#define INIT_CHAR_PKT 0x3C //  <--> '<' carattere iniziale
#define END_CHAR_PKT 0x3E  //  <--> '>' carattere finale
#define ESC_CHAR_PKT 0x2F  //  <--> '/' carattere di escape

#define ZDO_NWKADDR_REQUEST

//Type of announce msgs
#define NODE_TYPE_REF    1
#define NODE_TYPE_BLIND  0

//First Byte of the data field specifying the command type (from the Serial Port)
#define TYPE_GET_DIAGNOSTIC              0x99
#define TYPE_GET_IEEE_ANNOUNCE           0x45
#define TYPE_GET_ANNOUNCE                0x40
#define TYPE_GET_PING                    0x42
#define TYPE_REF_CONFIG                  0xEE
#define TYPE_BLIND_CONFIG                0xCC
#define TYPE_RSSI_REQ                    0x21
#define TYPE_XY_REQ                      0x13
#define TYPE_GET_SENS                    0x33

//First Byte of the data field specifying the command type (to the Serial Port)
#define TYPE_PING_REPLY                  0x43


//length of MSG that the GW must send OTA
#define RSSI_SENS_ASYNC_REQ_CMD_LEN       1

#define GATEWAY_MSG_NEXT_EVT       0x0001
#define GATEWAY_MSG_RTRY_EVT       0x0002
#define GATEWAY_RSP_RTRY_EVT       0x0004
#define GATEWAY_RSP_LOST_EVT       0x0008
#define GATEWAY_MSG_SEND_EVT       0x0003

#define GATEWAY_MAX_RETRIES        10
#define GATEWAY_MSG_RTRY_TIMEOUT   50
#define GATEWAY_RSP_RTRY_TIMEOUT   100
#define GATEWAY_RSP_LOST_TIMEOUT   1000

// OTA Flow Control Delays
//#define SERIALAPP_ACK_DELAY          25
//#define SERIALAPP_NAK_DELAY          100
//fine roba vecchia

//GLOBAL VARIABLES
extern byte Gateway_TaskID;

//Task Initialization for the Location Application
extern void Gateway_Init( byte task_id );

//Task Event Processor for the Location Application
extern UINT16 Gateway_ProcessEvent( byte task_id, UINT16 events );

#ifdef __cplusplus
}
#endif

#endif //GATEWAY_H
