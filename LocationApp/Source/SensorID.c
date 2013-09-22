//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : SensorID.c
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// SVN Repository : https://zigbeelocalizer.svn.sourceforge.net/svnroot/zigbeelocalizer
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

#include "ZComDef.h"
#include "hal_adc.h"
#include "LocationApp.h"
#include "SensorID.h"

uint8 findVectAvblSensId(uint8 vectSensId[]) {
  uint8 numOfSens = 0;
  #if defined ( SENS_TEMP_ID )
    vectSensId[numOfSens] = SENS_TEMP_ID;
    numOfSens++;
  #endif
  #if defined ( SENS_BATT_ID )
    vectSensId[numOfSens] = SENS_BATT_ID;
    numOfSens++;
  #endif
  #if defined ( SENS_LIGHT_ID )
    vectSensId[numOfSens] = SENS_LIGHT_ID;
    numOfSens++;
  #endif
  #if defined ( SENS_X_ACC_ID )
    vectSensId[numOfSens] = SENS_X_ACC_ID;
    numOfSens++;
  #endif
  #if defined ( SENS_Y_ACC_ID )
    vectSensId[numOfSens] = SENS_Y_ACC_ID;
    numOfSens++;
  #endif
  #if defined ( SENS_POTENT_ID )
    vectSensId[numOfSens] = SENS_POTENT_ID;
    numOfSens++;
  #endif
  #if defined ( SENS_SWITCH_ID )
    vectSensId[numOfSens] = SENS_SWITCH_ID;
    numOfSens++;
  #endif
  return numOfSens;
}

uint16 readSensValue(uint8 sensId) {
  uint16 sensVal;
  switch(sensId) {
    #if defined ( SENS_TEMP_ID )
    case SENS_TEMP_ID: //Temperatura
      HalAdcInit();
      sensVal=HalAdcRead(SENS_TEMP_CH, SENS_TEMP_RES); //X leggere i valori
      break;
    #endif
    #if defined ( SENS_BATT_ID )
    case SENS_BATT_ID: //Valore di tensione della batteria
      HalAdcInit();
      sensVal=HalAdcRead(SENS_BATT_CH, SENS_BATT_RES); //ris sempre su 12 bit
      break;
    #endif
    #if defined ( SENS_LIGHT_ID )
    case SENS_LIGHT_ID: //Luce
      HalAdcInit();
      sensVal=HalAdcRead(SENS_LIGHT_CH, SENS_LIGHT_RES);
      break;
    #endif
    #if defined ( SENS_X_ACC_ID )
    case SENS_X_ACC_ID: //X_Accelerometro
      HalAdcInit();
      sensVal=HalAdcRead(SENS_X_ACC_CH, SENS_X_ACC_RES);
      break;
    #endif
    #if defined ( SENS_Y_ACC_ID )
    case SENS_Y_ACC_ID: //Y_Accelerometro
      HalAdcInit();
      sensVal=HalAdcRead(SENS_Y_ACC_CH, SENS_Y_ACC_RES);
      break;
    #endif
    #if defined ( SENS_POTENT_ID )
    case SENS_POTENT_ID: //Potenziometro
      HalAdcInit();
      sensVal=HalAdcRead(SENS_POTENT_CH, SENS_POTENT_RES);
      break;
    #endif
    #if defined ( SENS_SWITCH_ID )
    case SENS_SWITCH_ID: //Switch
      HalAdcInit();
      sensVal=HalAdcRead(SENS_SWITCH_CH, SENS_SWITCH_RES);
      break;
    #endif
    default:
      sensVal=0xFFFF; //caso in cui non è stato trovato o definito sensId
  }
  return sensVal;
}
