package com.whu.gnss.gnsslogger;

import android.os.Build;

final public class Constants {

    public static final int VER_2_11 = 0;
    public static final int VER_3_03 = 1;
    public static final int Nav_Yes=1;
    public static final int Nav_No=0;

    public static final int STATUS_SATELLITE_GREEN = 30;
    public static final int STATUS_SATELLITE_YELLOW = 14;
    //public static final int STATUS_SATELLITE_RED = 2;


    public  static final  String KEY_NAV="rinexNav";
    public  static final  int DEF_RINEX_NAV=Constants.Nav_No;

    public static final String KEY_RINEX_VER = "rinexVer"; // value 0=2.11, 1=3.03
    public static final String KEY_MARK_NAME = "markName";
    public static final String KEY_MARK_TYPE = "markType";
    public static final String KEY_OBSERVER_NAME = "observerName";
    public static final String KEY_OBSERVER_AGENCY_NAME = "observerAgencyName";
    public static final String KEY_RECEIVER_NUMBER = "receiverNumber";
    public static final String KEY_RECEIVER_TYPE = "receiverType";
    public static final String KEY_RECEIVER_VERSION = "receiverVersion";
    public static final String KEY_ANTENNA_NUMBER = "antennaNumber";
    public static final String KEY_ANTENNA_TYPE = "antennaType";
    public static final String KEY_ANTENNA_ECCENTRICITY_EAST = "antennaEccentricityEast";
    public static final String KEY_ANTENNA_ECCENTRICITY_NORTH = "antennaEccentricityNorth";
    public static final String KEY_ANTENNA_HEIGHT = "antennaHeight";

    public static final int DEF_RINEX_VER = Constants.VER_2_11;
    public static final String DEF_MARK_NAME = "GnssRecord";
    public static final String DEF_MARK_TYPE = "Geodetic";
    public static final String DEF_OBSERVER_NAME = "RINEX Logger user";
    public static final String DEF_OBSERVER_AGENCY_NAME = "GnssRecord";
    public static final String DEF_RECEIVER_NUMBER = Build.SERIAL;
    public static final String DEF_RECEIVER_TYPE = Build.MANUFACTURER;
    public static final String DEF_RECEIVER_VERSION = Build.PRODUCT;
    public static final String DEF_ANTENNA_NUMBER = Build.SERIAL;
    public static final String DEF_ANTENNA_TYPE = Build.PRODUCT;
    public static final String DEF_ANTENNA_ECCENTRICITY_EAST = "0.0000";
    public static final String DEF_ANTENNA_ECCENTRICITY_NORTH = "0.0000";
    public static final String DEF_ANTENNA_HEIGHT = "0.0000";

    public static final String RINEX_SETTING = "gnss_record_setting";





    /*
    spp setting
     */
    public static final String SPP_SETTING="spp setting";

    public static final String KEY_SPP_FILE="spp file";
    public static final int SPP_FILE=Constants.SPP_FILE_NO;
    public static final int SPP_FILE_YES=1;
    public static final int SPP_FILE_NO=0;


    public static final String KEY_SPP_MODEL="spp model";
    public static final int  DEF_SPP_MODEL=Constants.SPP_MODEL_SINGLE;

    public static final int SPP_MODEL_SINGLE=0;
    public static final int SPP_MODEL_DIFF=1;
    public static final int SPP_MODEL_ALL=2;


    public static final String KEY_GPS_SYSTEM="gps system";
    public static final int  DEF_GPS_SYSTEM=Constants.GPS_NO;

    public static final String KEY_GAL_SYSTEM="gal system";
    public static final int  DEF_GAL_SYSTEM=Constants.GAL_NO;

    public static final String KEY_GLO_SYSTEM="glo system";
    public static final int  DEF_GLO_SYSTEM=Constants.GLO_NO;

    public static final String KEY_BDS_SYSTEM="bds system";
    public static final int  DEF_BDS_SYSTEM=Constants.BDS_NO;

    public static final String KEY_QZSS_SYSTEM="qzss system";
    public static final int  DEF_QZSS_SYSTEM=Constants.QZSS_NO;

    public static final int GPS_YES=1;
    public static final int GPS_NO=0;
    public static final int GAL_YES=1;
    public static final int GAL_NO=0;
    public static final int GLO_YES=1;
    public static final int GLO_NO=0;
    public static final int BDS_YES=1;
    public static final int BDS_NO=0;
    public static final int QZSS_YES=1;
    public static final int QZSS_NO=0;

    public static final String KEY_NTRIP_HOST="ntrip host";
    public static final String DEF_NTRIP_HOST="ntrip.gnsslab.cn";

    public static final String KEY_NTRIP_PORT="ntrip port";
    public static final String  DEF_NTRIP_PORT="2101";

    public static final String KEY_NTRIP_USERNAME="ntrip username";
    public static final String  DEF_NTRIP_USERNAME="tfbai";

    public static final String KEY_NTRIP_PASSWORD="ntrip password";
    public static final String  DEF_NTRIP_PASSWARD="tfbai@2020";










}
