//=======================================================================================
// This file is part of: ZigBee Localizer - DistributedLocationApp - ZigBee Firmware
// A localization system for ZigBee networks, based on the analysis of RSSI.
// 
// Name           : SensorID.h
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// Git Repository : https://github.com/alus-it/ZigBeeLocalizer.git
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

uint8 findVectAvblSensId(byte vectSensId[]);
uint16 readSensValue(uint8 sensId);

//Per quanto rigurada le letture dell'accelerometro:
//si nota una variazione nei bit dell'ADC se e solo se HAL_ADC_REF_VOLT viene settato a HAL_ADC_REF_AVDD nel file hal_adc.c

#define NUM_MAX_SENS 7

// Teperature
#define SENS_TEMP_ID 0x00
#define SENS_TEMP_RES HAL_ADC_RESOLUTION_12
#define SENS_TEMP_CH 0x0E

// Battery Reading
#define SENS_BATT_ID 0x01
#define SENS_BATT_RES HAL_ADC_RESOLUTION_12
#define SENS_BATT_CH 0x0F

#if defined ( CC2430DB )
  // Light Reading
  #include "hal_lcd.h"
  #define SENS_LIGHT_ID 0x02
  #define SENS_LIGHT_RES HAL_ADC_RESOLUTION_8
  #define SENS_LIGHT_CH HAL_ADC_CHANNEL_0

 // X_Acc Reading
  #define SENS_X_ACC_ID 0x03
  #define SENS_X_ACC_RES HAL_ADC_RESOLUTION_14
  #define SENS_X_ACC_CH HAL_ADC_CHANNEL_4

  // Y_Acc Reading
  #define SENS_Y_ACC_ID 0x04
  #define SENS_Y_ACC_RES HAL_ADC_RESOLUTION_14
  #define SENS_Y_ACC_CH HAL_ADC_CHANNEL_5
#endif

#if defined ( CC2430DB )
  // Potentiometer
  #define SENS_POTENT_ID 0x05
  #define SENS_POTENT_RES HAL_ADC_RESOLUTION_14
  #define SENS_POTENT_CH HAL_ADC_CHANNEL_7
#endif

#if defined ( CC2430EB )
  // Potentiometer
  #define SENS_POTENT_ID 0x05
  #define SENS_POTENT_RES HAL_ADC_RESOLUTION_14
  #define SENS_POTENT_CH HAL_ADC_CHANNEL_7
#endif

// Switch
//#define SENS_SWITCH_ID 0x06
//#define SENS_SWITCH_RES HAL_ADC_RESOLUTION_8
//#define SENS_SWITCH_CH HAL_ADC_CHANNEL_6
