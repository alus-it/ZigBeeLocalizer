//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : BlindNode.h
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#ifndef BLINDNODE_H
#define BLINDNODE_H

#ifdef __cplusplus
extern "C"
{
#endif

//INCLUDES
#include "ZComDef.h"

//CONSTANTS
// Blind Node's Events (bit masked)
#define BLINDNODE_BLAST_EVT                0x4000
#define BLINDNODE_FIND_EVT                 0x2000
#define BLINDNODE_WAIT_EVT                 0x1000

//GLOBAL VARIABLES
extern byte BlindNode_TaskID;

//FUNCTIONS PROTOTYPES
extern void BlindNode_Init(byte task_id);
extern UINT16 BlindNode_ProcessEvent(byte task_id, UINT16 events);

#ifdef __cplusplus
}
#endif

#endif /* BLINDNODE_H */
