package com.whu.gnss.gnsslogger.nav;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.GnssNavigationMessage;
import android.util.Log;

import com.whu.gnss.gnsslogger.constellations.GpsTime;
import com.whu.gnss.gnsslogger.GNSSConstants;

import java.util.concurrent.TimeUnit;


/**
 * Created by KuboLab on 2017/12/04.
 * Continue now
 */

//テスト
public class GpsNavigationConv {
    private static final byte IONOSPHERIC_PARAMETERS_PAGE_18_SV_ID = 56;

    private static final int WORD_SIZE_BITS = 30;
    private static final int WORD_PADDING_BITS = 2;
    private static final int BYTE_AS_BITS = 8;
    private static final int TYPE_BDS_D1 = 1281;
    private static final int TYPE_BDS_D2 = 1282;
    private static final int TYPE_GAL_F = 1538;
    private static final int TYPE_GAL_I = 1537;
    private static final int TYPE_GLO_L1CA = 769;
    private static final int TYPE_GPS_CNAV2 = 260;
    private static final int TYPE_GPS_L1CA = 257;
    private static final int TYPE_GPS_L2CNAV = 258;
    private static final int TYPE_GPS_L5CA = 259;


    private static final double POW_2_4 = Math.pow(2, 4);
    private static final double POW_2_11 = Math.pow(2, 11);
    private static final double POW_2_12 = Math.pow(2, 12);
    private static final double POW_2_14 = Math.pow(2, 14);
    private static final double POW_2_16 = Math.pow(2, 16);
    private static final double POW_2_NEG_5 = Math.pow(2, -5);
    private static final double POW_2_NEG_19 = Math.pow(2, -19);
    private static final double POW_2_NEG_24 = Math.pow(2, -24);
    private static final double POW_2_NEG_27 = Math.pow(2, -27);
    private static final double POW_2_NEG_29 = Math.pow(2, -29);
    private static final double POW_2_NEG_30 = Math.pow(2, -30);
    private static final double POW_2_NEG_31 = Math.pow(2, -31);
    private static final double POW_2_NEG_33 = Math.pow(2, -33);
    private static final double POW_2_NEG_43 = Math.pow(2, -43);
    private static final double POW_2_NEG_55 = Math.pow(2, -55);

    private static final long INTEGER_RANGE = 0xFFFFFFFFL;
    // 3657 is the number of days between the unix epoch and GPS epoch as the GPS epoch started on
    // Jan 6, 1980
    private static final long GPS_EPOCH_AS_UNIX_EPOCH_MS = TimeUnit.DAYS.toMillis(3657);
    // A GPS Cycle is 1024 weeks, or 7168 days
    private static final long GPS_CYCLE_MS = TimeUnit.DAYS.toMillis(7168);

    /**
     * Maximum possible number of GPS satellites
     */
    public static final int MAX_NUMBER_OF_SATELLITES = 32;

    private static final int L1_CA_MESSAGE_LENGTH_BYTES = 40;

    private static final int IODC1_INDEX = 82;
    private static final int IODC1_LENGTH = 2;
    private static final int IODC2_INDEX = 210;
    private static final int IODC2_LENGTH = 8;
    private static final int WEEK_INDEX = 60;
    private static final int WEEK_LENGTH = 10;
    private static final int URA_INDEX = 72;
    private static final int URA_LENGTH = 4;
    private static final int SV_HEALTH_INDEX = 76;
    private static final int SV_HEALTH_LENGTH = 6;
    private static final int TGD_INDEX = 196;
    private static final int TGD_LENGTH = 8;
    private static final int AF2_INDEX = 240;
    private static final int AF2_LENGTH = 8;
    private static final int AF1_INDEX = 248;
    private static final int AF1_LENGTH = 16;
    private static final int AF0_INDEX = 270;
    private static final int AF0_LENGTH = 22;
    private static final int IODE1_INDEX = 60;
    private static final int IODE_LENGTH = 8;
    private static final int TOC_INDEX = 218;
    private static final int TOC_LENGTH = 16;
    private static final int CRS_INDEX = 68;
    private static final int CRS_LENGTH = 16;
    private static final int DELTA_N_INDEX = 90;
    private static final int DELTA_N_LENGTH = 16;
    private static final int M0_INDEX8 = 106;
    private static final int M0_INDEX24 = 120;
    private static final int CUC_INDEX = 150;
    private static final int CUC_LENGTH = 16;
    private static final int E_INDEX8 = 166;
    private static final int E_INDEX24 = 180;
    private static final int CUS_INDEX = 210;
    private static final int CUS_LENGTH = 16;
    private static final int A_INDEX8 = 226;
    private static final int A_INDEX24 = 240;
    private static final int TOE_INDEX = 270;
    private static final int TOE_LENGTH = 16;
    private static final int IODE2_INDEX = 270;
    private static final int CIC_INDEX = 60;
    private static final int CIC_LENGTH = 16;
    private static final int O0_INDEX8 = 76;
    private static final int O0_INDEX24 = 90;
    private static final int O_INDEX8 = 196;
    private static final int O_INDEX24 = 210;
    private static final int ODOT_INDEX = 240;
    private static final int ODOT_LENGTH = 24;
    private static final int CIS_INDEX = 120;
    private static final int CIS_LENGTH = 16;
    private static final int I0_INDEX8 = 136;
    private static final int I0_INDEX24 = 150;
    private static final int CRC_INDEX = 180;
    private static final int CRC_LENGTH = 16;
    private static final int IDOT_INDEX = 278;
    private static final int IDOT_LENGTH = 14;

    private static final int A0_INDEX = 68;
    private static final int A_B_LENGTH = 8;
    private static final int A1_INDEX = 76;
    private static final int A2_INDEX = 90;
    private static final int A3_INDEX = 98;
    private static final int B0_INDEX = 106;
    private static final int B1_INDEX = 120;
    private static final int B2_INDEX = 128;
    private static final int B3_INDEX = 136;
    private static final int WN_LS_INDEX = 226;
    private static final int DELTA_T_LS_INDEX = 240;
    private static final int TOT_LS_INDEX = 218;
    private static final int DN_LS_INDEX = 256;
    private static final int WNF_LS_INDEX = 248;
    private static final int DELTA_TF_LS_INDEX = 270;
    private static final int I0UTC_INDEX8 = 210;
    private static final int I0UTC_INDEX24 = 180;
    private static final int I1UTC_INDEX = 150;


    //建立数据库的时候需要context
    private Context context;
    //建立数据库
    public  SQLiteManager sqliteManager;

    private SQLiteDatabase NavDB;

    //这个和prn结合起来相当于主键的作用
    //每个卫星对应一个mtocScaled
    private double[] mtocScaled = new double[50];

    public GpsNavigationConv(Context context) {
        this.context = context;
        //这个就相当于建立数据库
        sqliteManager = new SQLiteManager(context);

    }



    public void onGpsNavMessageReported(int prn, int type, int subframe, byte[] rawData) {

        if (type == GnssNavigationMessage.TYPE_GPS_L1CA || type == GnssNavigationMessage.TYPE_GPS_L5CNAV) {
            switch (subframe) {
                case 1:
                    //处理数据
                    handleFirstSubframe(prn, rawData);
                    Log.i("Navigation", getNAVType(type) + String.valueOf(prn) + "First SubFrame");
                    break;
                case 2:
                    handleSecondSubframe(prn, rawData);
                    Log.i("Navigation", getNAVType(type) + String.valueOf(prn) + "Second SubFrame");
                    break;
                case 3:
                    handleThirdSubframe(prn, rawData);
                    Log.i("Navigation", getNAVType(type) + String.valueOf(prn) + "Third SubFrame");
                    break;
                case 4:
                    handleFourthSubframe(18, rawData);
                    Log.i("Navigation", getNAVType(type) + String.valueOf(prn) + "Forth SubFrame");
                    break;
                case 5:
                    break;
                default:
                    // invalid message id
                    throw new IllegalArgumentException("Invalid Subframe ID: " + subframe);
            }
        }


    }




    public StringBuilder getNavgationMessageToRinex3Body() {
        StringBuilder stringBuilder = new StringBuilder();

        NavDB = sqliteManager.getWritableDatabase();

        //查询ephgpsBook表中所有数据
        Cursor cursor = NavDB.query("ephgpsBook", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                //遍历  cursor 取出数据并打印为stringbuilder
                int svid = cursor.getInt(cursor.getColumnIndex("Svid"));
                @SuppressLint("DefaultLocale") String prn = String.format("G%02d", svid);
                double Toc = cursor.getDouble(cursor.getColumnIndex("Toc"));

                double af0 = cursor.getDouble(cursor.getColumnIndex("af0"));

                double af1 = cursor.getDouble(cursor.getColumnIndex("af1"));

                double af2 = cursor.getDouble(cursor.getColumnIndex("af2"));

                double iode = cursor.getDouble(cursor.getColumnIndex("iode"));

                double Crs = cursor.getDouble(cursor.getColumnIndex("Crs"));

                double delta_n = cursor.getDouble(cursor.getColumnIndex("delta_n"));

                double M0 = cursor.getDouble(cursor.getColumnIndex("M0"));

                double Cuc = cursor.getDouble(cursor.getColumnIndex("Cuc"));

                double es = cursor.getDouble(cursor.getColumnIndex("es"));

                double Cus = cursor.getDouble(cursor.getColumnIndex("Cus"));

                double sqrtA = cursor.getDouble(cursor.getColumnIndex("sqrtA"));

                double Toe = cursor.getInt(cursor.getColumnIndex("Toe"));

                double Cic = cursor.getDouble(cursor.getColumnIndex("Cic"));

                double Omega_0 = cursor.getDouble(cursor.getColumnIndex("Omega_0"));

                double Cis = cursor.getDouble(cursor.getColumnIndex("Cis"));

                double i0 = cursor.getDouble(cursor.getColumnIndex("i0"));

                double Crc = cursor.getDouble(cursor.getColumnIndex("Crc"));

                double w = cursor.getDouble(cursor.getColumnIndex("w"));

                double Omega_dot = cursor.getDouble(cursor.getColumnIndex("Omega_dot"));


                double i_dot = cursor.getDouble(cursor.getColumnIndex("i_dot"));

                double L2code = cursor.getDouble(cursor.getColumnIndex("L2code"));


                double gpsweek = cursor.getDouble(cursor.getColumnIndex("gpsweek"));

                double L2Flag = cursor.getInt(cursor.getColumnIndex("L2Flag"));

                double svAccur = cursor.getInt(cursor.getColumnIndex("svAccur"));

                double svHealth = cursor.getInt(cursor.getColumnIndex("svHealth"));

                double iodc = cursor.getDouble(cursor.getColumnIndex("iodc"));

                double TGD = cursor.getDouble(cursor.getColumnIndex("TGD"));

                //计算从1980.1.6到此刻的纳秒,计算GPS时间
                //1024*2  是2048周    获取到的gpsweek 是总周数除以1024的余数。

                //没有考虑到数据库中一个卫星数据不全的问题
                double nanos = (gpsweek + 1024 * 2) * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK + Toc * 1e9;

                GpsTime gpsTime = new GpsTime(nanos);

                int year = gpsTime.getYear();
                int month = gpsTime.getMonth();
                int day = gpsTime.getDay();
                int hour = gpsTime.getHour();
                int minute = gpsTime.getMinute();
                double second = gpsTime.getSecond();

                @SuppressLint("DefaultLocale") String time = String.format("%d %02d %02d %02d %02d %02.0f", year, month, day, hour, minute, second);

                Log.d("gpstime", time);

                stringBuilder.append(prn).append(" ");
                stringBuilder.append(time);
                @SuppressLint("DefaultLocale") String epoch_clock = String.format("%19.12E%19.12E%19.12E", af0, af1, af2);
                epoch_clock = epoch_clock.replace("E", "D");
                Log.d("epoch_clock", epoch_clock);
                stringBuilder.append(epoch_clock);
                //按行
                stringBuilder.append('\n');

                //广播轨道1参数
                @SuppressLint("DefaultLocale") String broadcastorbit_1 = String.format("    %19.12E%19.12E%19.12E%19.12E", iode, Crs, delta_n, M0);
                broadcastorbit_1 = broadcastorbit_1.replace("E", "D");
                stringBuilder.append(broadcastorbit_1);
                stringBuilder.append('\n');

                //广播轨道2参数
                @SuppressLint("DefaultLocale") String broadcastorbit_2 = String.format("    %19.12E%19.12E%19.12E%19.12E", Cuc, es, Cus, sqrtA);
                broadcastorbit_2 = broadcastorbit_2.replace("E", "D");
                stringBuilder.append(broadcastorbit_2);
                stringBuilder.append('\n');

                //广播轨道3参数
                @SuppressLint("DefaultLocale") String broadcastorbit_3 = String.format("    %19.12E%19.12E%19.12E%19.12E", Toe, Cic, Omega_0, Cis);
                broadcastorbit_3 = broadcastorbit_3.replace("E", "D");
                stringBuilder.append(broadcastorbit_3);
                stringBuilder.append('\n');

                //广播轨道4参数
                @SuppressLint("DefaultLocale") String broadcastorbit_4 = String.format("    %19.12E%19.12E%19.12E%19.12E", i0, Crc, w, Omega_dot);
                broadcastorbit_4 = broadcastorbit_4.replace("E", "D");
                stringBuilder.append(broadcastorbit_4);
                stringBuilder.append('\n');

                //广播轨道5参数
                @SuppressLint("DefaultLocale") String broadcastorbit_5 = String.format("    %19.12E%19.12E%19.12E%19.12E", i_dot, L2code, gpsweek, L2Flag);
                broadcastorbit_5 = broadcastorbit_5.replace("E", "D");
                stringBuilder.append(broadcastorbit_5);
                stringBuilder.append('\n');

                //广播轨道6参数
                @SuppressLint("DefaultLocale") String broadcastorbit_6 = String.format("    %19.12E%19.12E%19.12E%19.12E", svAccur, svHealth, TGD, iodc);
                broadcastorbit_6 = broadcastorbit_6.replace("E", "D");
                stringBuilder.append(broadcastorbit_6);
                stringBuilder.append('\n');
                //广播轨道7参数
                stringBuilder.append('\n');
                stringBuilder.append('\n');

            } while (cursor.moveToNext());

        }
        cursor.close();

        return stringBuilder;

    }

    @SuppressLint("DefaultLocale")
    public StringBuilder getNavgationMessageToRinex2Body() {
        StringBuilder stringBuilder = new StringBuilder();

        NavDB = sqliteManager.getWritableDatabase();

        try
        {
            //查询ephgpsBook表中所有数据
            Cursor cursor = NavDB.query("ephgpsBook", null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    //遍历  cursor 取出数据并打印为stringbuilder
                    int svid = cursor.getInt(cursor.getColumnIndex("Svid"));
                    @SuppressLint("DefaultLocale") String prn = String.format("G%02d", svid);
                    double Toc = cursor.getDouble(cursor.getColumnIndex("Toc"));

                    double af0 = cursor.getDouble(cursor.getColumnIndex("af0"));

                    double af1 = cursor.getDouble(cursor.getColumnIndex("af1"));

                    double af2 = cursor.getDouble(cursor.getColumnIndex("af2"));

                    double iode = cursor.getDouble(cursor.getColumnIndex("iode"));

                    double Crs = cursor.getDouble(cursor.getColumnIndex("Crs"));

                    double delta_n = cursor.getDouble(cursor.getColumnIndex("delta_n"));

                    double M0 = cursor.getDouble(cursor.getColumnIndex("M0"));

                    double Cuc = cursor.getDouble(cursor.getColumnIndex("Cuc"));

                    double es = cursor.getDouble(cursor.getColumnIndex("es"));

                    double Cus = cursor.getDouble(cursor.getColumnIndex("Cus"));

                    double sqrtA = cursor.getDouble(cursor.getColumnIndex("sqrtA"));

                    double Toe = cursor.getInt(cursor.getColumnIndex("Toe"));

                    double Cic = cursor.getDouble(cursor.getColumnIndex("Cic"));

                    double Omega_0 = cursor.getDouble(cursor.getColumnIndex("Omega_0"));

                    double Cis = cursor.getDouble(cursor.getColumnIndex("Cis"));

                    double i0 = cursor.getDouble(cursor.getColumnIndex("i0"));

                    double Crc = cursor.getDouble(cursor.getColumnIndex("Crc"));

                    double w = cursor.getDouble(cursor.getColumnIndex("w"));

                    double Omega_dot = cursor.getDouble(cursor.getColumnIndex("Omega_dot"));


                    double i_dot = cursor.getDouble(cursor.getColumnIndex("i_dot"));

                    double L2code = cursor.getDouble(cursor.getColumnIndex("L2code"));


                    double gpsweek = cursor.getDouble(cursor.getColumnIndex("gpsweek"));

                    double L2Flag = cursor.getInt(cursor.getColumnIndex("L2Flag"));

                    double svAccur = cursor.getInt(cursor.getColumnIndex("svAccur"));

                    double svHealth = cursor.getInt(cursor.getColumnIndex("svHealth"));

                    double iodc = cursor.getDouble(cursor.getColumnIndex("iodc"));

                    double TGD = cursor.getDouble(cursor.getColumnIndex("TGD"));

                    //计算从1980.1.6到此刻的纳秒,计算GPS时间
                    //1024*2  是2048周    获取到的gpsweek 是总周数除以1024的余数。

                    //没有考虑到数据库中一个卫星数据不全的问题
                    double nanos = (gpsweek + 1024 * 2) * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK + Toc * 1e9;

                    GpsTime gpsTime = new GpsTime(nanos);

                    int year = gpsTime.getYearSimplify();
                    int month = gpsTime.getMonth();
                    int day = gpsTime.getDay();
                    int hour = gpsTime.getHour();
                    int minute = gpsTime.getMinute();
                    double second = gpsTime.getSecond();

                    @SuppressLint("DefaultLocale") String time = String.format(" %d %2d %2d %2d %2d %5.1f", year, month, day, hour, minute, second);

                    Log.d("gpstime", time);

                    stringBuilder.append(String.format("%2d",svid));
                    stringBuilder.append(time);
                    @SuppressLint("DefaultLocale") String epoch_clock = String.format("%19.12E%19.12E%19.12E", af0, af1, af2);
                    epoch_clock = epoch_clock.replace("E", "D");
                    Log.d("epoch_clock", epoch_clock);
                    stringBuilder.append(epoch_clock);
                    //按行
                    stringBuilder.append('\n');

                    //广播轨道1参数
                    @SuppressLint("DefaultLocale") String broadcastorbit_1 = String.format("    %19.12E%19.12E%19.12E%19.12E", iode, Crs, delta_n, M0);
                    broadcastorbit_1 = broadcastorbit_1.replace("E", "D");
                    stringBuilder.append(broadcastorbit_1);
                    stringBuilder.append('\n');

                    //广播轨道2参数
                    @SuppressLint("DefaultLocale") String broadcastorbit_2 = String.format("    %19.12E%19.12E%19.12E%19.12E", Cuc, es, Cus, sqrtA);
                    broadcastorbit_2 = broadcastorbit_2.replace("E", "D");
                    stringBuilder.append(broadcastorbit_2);
                    stringBuilder.append('\n');

                    //广播轨道3参数
                    @SuppressLint("DefaultLocale") String broadcastorbit_3 = String.format("    %19.12E%19.12E%19.12E%19.12E", Toe, Cic, Omega_0, Cis);
                    broadcastorbit_3 = broadcastorbit_3.replace("E", "D");
                    stringBuilder.append(broadcastorbit_3);
                    stringBuilder.append('\n');

                    //广播轨道4参数
                    @SuppressLint("DefaultLocale") String broadcastorbit_4 = String.format("    %19.12E%19.12E%19.12E%19.12E", i0, Crc, w, Omega_dot);
                    broadcastorbit_4 = broadcastorbit_4.replace("E", "D");
                    stringBuilder.append(broadcastorbit_4);
                    stringBuilder.append('\n');

                    //广播轨道5参数
                    @SuppressLint("DefaultLocale") String broadcastorbit_5 = String.format("    %19.12E%19.12E%19.12E%19.12E", i_dot, L2code, gpsweek, L2Flag);
                    broadcastorbit_5 = broadcastorbit_5.replace("E", "D");
                    stringBuilder.append(broadcastorbit_5);
                    stringBuilder.append('\n');

                    //广播轨道6参数
                    @SuppressLint("DefaultLocale") String broadcastorbit_6 = String.format("    %19.12E%19.12E%19.12E%19.12E", svAccur, svHealth, TGD, iodc);
                    broadcastorbit_6 = broadcastorbit_6.replace("E", "D");
                    stringBuilder.append(broadcastorbit_6);
                    stringBuilder.append('\n');
                    //广播轨道7参数
                    stringBuilder.append('\n');
                    stringBuilder.append('\n');

                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        catch (Exception e)
        {

        }


        return stringBuilder;

    }

    @SuppressLint("DefaultLocale")
    public StringBuilder getNavigationMessageToRinex3Header() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("     3.03           N: GNSS NAV DATA    G: GPS              RINEX VERSION / TYPE");
        stringBuilder.append('\n');
        stringBuilder.append("Converto v3.5.2     BTFyoudianleiaa                          PGM / RUN BY / DATE");
        stringBuilder.append('\n');
        NavDB = sqliteManager.getWritableDatabase();

        //查询IONOSPHERIC表中所有数据
        try {
        Cursor cursor = NavDB.query("IONOSPHERIC", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                double GPSA0 = cursor.getDouble(cursor.getColumnIndex("GPSA0"));
                double GPSA1 = cursor.getDouble(cursor.getColumnIndex("GPSA1"));
                double GPSA2 = cursor.getDouble(cursor.getColumnIndex("GPSA2"));
                double GPSA3 = cursor.getDouble(cursor.getColumnIndex("GPSA3"));
                double GPSB0 = cursor.getDouble(cursor.getColumnIndex("GPSB0"));
                double GPSB1 = cursor.getDouble(cursor.getColumnIndex("GPSB1"));
                double GPSB2 = cursor.getDouble(cursor.getColumnIndex("GPSB2"));
                double GPSB3 = cursor.getDouble(cursor.getColumnIndex("GPSB3"));

                stringBuilder.append(String.format("GPSA  %12.4E %12.4E %12.4E %12.4E       IONOSPHERIC CORR", GPSA0, GPSA1, GPSA2, GPSA3));
                stringBuilder.append('\n');
                stringBuilder.append(String.format("GPSB  %12.4E %12.4E %12.4E %12.4E       IONOSPHERIC CORR", GPSB0, GPSB1, GPSB2, GPSB3));
                stringBuilder.append('\n');

            } while (cursor.moveToNext());
        }
        cursor.close();
        } catch (Exception e) {

        }
        //查询UTC表中所有数据
        try {
        Cursor cursor1 = NavDB.query("UTC", null, null, null, null, null, null);

        if (cursor1.moveToFirst()) {
            do {
                double a0UTC = cursor1.getDouble(cursor1.getColumnIndex("a0UTC"));
                double a1UTC = cursor1.getDouble(cursor1.getColumnIndex("a1UTC"));

                double tot = cursor1.getDouble(cursor1.getColumnIndex("tot"));
                double wnt = cursor1.getDouble(cursor1.getColumnIndex("wnt"));

                if (tot > 0 && wnt > 0) {
                    stringBuilder.append(String.format("GPUT %19.12E%19.12E%9.0f%9.0f   TIME SYSTEM CORR", a0UTC, a1UTC, tot, wnt));
                    stringBuilder.append('\n');
                }

            } while (cursor1.moveToNext());
        }
        cursor1.close();
        } catch (Exception e) {
        }
        try {
        Cursor cursor2 = NavDB.query("LEAPSECOND", null, null, null, null, null, null);

        if (cursor2.moveToFirst()) {
            do {
                double tls = cursor2.getDouble(cursor2.getColumnIndex("tls"));
                double wnlsf = cursor2.getDouble(cursor2.getColumnIndex("wnlsf"));

                double dn = cursor2.getDouble(cursor2.getColumnIndex("dn"));
                double tlsf = cursor2.getDouble(cursor2.getColumnIndex("tlsf"));
                stringBuilder.append(String.format("    %6.0f%6.0f%6.0f%6.0f                                        LEAP SECONDS", tls, tlsf, wnlsf, dn));
                stringBuilder.append('\n');
            } while (cursor2.moveToNext());
        }
        cursor2.close();
        } catch (Exception e) {
        }

        stringBuilder.append("                                                                   END OF HEADER");
        stringBuilder.append('\n');
        return stringBuilder;
    }
    public StringBuilder getNavigationMessageToRinex2Header() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("     2.11           N: GPS NAV DATA                         RINEX VERSION / TYPE");
        stringBuilder.append('\n');
        stringBuilder.append("Converto v3.5.2     BTFyoudianleiaa                          PGM / RUN BY / DATE");
        stringBuilder.append('\n');
        NavDB = sqliteManager.getWritableDatabase();

        //查询IONOSPHERIC表中所有数据
        try {
        Cursor cursor = NavDB.query("IONOSPHERIC", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                double GPSA0 = cursor.getDouble(cursor.getColumnIndex("GPSA0"));
                double GPSA1 = cursor.getDouble(cursor.getColumnIndex("GPSA1"));
                double GPSA2 = cursor.getDouble(cursor.getColumnIndex("GPSA2"));
                double GPSA3 = cursor.getDouble(cursor.getColumnIndex("GPSA3"));
                double GPSB0 = cursor.getDouble(cursor.getColumnIndex("GPSB0"));
                double GPSB1 = cursor.getDouble(cursor.getColumnIndex("GPSB1"));
                double GPSB2 = cursor.getDouble(cursor.getColumnIndex("GPSB2"));
                double GPSB3 = cursor.getDouble(cursor.getColumnIndex("GPSB3"));

                stringBuilder.append(String.format("GPSA  %12.4E %12.4E %12.4E %12.4E       IONOSPHERIC CORR", GPSA0, GPSA1, GPSA2, GPSA3));
                stringBuilder.append('\n');
                stringBuilder.append(String.format("GPSB  %12.4E %12.4E %12.4E %12.4E       IONOSPHERIC CORR", GPSB0, GPSB1, GPSB2, GPSB3));
                stringBuilder.append('\n');

            } while (cursor.moveToNext());
        }
        cursor.close();
        } catch (Exception e) {

        }
        //查询UTC表中所有数据
        try {
        Cursor cursor1 = NavDB.query("UTC", null, null, null, null, null, null);

        if (cursor1.moveToFirst()) {
            do {
                double a0UTC = cursor1.getDouble(cursor1.getColumnIndex("a0UTC"));
                double a1UTC = cursor1.getDouble(cursor1.getColumnIndex("a1UTC"));

                double tot = cursor1.getDouble(cursor1.getColumnIndex("tot"));
                double wnt = cursor1.getDouble(cursor1.getColumnIndex("wnt"));

                if (tot > 0 && wnt > 0) {
                    stringBuilder.append(String.format("GPUT %19.12E%19.12E%9.0f%9.0f   TIME SYSTEM CORR", a0UTC, a1UTC, tot, wnt));
                    stringBuilder.append('\n');
                }

            } while (cursor1.moveToNext());
        }
        cursor1.close();
        } catch (Exception e) {
        }
        try {
        Cursor cursor2 = NavDB.query("LEAPSECOND", null, null, null, null, null, null);

        if (cursor2.moveToFirst()) {
            do {
                double tls = cursor2.getDouble(cursor2.getColumnIndex("tls"));
                double wnlsf = cursor2.getDouble(cursor2.getColumnIndex("wnlsf"));

                double dn = cursor2.getDouble(cursor2.getColumnIndex("dn"));
                double tlsf = cursor2.getDouble(cursor2.getColumnIndex("tlsf"));
                stringBuilder.append(String.format("    %6.0f%6.0f%6.0f%6.0f                                        LEAP SECONDS", tls, tlsf, wnlsf, dn));
                stringBuilder.append('\n');
            } while (cursor2.moveToNext());
        }
        cursor2.close();
        } catch (Exception e) {
        }

        stringBuilder.append("                                                                   END OF HEADER");
        stringBuilder.append('\n');
        return stringBuilder;
    }


    public void handleFirstSubframe(int prn, byte[] rawData) {

        //IODC的数据龄期，在6广播轨道里面
        int iodc = extractBits(IODC1_INDEX, IODC1_LENGTH, rawData) << 8;
        iodc |= extractBits(IODC2_INDEX, IODC2_LENGTH, rawData);
        //Gps周数，在5广播轨道里
        // the navigation message contains a modulo-1023 week number
        int week = extractBits(WEEK_INDEX, WEEK_LENGTH, rawData);
        //指代卫星精度(m) 在6广播轨道
        int uraIndex = extractBits(URA_INDEX, URA_LENGTH, rawData);
        //卫星健康状态    6广播轨道
        int svHealth = extractBits(SV_HEALTH_INDEX, SV_HEALTH_LENGTH, rawData);
        //TGD 6广播轨道
        byte tgd = (byte) extractBits(TGD_INDEX, TGD_LENGTH, rawData);
        @SuppressLint("DefaultLocale") String broadcastorbit_6 = String.format("    %1.12E,%1.12E,%1.12E,%1.12E", (double) uraIndex, (double) svHealth, (double) tgd, (double) iodc);
        //Log.i("Navigation---6", broadcastorbit_6);

        //TOC 历元  卫星钟的参考时刻
        int toc = extractBits(TOC_INDEX, TOC_LENGTH, rawData);
        double tocScaled = toc * POW_2_4;
        this.mtocScaled[prn] = tocScaled;

        //卫星中的漂移速度
        byte af2 = (byte) extractBits(AF2_INDEX, AF2_LENGTH, rawData);
        //卫星钟的漂移
        short af1 = (short) extractBits(AF1_INDEX, AF1_LENGTH, rawData);
        //卫星钟的偏差
        int af0 = extractBits(AF0_INDEX, AF0_LENGTH, rawData);
        af0 = getTwoComplement(af0, AF0_LENGTH);

        @SuppressLint("DefaultLocale") String Epoch_Sv_Clk = String.format("    %1.12E,%1.12E,%1.12E,%1.12E", tocScaled, (double) af0, (double) af1, (double) af2);
        //Log.i("Navigation---Epoch_Sv_Clk", Epoch_Sv_Clk);

        //L2上的码   5广播轨道
        double L2code = 0;
        //L2 P码数据标记  5广播轨道
        double L2P = 0;

        //把数据更新到数据库中
        NavDB = sqliteManager.getWritableDatabase();

        //找这个卫星是否被记录到数据库中
        String query = "select Svid from ephgpsBook  where Svid='" + prn + "' and Toc='" + tocScaled + "'";
        Cursor c = NavDB.rawQuery(query, null);
        c.moveToFirst();
        int count = c.getCount();
        c.close();

        //表示存在
        if (count > 0) {
            ContentValues values = new ContentValues();
            values.put("Svid", prn);


            values.put("af0", af0);
            values.put("af1", af1);
            values.put("af2", af2);
            values.put("Toc", tocScaled);


            values.put("gpsweek", week);
            values.put("svAccur", uraIndex);
            values.put("svHealth", svHealth);
            values.put("iodc", iodc);
            values.put("TGD", tgd);
            values.put("L2code", L2code);
            values.put("L2Flag", L2P);
            NavDB.update("ephgpsBook", values, "Svid=? and Toc=?", new String[]{String.valueOf(prn), String.valueOf(tocScaled)});

            values.clear();
        } else {
            //表示不存在
            ContentValues values = new ContentValues();
            values.put("Svid", prn);

            values.put("af0", af0);
            values.put("af1", af1);
            values.put("af2", af2);

            values.put("Toc", tocScaled);

            values.put("gpsweek", week);
            values.put("svAccur", uraIndex);
            values.put("svHealth", svHealth);
            values.put("iodc", iodc);
            values.put("TGD", tgd);
            values.put("L2code", L2code);
            values.put("L2Flag", L2P);

            NavDB.insert("ephgpsBook", null, values);
            values.clear();
        }

    }

    public void handleSecondSubframe(int prn, byte[] rawData) {

        //IODE数据、星历发布时间  1广播轨道
        double iode = extractBits(IODE1_INDEX, IODE_LENGTH, rawData);

        // 1广播轨道
        double crs = (short) extractBits(CRS_INDEX, CRS_LENGTH, rawData) * POW_2_NEG_5;

        //1广播轨道
        double deltaN = (short) extractBits(DELTA_N_INDEX, DELTA_N_LENGTH, rawData) * POW_2_NEG_43 * Math.PI;
        //1广播轨道
        double m0 = (int) buildUnsigned32BitsWordFrom8And24Words(M0_INDEX8, M0_INDEX24, rawData) * POW_2_NEG_31 * Math.PI;

        @SuppressLint("DefaultLocale") String broadcastorbit_1 = String.format("    %1.12E,%1.12E,%1.12E,%1.12E", iode, crs, deltaN, m0);
        //Log.i("Navigation--1", "G" + prn + "," + broadcastorbit_1);

        //2广播轨道
        double cuc = (short) extractBits(CUC_INDEX, CUC_LENGTH, rawData) * POW_2_NEG_29;

        //轨道偏心  2广播轨道
        // an unsigned 32 bit value
        double e = buildUnsigned32BitsWordFrom8And24Words(E_INDEX8, E_INDEX24, rawData) * POW_2_NEG_33;

        //2广播轨道
        double cus = (short) extractBits(CUS_INDEX, CUS_LENGTH, rawData) * POW_2_NEG_29;

        // an unsigned 32 bit value
        //2广播轨道
        double a = buildUnsigned32BitsWordFrom8And24Words(A_INDEX8, A_INDEX24, rawData) * POW_2_NEG_19;

        @SuppressLint("DefaultLocale") String broadcastorbit_2 = String.format("    %1.12E,%1.12E,%1.12E,%1.12E", cuc, e, cus, a);
        //Log.i("Navigation---2", "G" + prn + "," + broadcastorbit_2);

        //GPS周内的秒数  3广播轨道
        double toe = extractBits(TOE_INDEX, TOE_LENGTH, rawData);
        double toeScaled = toe * POW_2_4;


        //Log.i("Navigation---3", "G" + prn + "," + toeScaled);

        //打开数据库
        //把数据更新到数据库中
        NavDB = sqliteManager.getWritableDatabase();

        //判断这个svid和  tocScaled 已经存在，则更新数据库
        double nowTocScaled = this.mtocScaled[prn];
        String query = "select Svid from ephgpsBook  where Svid='" + prn + "' and Toc='" + nowTocScaled + "'";
        Cursor c = NavDB.rawQuery(query, null);
        c.moveToFirst();
        int count = c.getCount();
        c.close();

        if (count > 0) {
            //表示存在
            ContentValues values = new ContentValues();
            values.put("Svid", prn);

            values.put("iode", iode);
            values.put("Crs", crs);
            values.put("delta_n", deltaN);
            values.put("M0", m0);

            values.put("Cuc", cuc);
            values.put("Cus", cus);
            values.put("es", e);
            values.put("sqrtA", a);
            values.put("Toe", toeScaled);
            NavDB.update("ephgpsBook", values, "Svid=? and Toc=?", new String[]{String.valueOf(prn), String.valueOf(nowTocScaled)});

            values.clear();
        }


    }


    public void handleThirdSubframe(int prn, byte[] rawData) {

        //3广播轨道
        double cic = (short) extractBits(CIC_INDEX, CIC_LENGTH, rawData) * POW_2_NEG_29;

        //w  4广播轨道
        double w = (int) buildUnsigned32BitsWordFrom8And24Words(O0_INDEX8, O0_INDEX24, rawData) * POW_2_NEG_31 * Math.PI;

        //omega  3广播轨道
        double omega = (int) buildUnsigned32BitsWordFrom8And24Words(O_INDEX8, O_INDEX24, rawData) * POW_2_NEG_31 * Math.PI;

        //Omegadot  4广播轨道
        int odot = extractBits(ODOT_INDEX, ODOT_LENGTH, rawData);
        double Omegadot = getTwoComplement(odot, ODOT_LENGTH) * POW_2_NEG_43 * Math.PI;

        //3广播轨道
        double cis = (short) extractBits(CIS_INDEX, CIS_LENGTH, rawData) * POW_2_NEG_29;

        @SuppressLint("DefaultLocale") String broadcastorbit_3 = String.format("    %1.12E,%1.12E,%1.12E", cic, omega, cis);
        //Log.i("Navigation---3", "G" + prn + "," + broadcastorbit_3);
        //4广播轨道
        double i0 = (int) buildUnsigned32BitsWordFrom8And24Words(I0_INDEX8, I0_INDEX24, rawData) * POW_2_NEG_31 * Math.PI;

        //4广播轨道
        double crc = (short) extractBits(CRC_INDEX, CRC_LENGTH, rawData) * POW_2_NEG_5;
        //5广播轨道
        // a 14-bit two's complement number
        int idot = extractBits(IDOT_INDEX, IDOT_LENGTH, rawData);
        double Idot = getTwoComplement(idot, IDOT_LENGTH) * POW_2_NEG_43 * Math.PI;

        @SuppressLint("DefaultLocale") String broadcastorbit_5 = String.format("    %1.12E", Idot);

        @SuppressLint("DefaultLocale") String broadcastorbit_4 = String.format("    %1.12E,%1.12E,%1.12E,%1.12E", i0, crc, w, Omegadot);
        //Log.i("Navigation----4", "G" + prn + "," + broadcastorbit_4);
        //Log.i("Navigation----5", "G" + prn + "," + broadcastorbit_5);

        //打开数据库
        //把数据更新到数据库中
        NavDB = sqliteManager.getWritableDatabase();

        //判断这个svid和  tocScaled 已经存在，则更新数据库
        double nowTocScaled = this.mtocScaled[prn];
        String query = "select Svid from ephgpsBook  where Svid='" + prn + "' and Toc='" + nowTocScaled + "'";

        Cursor c = NavDB.rawQuery(query, null);
        c.moveToFirst();
        int count = c.getCount();
        c.close();

        if (count > 0) {
            //表示存在
            ContentValues values = new ContentValues();
            values.put("Svid", prn);

            //values.put("iode", iode);
            values.put("Cic", cic);
            values.put("Omega_0", omega);
            values.put("Cis", cis);

            values.put("i0", i0);
            values.put("Crc", crc);
            values.put("w", w);
            values.put("i_dot", Idot);
            values.put("Omega_dot", Omegadot);
            NavDB.update("ephgpsBook", values, "Svid=? and Toc=?", new String[]{String.valueOf(prn), String.valueOf(nowTocScaled)});
            values.clear();
        }
    }

    private static int buildSigned32BitsWordFrom8And24WordsWith8bitslsb(
            int index8, int index24, byte[] rawData) {
        int result = extractBits(index24, 24, rawData) << 8;
        result |= extractBits(index8, 8, rawData);
        return result;
    }

    private void handleFourthSubframe(int page, byte[] rawData) {
        /*byte pageId = (byte) extractBits(62, 6, rawData);*/
        if (page != 18) {
            // We only care to decode ionospheric parameters for now
            Log.i("Navigation", "PAGE No." + page + " Not found IONOSPHERIC DATA");
        }

        StringBuilder FourthSubframe = new StringBuilder();

        NavDB = sqliteManager.getWritableDatabase();

        double[] alpha = new double[4];
        byte a0 = (byte) extractBits(A0_INDEX, A_B_LENGTH, rawData);

        alpha[0] = a0 * POW_2_NEG_30;
        byte a1 = (byte) extractBits(A1_INDEX, A_B_LENGTH, rawData);
        alpha[1] = a1 * POW_2_NEG_27;
        byte a2 = (byte) extractBits(A2_INDEX, A_B_LENGTH, rawData);
        alpha[2] = a2 * POW_2_NEG_24;
        byte a3 = (byte) extractBits(A3_INDEX, A_B_LENGTH, rawData);
        alpha[3] = a3 * POW_2_NEG_24;
        FourthSubframe.append(String.format("GPSA   %1.4E %1.4E %1.4E %1.4E       IONOSPHERIC CORR\n", alpha[0], alpha[1], alpha[2], alpha[3]));


        double[] beta = new double[4];
        byte b0 = (byte) extractBits(B0_INDEX, A_B_LENGTH, rawData);
        beta[0] = b0 * POW_2_11;
        byte b1 = (byte) extractBits(B1_INDEX, A_B_LENGTH, rawData);
        beta[1] = b1 * POW_2_14;
        byte b2 = (byte) extractBits(B2_INDEX, A_B_LENGTH, rawData);
        beta[2] = b2 * POW_2_16;
        byte b3 = (byte) extractBits(B3_INDEX, A_B_LENGTH, rawData);
        beta[3] = b3 * POW_2_16;
        FourthSubframe.append(String.format("GPSB   %1.4E %1.4E %1.4E %1.4E       IONOSPHERIC CORR\n", beta[0], beta[1], beta[2], beta[3]));

        double a0UTC =
                buildSigned32BitsWordFrom8And24WordsWith8bitslsb(I0UTC_INDEX8, I0UTC_INDEX24, rawData)
                        * Math.pow(2, -30);
        double a1UTC = getTwoComplement(extractBits(I1UTC_INDEX, 24, rawData), 24) * Math.pow(2, -50);
        short tot = (short) (extractBits(TOT_LS_INDEX, A_B_LENGTH, rawData) * POW_2_12);
        short wnt = (short) extractBits(WN_LS_INDEX, A_B_LENGTH, rawData);

        short tls = (short) extractBits(DELTA_T_LS_INDEX, A_B_LENGTH, rawData);
        short wnlsf = (short) extractBits(WNF_LS_INDEX, A_B_LENGTH, rawData);
        short dn = (short) extractBits(DN_LS_INDEX, A_B_LENGTH, rawData);
        short tlsf = (short) extractBits(DELTA_TF_LS_INDEX, A_B_LENGTH, rawData);
        //tot > 0 && a1 > 0 && tls > 0
        // if (a1 > 0) {
        if (!sqliteManager.existTable(NavDB, "IONOSPHERIC")) {
            sqliteManager.createTable(NavDB, "IONOSPHERIC");
        }

        if (!sqliteManager.existColumn(NavDB, "IONOSPHERIC", "GPSA0")) {
            ContentValues values = new ContentValues();
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSA0'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSA1'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSA2'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSA3'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSB0'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSB1'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSB2'");
            NavDB.execSQL("ALTER TABLE 'IONOSPHERIC' ADD COLUMN 'GPSB3'");
            values.put("GPSA0", alpha[0]);
            values.put("GPSA1", alpha[1]);
            values.put("GPSA2", alpha[2]);
            values.put("GPSA3", alpha[3]);
            values.put("GPSB0", beta[0]);
            values.put("GPSB1", beta[1]);
            values.put("GPSB2", beta[2]);
            values.put("GPSB3", beta[3]);
            NavDB.insert("IONOSPHERIC", null, values);
            values.clear();
        } else {
            ContentValues values = new ContentValues();
            values.put("GPSA0", alpha[0]);
            values.put("GPSA1", alpha[1]);
            values.put("GPSA2", alpha[2]);
            values.put("GPSA3", alpha[3]);
            values.put("GPSB0", beta[0]);
            values.put("GPSB1", beta[1]);
            values.put("GPSB2", beta[2]);
            values.put("GPSB3", beta[3]);
            NavDB.update("IONOSPHERIC", values, null, null);
            values.clear();
        }
        //NavDB.close();
        // }
        //if (tot > 0 && tls > 0) {

        if (!sqliteManager.existTable(NavDB, "UTC")) {
            sqliteManager.createTable(NavDB, "UTC");
        }

        FourthSubframe.append(String.format("GPUT %1.10E%1.10E %6d %6d         TIME SYSTEM CORR\n", a0UTC, a1UTC, tot, wnt));
        if (!sqliteManager.existColumn(NavDB, "UTC", "a0UTC")) {
            ContentValues values = new ContentValues();
            NavDB.execSQL("ALTER TABLE 'UTC' ADD COLUMN 'a0UTC'");
            NavDB.execSQL("ALTER TABLE 'UTC' ADD COLUMN 'a1UTC'");
            NavDB.execSQL("ALTER TABLE 'UTC' ADD COLUMN 'tot'");
            NavDB.execSQL("ALTER TABLE 'UTC' ADD COLUMN 'wnt'");
            values.put("a0UTC", a0UTC);
            values.put("a1UTC", a1UTC);
            values.put("tot", tot);
            values.put("wnt", wnt);
            NavDB.insert("UTC", null, values);
            values.clear();
        } else {
            if (tot > 0 && wnt > 0) {
                ContentValues values = new ContentValues();
                values.put("a0UTC", a0UTC);
                values.put("a1UTC", a1UTC);
                values.put("tot", tot);
                values.put("wnt", wnt);
                NavDB.update("UTC", values, null, null);
                values.clear();
            }
        }
        //}

        if (!sqliteManager.existTable(NavDB, "LEAPSECOND")) {
            sqliteManager.createTable(NavDB, "LEAPSECOND");
        }

        FourthSubframe.append(String.format("%6d%6d%6d%6d                                   LEAP SECONDS\n", tls, tlsf, wnlsf, dn));
        if (!sqliteManager.existColumn(NavDB, "LEAPSECOND", "tls")) {
            ContentValues values = new ContentValues();
            NavDB.execSQL("ALTER TABLE 'LEAPSECOND' ADD COLUMN 'tls'");
            NavDB.execSQL("ALTER TABLE 'LEAPSECOND' ADD COLUMN 'wnlsf'");
            NavDB.execSQL("ALTER TABLE 'LEAPSECOND' ADD COLUMN 'dn'");
            NavDB.execSQL("ALTER TABLE 'LEAPSECOND' ADD COLUMN 'tlsf'");
            values.put("tls", tls);
            values.put("wnlsf", wnlsf);
            values.put("dn", dn);
            values.put("tlsf", tlsf);
            NavDB.insert("LEAPSECOND", null, values);
            values.clear();
        } else {

            ContentValues values = new ContentValues();
            values.put("tls", tls);
            values.put("wnlsf", wnlsf);
            values.put("dn", dn);
            values.put("tlsf", tlsf);
            NavDB.update("LEAPSECOND", values, null, null);
            values.clear();
        }
        Log.i("Navigation", FourthSubframe.toString());

    }


    private static int getTwoComplement(int value, int bits) {
        int msbMask = 1 << bits - 1;
        int msb = value & msbMask;
        if (msb == 0) {
            // the value is positive
            return value;
        }

        int valueBitMask = (1 << bits) - 1;
        int extendedSignMask = (int) INTEGER_RANGE - valueBitMask;
        return value | extendedSignMask;
    }

    private static int extractBits(int index, int length, byte[] rawData) {
        int result = 0;

        for (int i = 0; i < length; ++i) {
            int workingIndex = index + i;

            int wordIndex = workingIndex / WORD_SIZE_BITS;
            // account for 2 bit padding for every 30bit word
            workingIndex += (wordIndex + 1) * WORD_PADDING_BITS;
            int byteIndex = workingIndex / BYTE_AS_BITS;
            int byteOffset = workingIndex % BYTE_AS_BITS;

            byte raw = rawData[byteIndex];
            // account for zero-based indexing
            int shiftOffset = BYTE_AS_BITS - 1 - byteOffset;
            int mask = 1 << shiftOffset;
            int bit = raw & mask;
            bit >>= shiftOffset;

            // account for zero-based indexing
            result |= bit << length - 1 - i;
        }
        return result;
    }

    private static long buildUnsigned32BitsWordFrom8And24Words(int index8, int index24,
                                                               byte[] rawData) {
        long result = (long) extractBits(index8, 8, rawData) << 24;
        result |= extractBits(index24, 24, rawData);
        return result;
    }

    private static String getNAVType(int type) {
        switch (type) {
            case GnssNavigationMessage.TYPE_GPS_L1CA:
                return "GPS_L1CA";
            case GnssNavigationMessage.TYPE_GLO_L1CA:
                return "GLO_L1CA";
            case GnssNavigationMessage.TYPE_GPS_L5CNAV:
                return "GPS_L5CNAV";
            default:
                return "UNKNOWN";
        }
    }


}
