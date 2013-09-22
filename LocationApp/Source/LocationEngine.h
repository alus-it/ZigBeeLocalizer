//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : LocationEngine.h
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#ifndef LOCATIONENGINE_H
#define LOCATIONENGINE_H

#ifdef __cplusplus
extern "C"
{
#endif

//#if defined ( ZBIT )
//  #define CC2431
//#endif

#if defined ( CC2431 )

//INCLUDES
#include "ZComDef.h"

//CONSTANTS
#define LOC_ENGINE_NODE_CAPACITY_REVB   8
#define LOC_ENGINE_NODE_CAPACITY_REVC  16
// TBD - how to define/use min/max x/y?
#define LOC_ENGINE_X_MIN  0x00
#define LOC_ENGINE_X_MAX  0xff
#define LOC_ENGINE_Y_MIN  0x00
#define LOC_ENGINE_Y_MAX  0xff

//STRUCTURES
 typedef struct{
  byte x;
  byte y;
  byte rssi;
} LocRefNode_t;

typedef struct {
  byte  param_a;
  byte  param_n;
  byte  x;
  byte  y;
  byte  min;
} LocDevCfg_t;

extern void locationCalculatePosition( LocRefNode_t *ref, LocDevCfg_t *node );

#endif //CC2431

#ifdef __cplusplus
}
#endif

#endif //LOCATIONENGINE_H
