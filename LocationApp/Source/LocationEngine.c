//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : LocationEngine.c
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// Git Repository : https://github.com/alus-it/ZigBeeLocalizer.git
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#if defined ( CC2431 ) //E' necessario usare il CC2431

//INCLUDES
#include "ZComDef.h"
#include "LocationEngine.h"
#include "hal_mcu.h"

//MACROS
#define XREG(addr)       ((unsigned char volatile __xdata *) 0)[addr]
#define  REFCOORD    XREG( 0xDF55 )  //Location Engine 
#define  MEASPARM    XREG( 0xDF56 )  //Location Engine
#define  LOCENG      XREG( 0xDF57 )  //Location Engine
#define  LOCX        XREG( 0xDF58 )  //Location Engine
#define  LOCY        XREG( 0xDF59 )  //Location Engine
#define  LOCMIN      XREG( 0xDF5A )  //Location Engine

//Location engine enable
#define LOC_ENABLE()  do { LOCENG |= 0x10;  } while(0)
#define LOC_DISABLE() do { LOCENG &= ~0x10; } while(0)

//Location engine load parameters
#define LOC_PARAMETER_LOAD( on )\
   do {                         \
      if(on) LOCENG |= 0x04;    \
      else LOCENG &= ~0x04;     \
   } while(0)

//Location engine load reference coordinates
#define LOC_REFERENCE_LOAD( on )\
   do {                         \
      if(on) LOCENG |= 0x02;    \
      else LOCENG &= ~0x02;     \
} while(0)

//Location engine run
#define LOC_RUN() do { LOCENG |= 0x01; } while(0)

//Location engine done
#define LOC_DONE()  (LOCENG & 0x08)

/* @fn      locationCalculatePosition
 * @brief   Calculates the position of this device.  This function
 *          only works in a 2431.
 * @param   ref - input - Reference Nodes
 * @param   node - input/output - This node's information
 * @return  none*/
void locationCalculatePosition(LocRefNode_t *ref, LocDevCfg_t *node) {
  LocRefNode_t *pRef = ref;
  // Rev-B Chip have LocEng Ver 1.0 w/ cap=8, Rev-C have LocEng Ver 2.0 w/ 16.
  const byte stop=((CHVER==0x01)?LOC_ENGINE_NODE_CAPACITY_REVB:LOC_ENGINE_NODE_CAPACITY_REVC);
  byte idx;
  LOC_DISABLE();	
  LOC_ENABLE();
  LOC_REFERENCE_LOAD( TRUE ); // Load the reference coordinates.
  for(idx = 0; idx < stop; idx++) {
    REFCOORD = pRef->x;		
    REFCOORD = pRef->y;
    pRef++;
  }			
  LOC_REFERENCE_LOAD( FALSE );
  LOC_PARAMETER_LOAD( TRUE ); // Load the measured parameters.
  MEASPARM = node->param_a;  	
  MEASPARM = node->param_n;
  if(CHVER != 0x01) { // CC2431 rev. C->
    MEASPARM = LOC_ENGINE_X_MIN;
    MEASPARM = LOC_ENGINE_X_MAX;
    MEASPARM = LOC_ENGINE_Y_MIN;
    MEASPARM = LOC_ENGINE_Y_MAX;
  } // Load the measured RSSI values shifted for not used fractional bit.
  for(idx=0;idx<stop;idx++,ref++) MEASPARM = ref->rssi * 2;
  LOC_PARAMETER_LOAD( FALSE );	
  LOC_RUN();
  while(!(LOCENG & 0x08));
  if(CHVER==0x01) {//Convert output format(LSB=.5m)to input format(2 LSB's=.25m)
    node->x = (LOCX << 1);
    node->y = (LOCY << 1);
    node->min = LOCMIN;
  } else {
    node->x = LOCX + 2;
    node->y = LOCY;
    node->min = 0;
  }
  LOC_DISABLE();	
}

#endif //CC2431
