package com.whu.gnss.gnsslogger;

public class ConstantSystem {


    public  static final char GPS_SYSTEM='G';
    public  static final char GLONASS_SYSTEM='R';
    public static final char QZSS_SYSTEM='J';
    public  static final char BEIDOU_SYSTEM='C';
    public  static final char GALILEO_SYSTEM='E';
    public  static final char SBAS_SYSTEM='S';

    public static final int MAXSATIDGPS=32;
    public static final int MINSATIDGPS=1;

    public static final int MAXSATIDQZSS=199;
    public static final int MINSATIDQZSS=193;

    public static final int MAXSATIDGLO=24;
    public static final int MINSATIDGLO=1;

    public static final int MAXSATIDGAL=30;
    public static final int MINSATIDGAL=1;
    //先定义35颗
    public static final int MAXSATIDBDS=35;
    public static final int MINSATIDBDS=1;


    //在写程序的过程中发现  ftp获取的gps广播星历中的sqrt(A)与ntrip获取的gps广播星历中的sqrt(A)相减为一个定值8192

    public static final double UNKNOWN=8192.0;


}
