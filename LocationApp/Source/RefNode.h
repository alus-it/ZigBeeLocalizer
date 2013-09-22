//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : RefNode.h
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#ifndef REFNODE_H
#define REFNODE_H

//INCLUDES
#include "ZComDef.h"

//CONSTANTS
#define ANNCE_EVT                 0x2000
#define SENS_EVT                  0x3000


#define ANNCE_DELAY               6000
#define REFNODE_DEFAULT_CYCLE     3000   //(in decimi di secondi) 5 min.
#define REFNODE_MIN_CYCLE         50     //(in decimi di secondi) 5 sec.
//GLOBAL VARIABLES
extern byte RefNode_TaskID;

//FUNCTIONS PROTOTYPES

//Task Initialization for the Location Application - Reference Node
void RefNode_Init( byte task_id );

//Task Event Processor for the Location Application - Reference Node
UINT16 RefNode_ProcessEvent( byte task_id, UINT16 events );

#endif  // #ifndef REFNODE_H

