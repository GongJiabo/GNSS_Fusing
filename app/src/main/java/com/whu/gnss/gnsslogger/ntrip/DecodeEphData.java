package com.whu.gnss.gnsslogger.ntrip;

import android.util.Log;

import com.whu.gnss.gnsslogger.ConstantSystem;
import com.whu.gnss.gnsslogger.constellations.Time;


public class DecodeEphData {

    private static final String TAG = "decodeEphData";

    public static  long getbitu(byte[] buff, int pos, int len) {
        long bits = 0L;
        int i;
        for (i = pos; i < pos + len; i++)
            bits = (bits << 1) + ((buff[i / 8] >> (7 - i % 8)) & 1L);
        return bits;
    }

    private static double getbitg(byte[] buff, int pos, int len) {
        double value = getbitu(buff, pos + 1, len - 1);
        return getbitu(buff, pos, 1) != 0 ? -value : value;
    }


    private static int getbits(byte[] buff, int pos, int len) {
        long  bits = getbitu(buff, pos, len);
        if (len <= 0 || 32 <= len || (bits & (1L << (len - 1))) == 0) return (int) bits;
        return (int) (bits | (~0L << len));
    }

    private static int adjustGpsWeek(int week) {
        //目前应该是这样的
        return week = 1024 * 2 + week;

        //所以这个只能用10年吧，后续还要改
    }

    private static int satExist(char system, int satId) {
        switch (system) {
            case ConstantSystem.GPS_SYSTEM:
                if (satId >= ConstantSystem.MINSATIDGPS && satId <= ConstantSystem.MAXSATIDGPS)
                    return 1;
                else return 0;
            case ConstantSystem.QZSS_SYSTEM:
                if (satId >= ConstantSystem.MINSATIDQZSS && satId <= ConstantSystem.MAXSATIDQZSS)
                    return 1;
                else return 0;
            case ConstantSystem.GALILEO_SYSTEM:
                if (satId >= ConstantSystem.MINSATIDGAL && satId <= ConstantSystem.MAXSATIDGAL)
                    return 1;
                else return 0;
            case ConstantSystem.GLONASS_SYSTEM:
                if (satId >= ConstantSystem.MINSATIDGLO && satId <= ConstantSystem.MAXSATIDGLO)
                    return 1;
                else return 0;
            case ConstantSystem.BEIDOU_SYSTEM:
                if (satId >= ConstantSystem.MINSATIDBDS && satId <= ConstantSystem.MAXSATIDBDS)
                    return 1;
                else return 0;
        }
        return 0;
    }

    private static int bdtWeek2GpsWeek(int beidouWeek) {
        return beidouWeek + 1356;
    }

    /*
    北斗时=GPS时-14s
     */
    private static double bdtWeekSec2GpsWeekSec(double bdtWeekSec) {
        return bdtWeekSec - 14;
    }
    /*
    glonass 时间=  utc时间+3小时
     */

    //解码gps广播星历---1019
    public static EphGps decodeGpsEph(byte[] data) {

        int type = (int)getbitu(data, 0, 12);
        Log.d("GPSTYPE", "-"+type);
        if(type!=1019)
            return null;

        EphGps ephGps = new EphGps();

        int week;
        double toe, toc;
        int i = 12;
        ephGps.setSatType(ConstantSystem.GPS_SYSTEM);
        if (i + 476 <= data.length * 8) {
            ephGps.setSatID((int)getbitu(data, i, 6));
            i = i + 6;
            //System.out.println("G"+ephGps.getSatID());
            //ephGps.setWeek(getbitu(data, i, 10));
            week = (int)getbitu(data, i, 10);
            i = i + 10;
            //System.out.println(week);

            ephGps.setSvAccur((int)getbitu(data, i, 4));
            i = i + 4;
            //System.out.println(ephGps.getSvAccur());

            ephGps.setL2Code((int)getbitu(data, i, 2));
            i = i + 2;
            //System.out.println(ephGps.getL2Code());

            ephGps.setiDot(getbits(data, i, 14) * Math.pow(2, -43) * Math.PI);
            i = i + 14;
            //System.out.println(ephGps.getiDot());

            ephGps.setIode((int)getbitu(data, i, 8));
            i = i + 8;
            //System.out.println("iode" + ephGps.getIode());

            //ephGps.setToc(getbitu(data, i, 16) * 16.0);
            toc = getbitu(data, i, 16) * 16.0;
            i = i + 16;
            //System.out.println(ephGps.getToc());

            //2^-55
            ephGps.setAf2(getbits(data, i, 8) * Math.pow(2, -55));
            i = i + 8;
            //System.out.println(ephGps.getAf2());
            //2^-43
            ephGps.setAf1(getbits(data, i, 16) * Math.pow(2, -43));
            i = i + 16;
            //System.out.println("AF1" + ephGps.getAf1());
            //2^-31
            ephGps.setAf0(getbits(data, i, 22) * Math.pow(2, -31));
            i = i + 22;
            //System.out.println("AF0" + ephGps.getAf0());

            ephGps.setIodc((int)getbitu(data, i, 10));
            i = i + 10;
            //System.out.println("iodc" + ephGps.getIodc());
            //2^-5
            ephGps.setCrs(getbits(data, i, 16) * 0.03125);
            i = i + 16;

            ephGps.setDeltaN(getbits(data, i, 16) * Math.pow(2, -43) * Math.PI);
            i = i + 16;

            ephGps.setM0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;

            ephGps.setCuc(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;

            ephGps.setE(getbitu(data, i, 32) * Math.pow(2, -33));
            i = i + 32;

            ephGps.setCus(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;

            ephGps.setRootA(getbitu(data, i, 32) * Math.pow(2, -19));
            i = i + 32;

            //ephGps.setToe(getbitu(data, i, 16) * 16.0);
            toe = getbitu(data, i, 16) * 16.0;
            i = i + 16;
            //System.out.println("toe" + ephGps.getToe());


            ephGps.setCic(getbits(data, i, 16) * Math.pow(2, -29));

            i = i + 16;

            ephGps.setOmega0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;

            ephGps.setCis(getbits(data, i, 16) * Math.pow(2, -29));

            i = i + 16;

            ephGps.setI0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);

            i = i + 32;

            ephGps.setCrc(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;

            ephGps.setOmg(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);

            i = i + 32;

            ephGps.setOmegaDot(getbits(data, i, 24) * Math.pow(2, -43) * Math.PI);

            i = i + 24;

            ephGps.setTgd(getbits(data, i, 8) * Math.pow(2, -31));

            i = i + 8;

            ephGps.setSvHealth((int)getbitu(data, i, 6));
            i = i + 6;

            ephGps.setL2Flag((int)getbitu(data, i, 1));
            i = i + 1;
            //拟合区间如果不等于0，就把它定为2个小时；如果等于0，就说明没有给出
            ephGps.setFitInt((getbitu(data, i, 1) != 0) ? 2 : 0);
            //System.out.println(ephGps.getFitInt());

        } else {
            Log.d(TAG + "GPS-decode", "接收到的数据长度错误");
            return null;

        }
        if (satExist(ConstantSystem.GPS_SYSTEM, ephGps.getSatID()) == 0) {
            Log.d(TAG, "G" + ephGps.getSatID() + "不存在");
            return null;
        }
        //对gps周进行调整
        week = adjustGpsWeek(week);
        ephGps.setWeek(week);

        //System.out.println(ephGps.getWeek());

        //toe表示toe星历的参考时刻
        ephGps.setToe(toe);
        //System.out.println("toe" + toe);

        //toc表示卫星钟的参考时刻，和toe应该是一样的。
        ephGps.setToc(toc);
        //System.out.println("toc" + toc);
        //reftime  这个历元的时间
        ephGps.setRefTime(new Time(week, toc));

        //System.out.println("历元时刻" + ephGps.getRefTime().toString());
       // Log.d(TAG, "G" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString());
        Log.d(TAG, "G" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString()+"sqrtA"+ephGps.getRootA());

        return ephGps;

    }


    /*
     prn        =getbitu(rtcm->buff,i, 6);           i+= 6;
        geph.frq   =getbitu(rtcm->buff,i, 5)-7;         i+= 5+2+2;
        tk_h       =getbitu(rtcm->buff,i, 5);           i+= 5;
        tk_m       =getbitu(rtcm->buff,i, 6);           i+= 6;
        tk_s       =getbitu(rtcm->buff,i, 1)*30.0;      i+= 1;
        bn         =getbitu(rtcm->buff,i, 1);           i+= 1+1;
        tb         =getbitu(rtcm->buff,i, 7);           i+= 7;
        geph.vel[0]=getbitg(rtcm->buff,i,24)*P2_20*1E3; i+=24;
        geph.pos[0]=getbitg(rtcm->buff,i,27)*P2_11*1E3; i+=27;
        geph.acc[0]=getbitg(rtcm->buff,i, 5)*P2_30*1E3; i+= 5;
        geph.vel[1]=getbitg(rtcm->buff,i,24)*P2_20*1E3; i+=24;
        geph.pos[1]=getbitg(rtcm->buff,i,27)*P2_11*1E3; i+=27;
        geph.acc[1]=getbitg(rtcm->buff,i, 5)*P2_30*1E3; i+= 5;
        geph.vel[2]=getbitg(rtcm->buff,i,24)*P2_20*1E3; i+=24;
        geph.pos[2]=getbitg(rtcm->buff,i,27)*P2_11*1E3; i+=27;
        geph.acc[2]=getbitg(rtcm->buff,i, 5)*P2_30*1E3; i+= 5+1;
        geph.gamn  =getbitg(rtcm->buff,i,11)*P2_40;     i+=11+3;
        geph.taun  =getbitg(rtcm->buff,i,22)*P2_30;
     */
    public static EphGps decodeGlonassEph(byte[] data) {


        int type = (int)getbitu(data, 0, 12);
        Log.d("GPSTYPE", "点位类型："+type);
        double tk_h, tk_m, tk_s, toe, tow, tod, tof;

        EphGps ephGlonass = new EphGps();

        int i =  12;
        int tb, bn, week;
        ephGlonass.setSatType(ConstantSystem.GLONASS_SYSTEM);

        if (i + 348 <= data.length * 8) {
            //id
            ephGlonass.setSatID((int)getbitu(data, i, 6));
            i = i + 6;
            //System.out.println("R"+ephGlonass.getSatID());
            //频率
            ephGlonass.setfreq_num((int)getbitu(data, i, 5) - 7);
            i += 5 + 2 + 2;
            //tk_h
            tk_h = getbitu(data, i, 5);
            //System.out.println(tk_h);
            i += 5;
            //tk_m
            tk_m = getbitu(data, i, 6);
            //System.out.println(tk_m);
            i += 6;
            //tk_s
            tk_s = getbitu(data, i, 1) * 30.0;
            //System.out.println("s"+tk_s);
            i += 1;
            //bn
            bn = (int)getbitu(data, i, 1);
            i += 1 + 1;
            //tb
            tb = (int)getbitu(data, i, 7);
            i += 7;
            //x_speed
            ephGlonass.setXv(getbitg(data, i, 24) * Math.pow(2, -20) * 1.0E3);
            i += 24;
            //x_pos
            ephGlonass.setX(getbitg(data, i, 27) * Math.pow(2, -11) * 1.0E3);
            i += 24;
            //x_acc
            ephGlonass.setXa(getbitg(data, i, 5) * Math.pow(2, -30) * 1.0E3);
            i += 5;
            //y_speed
            ephGlonass.setYv(getbitg(data, i, 24) * Math.pow(2, -20) * 1.0E3);
            i += 24;
            //y_pos
            ephGlonass.setY(getbitg(data, i, 27) * Math.pow(2, -11) * 1.0E3);
            i += 24;
            //y_acc
            ephGlonass.setYa(getbitg(data, i, 5) * Math.pow(2, -30) * 1.0E3);
            i += 5;
            //z_speed
            ephGlonass.setZv(getbitg(data, i, 24) * Math.pow(2, -20) * 1.0E3);
            i += 24;
            //z_pos
            ephGlonass.setZ(getbitg(data, i, 27) * Math.pow(2, -11) * 1.0E3);
            i += 24;
            //z_acc
            ephGlonass.setZa(getbitg(data, i, 5) * Math.pow(2, -30) * 1.0E3);
            i += 5 + 1;

            ephGlonass.setGammaN(getbitg(data, i, 11) * Math.pow(2, -40));
            i += 11 + 3;

            ephGlonass.setTauN(getbitg(data, i, 22) * Math.pow(2, -30));

        } else {
            Log.d(TAG + "GLO-decode", "接收到的数据长度错误");
            return null;
        }
        if (satExist(ConstantSystem.GLONASS_SYSTEM, ephGlonass.getSatID()) == 0) {
            Log.d(TAG, "R" + ephGlonass.getSatID() + "不存在");
            return null;
        }


        ephGlonass.setSvHealth(bn);

        ephGlonass.setIode(tb & 0x7F);

        //当前系统时间戳  ms
        long msec = System.currentTimeMillis();

        Time time = new Time(msec);
        //gps周
        week = time.getGpsWeek();

        //gps周内秒
        tow = time.getGpsWeekSec();

        //86400是一天的秒数
        tod = tow % 86400.0;

        tow = tow - tod;
        //System.out.println("week"+week+",tow"+tow+",tod"+tod);

        //-10800  就是减去3个小时  转成utc   这个只是天内秒，所以不用减去  跳秒总数转为gps时间，，
        tof = tk_h * 3600.0 + tk_m * 60.0 + tk_s - 10800.0; /* lt->utc */
        if (tof < tod - 43200.0) tof += 86400.0;
        else if (tof > tod + 43200.0) tof -= 86400.0;

        Time toftime = new Time(week, tow + tof);

        ephGlonass.setRefTime(toftime);
        toe = tb * 900.0 - 10800.0; /* lt->utc */
//        System.out.println("tb" + tb);
//        System.out.println("toe" + toe);
        if (toe < tod - 43200.0) toe += 86400.0;
        else if (toe > tod + 43200.0) toe -= 86400.0;

        Time toetime = new Time(week, tow + toe);

        ephGlonass.setToe(toetime.getGpsWeekSec());
        //System.out.println("toftime" + toftime.toString());
        //System.out.println("toetime" + toetime.toString());
        Log.d(TAG, "R" + ephGlonass.getSatID() + "---" + ephGlonass.getRefTime().toString());

        return ephGlonass;

    }

    //decode type 1045: galileo F/NAV satellite ephemerides (ref [15])
    public static EphGps decodeGalileoEph(byte[] data) {


        EphGps ephGps = new EphGps();
        int week = 0;
        double toe, toc;
        int i =  12;
        int e5a_hs, e5a_dvs, rsv;
        ephGps.setSatType(ConstantSystem.GALILEO_SYSTEM);
        if (i + 484 <= data.length * 8) {
            ephGps.setSatID((int)getbitu(data, i, 6));
            i = i + 6;
            //System.out.println("E"+ephGps.getSatID());
            //week
            week = (int)getbitu(data, i, 12);
            i = i + 12;
            //System.out.println(week);
            //iode
            ephGps.setIode((int)getbitu(data, i, 10));
            i = i + 10;
            //System.out.println("iode" + ephGps.getIode());
            //sva
            ephGps.setSvAccur((int)getbitu(data, i, 8));
            i = i + 8;
            //System.out.println(ephGps.getSvAccur());
            //idot
            ephGps.setiDot(getbits(data, i, 14) * Math.pow(2, -43) * Math.PI);
            i = i + 14;
            //System.out.println(ephGps.getiDot());
            //toc
            toc = getbitu(data, i, 14) * 60.0;
            i = i + 14;
            //af2,af1,af0

            ephGps.setAf2(getbits(data, i, 6) * Math.pow(2, -59));
            i = i + 6;
            //System.out.println(ephGps.getAf2());

            ephGps.setAf1(getbits(data, i, 21) * Math.pow(2, -46));
            i = i + 21;
            //System.out.println("AF1" + ephGps.getAf1());

            ephGps.setAf0(getbits(data, i, 31) * Math.pow(2, -34));
            i = i + 31;
            //System.out.println("AF0" + ephGps.getAf0());
            //crs
            ephGps.setCrs(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;
            //deltaN
            ephGps.setDeltaN(getbits(data, i, 16) * Math.pow(2, -43) * Math.PI);
            i = i + 16;
            //M0
            ephGps.setM0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //cuc
            ephGps.setCuc(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //E
            ephGps.setE(getbitu(data, i, 32) * Math.pow(2, -33));
            i = i + 32;
            //cus
            ephGps.setCus(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //根号a
            ephGps.setRootA(getbitu(data, i, 32) * Math.pow(2, -19));
            i = i + 32;
            //toe
            toe = getbitu(data, i, 14) * 60.0;
            i = i + 14;
            //cic
            ephGps.setCic(getbits(data, i, 16) * Math.pow(2, -29));

            i = i + 16;
            //Omega0
            ephGps.setOmega0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //cis
            ephGps.setCis(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //i0
            ephGps.setI0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //crc
            ephGps.setCrc(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;
            //omg  -w
            ephGps.setOmg(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //OmegaDOT
            ephGps.setOmegaDot(getbits(data, i, 24) * Math.pow(2, -43) * Math.PI);
            i = i + 24;
            //tgd
            ephGps.setTgd(getbits(data, i, 10) * Math.pow(2, -32));
            i = i + 10;
            //e5a_hs
            e5a_hs = (int)getbitu(data, i, 2);
            i = i + 2;
            //e5a_dvs
            e5a_dvs = (int)getbitu(data, i, 1);
            i = i + 1;
            //rsv
            rsv = (int)getbitu(data, i, 7);


        } else {
            Log.d(TAG + "GAL-decode", "接收到的数据长度错误");
            return null;
        }
        if (satExist(ConstantSystem.GALILEO_SYSTEM, ephGps.getSatID()) == 0) {
            Log.d(TAG, "E" + ephGps.getSatID() + "不存在");
            return null;
        }

        //对gps周进行调整
        week = 1024 + week; /* gal-week = gst-week + 1024 */
        ephGps.setWeek(week);

        //System.out.println(ephGps.getWeek());

        //toe表示toe星历的参考时刻
        ephGps.setToe(toe);
        //System.out.println("toe" + toe);

        //toc表示卫星钟的参考时刻，和toe应该是一样的。
        ephGps.setToc(toc);
        //System.out.println("toc" + toc);
        //reftime  这个历元的时间
        ephGps.setRefTime(new Time(week, toc));


        //System.out.println("galileo历元时刻" + ephGps.getRefTime().toString());
        ephGps.setSvHealth(e5a_hs << 4 + (e5a_dvs << 3));
        ephGps.setL2Code(1);/* data source = F/NAV */
        Log.d(TAG, "E" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString()+"sqrtA"+ephGps.getRootA());
        return ephGps;


    }

    public static EphGps decodeGalileo1046Eph(byte[] data) {
        EphGps ephGps = new EphGps();
        int week;
        double toe, toc;
        int i = 12;
        int e5a_hs, e5a_dvs, e5b_hs, e5b_dvs, e1_hs, e1_dvs;
        ephGps.setSatType(ConstantSystem.GALILEO_SYSTEM);
        if (i + 492 <= data.length * 8) {
            ephGps.setSatID((int)getbitu(data, i, 6));
            i = i + 6;

            //week
            week = (int)getbitu(data, i, 12);
            i = i + 12;
            //System.out.println(week);
            //iode
            ephGps.setIode((int)getbitu(data, i, 10));
            i = i + 10;
            //System.out.println("iode" + ephGps.getIode());
            //sva
            ephGps.setSvAccur((int)getbitu(data, i, 8));
            i = i + 8;
            //System.out.println(ephGps.getSvAccur());
            //idot
            ephGps.setiDot(getbits(data, i, 14) * Math.pow(2, -43) * Math.PI);
            i = i + 14;
            //System.out.println(ephGps.getiDot());
            //toc
            toc = getbitu(data, i, 14) * 60.0;
            i = i + 14;
            //af2,af1,af0

            ephGps.setAf2(getbits(data, i, 6) * Math.pow(2, -59));
            i = i + 6;
            //System.out.println(ephGps.getAf2());

            ephGps.setAf1(getbits(data, i, 21) * Math.pow(2, -46));
            i = i + 21;
            //System.out.println("AF1" + ephGps.getAf1());

            ephGps.setAf0(getbits(data, i, 31) * Math.pow(2, -34));
            i = i + 31;
            //System.out.println("AF0" + ephGps.getAf0());
            //crs
            ephGps.setCrs(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;
            //deltaN
            ephGps.setDeltaN(getbits(data, i, 16) * Math.pow(2, -43) * Math.PI);
            i = i + 16;
            //M0
            ephGps.setM0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //cuc
            ephGps.setCuc(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //E
            ephGps.setE(getbitu(data, i, 32) * Math.pow(2, -33));
            i = i + 32;
            //cus
            ephGps.setCus(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //根号a
            ephGps.setRootA(getbitu(data, i, 32) * Math.pow(2, -19));
            i = i + 32;
            //toe
            toe = getbitu(data, i, 14) * 60.0;
            i = i + 14;
            //cic
            ephGps.setCic(getbits(data, i, 16) * Math.pow(2, -29));

            i = i + 16;
            //Omega0
            ephGps.setOmega0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //cis
            ephGps.setCis(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //i0
            ephGps.setI0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //crc
            ephGps.setCrc(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;
            //omg  -w
            ephGps.setOmg(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //OmegaDOT
            ephGps.setOmegaDot(getbits(data, i, 24) * Math.pow(2, -43) * Math.PI);
            i = i + 24;
            //tgd  E5a/E1
            ephGps.setTgdE5a_E1(getbits(data, i, 10) * Math.pow(2, -32));
            i = i + 10;
            //tgd  E5b/E1
            ephGps.setTgdE5b_E1(getbits(data, i, 10) * Math.pow(2, -32));
            i = i + 10;
            //e5b_hs
            e5b_hs = (int)getbitu(data, i, 2);
            i = i + 2;
            //e5b_dvs
            e5b_dvs = (int)getbitu(data, i, 1);
            i = i + 1;
            //e1_hs
            e1_hs = (int)getbitu(data, i, 2);
            i = i + 2;
            e1_dvs = (int)getbitu(data, i, 1);
            i = i + 1;


        } else {
            Log.d(TAG + "GAL-decode", "接收到的数据长度错误");
            return null;
        }
        if (satExist(ConstantSystem.GALILEO_SYSTEM, ephGps.getSatID()) == 0) {
            Log.d(TAG, "E" + ephGps.getSatID() + "不存在");
            return null;
        }
        //对gps周进行调整
        week = 1024 + week; /* gal-week = gst-week + 1024 */
        ephGps.setWeek(week);

        //System.out.println(ephGps.getWeek());

        //toe表示toe星历的参考时刻
        ephGps.setToe(toe);
        //System.out.println("toe" + toe);

        //toc表示卫星钟的参考时刻，和toe应该是一样的。
        ephGps.setToc(toc);
        //System.out.println("toc" + toc);
        //reftime  这个历元的时间
        ephGps.setRefTime(new Time(week, toc));

        //System.out.println("galileo历元时刻" + ephGps.getRefTime().toString());
        ephGps.setSvHealth((e5b_hs << 7) + (e5b_dvs << 6) + (e1_hs << 1) + (e1_dvs << 0));
        ephGps.setL2Code(0);/* data source = I/NAV */
        Log.d(TAG, "E" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString()+"sqrtA"+ephGps.getRootA());
        return ephGps;

    }

    public static EphGps decodeQzssEph(byte[] data) {

        EphGps ephGps = new EphGps();

        int week;
        double toe, toc;
        int i =  12;
        ephGps.setSatType(ConstantSystem.QZSS_SYSTEM);


        if (i + 473 <= data.length * 8) {
            //id
            ephGps.setSatID((int)getbitu(data, i, 4) + 192);
            i = i + 4;
            //System.out.println(ephGps.getSatID());
            //ephGps.setWeek(getbitu(data, i, 10));

            //toc
            toc = getbitu(data, i, 16) * 16.0;
            i = i + 16;

            //af2,af1,af0
            //2^-55
            ephGps.setAf2(getbits(data, i, 8) * Math.pow(2, -55));
            i = i + 8;
            //System.out.println(ephGps.getAf2());
            //2^-43
            ephGps.setAf1(getbits(data, i, 16) * Math.pow(2, -43));
            i = i + 16;
            //System.out.println("AF1" + ephGps.getAf1());
            //2^-31
            ephGps.setAf0(getbits(data, i, 22) * Math.pow(2, -31));
            i = i + 22;
            //System.out.println("AF0" + ephGps.getAf0());

            //iode
            ephGps.setIode((int)getbitu(data, i, 8));
            i = i + 8;
            //System.out.println("iode" + ephGps.getIode());

            //crs
            //2^-5
            ephGps.setCrs(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;

            //deltaN
            ephGps.setDeltaN(getbits(data, i, 16) * Math.pow(2, -43) * Math.PI);
            i = i + 16;
            //M0
            ephGps.setM0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //Cuc
            ephGps.setCuc(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //E
            ephGps.setE(getbitu(data, i, 32) * Math.pow(2, -33));
            i = i + 32;
            //Cus
            ephGps.setCus(getbits(data, i, 16) * Math.pow(2, -29));
            i = i + 16;
            //根号A
            ephGps.setRootA(ConstantSystem.UNKNOWN+getbitu(data, i, 32) * Math.pow(2, -19));
            i = i + 32;

            //toe
            toe = getbitu(data, i, 16) * 16.0;
            i = i + 16;
            //System.out.println("toe" + ephGps.getToe());

            //cic
            ephGps.setCic(getbits(data, i, 16) * Math.pow(2, -29));

            i = i + 16;
            //OMEGA
            ephGps.setOmega0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;

            //cis
            ephGps.setCis(getbits(data, i, 16) * Math.pow(2, -29));

            i = i + 16;
            //i0
            ephGps.setI0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);

            i = i + 32;
            //crc
            ephGps.setCrc(getbits(data, i, 16) * Math.pow(2, -5));
            i = i + 16;

            //omg
            ephGps.setOmg(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);

            i = i + 32;
            //OMEGADOT
            ephGps.setOmegaDot(getbits(data, i, 24) * Math.pow(2, -43) * Math.PI);

            i = i + 24;

            //idot
            ephGps.setiDot(getbits(data, i, 14) * Math.pow(2, -43) * Math.PI);
            i = i + 14;
            //System.out.println(ephGps.getiDot());

            //l2code
            ephGps.setL2Code((int)getbitu(data, i, 2));
            i = i + 2;
            //week
            week = (int)getbitu(data, i, 10);
            i = i + 10;
            //System.out.println(week);
            //sva
            ephGps.setSvAccur((int)getbitu(data, i, 4));
            i = i + 4;
            //System.out.println(ephGps.getSvAccur());
            //svh
            ephGps.setSvHealth((int)getbitu(data, i, 6));
            i = i + 6;
            //tgd
            ephGps.setTgd(getbits(data, i, 8) * Math.pow(2, -31));

            i = i + 8;
            //iodc
            ephGps.setIodc((int)getbitu(data, i, 10));
            i = i + 10;
            //System.out.println("iodc" + ephGps.getIodc());

//            ephGps.setL2Flag(getbitu(data, i, 1));
//            i = i + 1;
            //拟合区间如果不等于0，就把它定为2个小时；如果等于0，就说明没有给出
            ephGps.setFitInt((getbitu(data, i, 1) != 0) ? 2 : 0);
            //System.out.println(ephGps.getFitInt());

        } else {
            Log.d(TAG + "QZS-decode", "接收到的数据长度错误");
            return null;
        }
        if (satExist(ConstantSystem.QZSS_SYSTEM, ephGps.getSatID()) == 0) {
            Log.d(TAG, "J" + ephGps.getSatID() + "不存在");
            return null;
        }
        //对gps周进行调整
        week = adjustGpsWeek(week);
        ephGps.setWeek(week);

        //System.out.println(ephGps.getWeek());

        //toe表示toe星历的参考时刻
        ephGps.setToe(toe);
        //System.out.println("toe" + toe);

        //toc表示卫星钟的参考时刻，和toe应该是一样的。
        ephGps.setToc(toc);
        //System.out.println("toc" + toc);
        //reftime  这个历元的时间
        ephGps.setRefTime(new Time(week, toc));

        //System.out.println("历元时刻" + ephGps.getRefTime().toString());
        Log.d(TAG, "J" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString()+"sqrtA"+ephGps.getRootA());

        return ephGps;

    }

    public static EphGps decodeBeidouEph(byte[] data) {

        EphGps ephGps = new EphGps();

        int week;
        double toe, toc;
        int i =  12;
        ephGps.setSatType(ConstantSystem.BEIDOU_SYSTEM);
        if (i + 499 <= data.length * 8) {
            //id
            ephGps.setSatID((int)getbitu(data, i, 6));
            i = i + 6;
            //System.out.println(ephGps.getSatID());
            //week
            week = (int)getbitu(data, i, 13);
            i = i + 13;
            //System.out.println(week);
            //sva
            ephGps.setSvAccur((int)getbitu(data, i, 4));
            i = i + 4;
            //System.out.println(ephGps.getSvAccur());

            //idot
            ephGps.setiDot(getbits(data, i, 14) * Math.pow(2, -43) * Math.PI);
            i = i + 14;
            //System.out.println(ephGps.getiDot());
            //iode--------ADOE
            ephGps.setIode((int)getbitu(data, i, 5));
            i = i + 5;
            //System.out.println("iode" + ephGps.getIode());
            //toc
            toc = getbitu(data, i, 17) * 8.0;
            i = i + 17;
            //af2,af1,af0

            ephGps.setAf2(getbits(data, i, 11) * Math.pow(2, -66));
            i = i + 11;
            //System.out.println(ephGps.getAf2());

            ephGps.setAf1(getbits(data, i, 22) * Math.pow(2, -50));
            i = i + 22;
            //System.out.println("AF1" + ephGps.getAf1());
            ephGps.setAf0(getbits(data, i, 24) * Math.pow(2, -33));
            i = i + 24;
            //System.out.println("AF0" + ephGps.getAf0());
            //iodc----------ADOC
            ephGps.setIodc((int)getbitu(data, i, 5));
            i = i + 5;
            //System.out.println("iodc" + ephGps.getIodc());
            //crs
            ephGps.setCrs(getbits(data, i, 18) * Math.pow(2, -6));
            i = i + 18;
            //deltaN
            ephGps.setDeltaN(getbits(data, i, 16) * Math.pow(2, -43) * Math.PI);
            i = i + 16;
            //M0
            ephGps.setM0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //Cuc
            ephGps.setCuc(getbits(data, i, 18) * Math.pow(2, -31));
            i = i + 18;
            //e
            ephGps.setE(getbitu(data, i, 32) * Math.pow(2, -33));
            i = i + 32;
            //cus
            ephGps.setCus(getbits(data, i, 18) * Math.pow(2, -31));
            i = i + 18;
            //根号A
            ephGps.setRootA(getbitu(data, i, 32) * Math.pow(2, -19));
            i = i + 32;
            //toe
            toe = getbitu(data, i, 17) * 8.0;
            i = i + 17;
            //cic
            ephGps.setCic(getbits(data, i, 18) * Math.pow(2, -31));
            i = i + 18;
            //Omega0
            ephGps.setOmega0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);
            i = i + 32;
            //cis
            ephGps.setCis(getbits(data, i, 18) * Math.pow(2, -31));

            i = i + 18;
            //i0
            ephGps.setI0(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);

            i = i + 32;
            //crc
            ephGps.setCrc(getbits(data, i, 18) * Math.pow(2, -6));
            i = i + 18;
            //omg---w
            ephGps.setOmg(getbits(data, i, 32) * Math.pow(2, -31) * Math.PI);

            i = i + 32;
            //OmegaDot
            ephGps.setOmegaDot(getbits(data, i, 24) * Math.pow(2, -43) * Math.PI);
            i = i + 24;
            //TgdB1C
            ephGps.setTgdB1C(getbits(data, i, 10) * Math.pow(1, -10));

            i = i + 10;
            //TgdB2a
            ephGps.setTgdB2a(getbits(data, i, 10) * Math.pow(1, -10));

            i = i + 10;
            //SvHealth
            ephGps.setSvHealth((int)getbitu(data, i, 1));
            i = i + 1;


        } else {
            Log.d(TAG + "BDS-decode", "接收到的数据长度错误");
            return null;
        }
        if (satExist(ConstantSystem.BEIDOU_SYSTEM, ephGps.getSatID()) == 0) {
            Log.d(TAG, "C" + ephGps.getSatID() + "不存在");
            return null;
        }
        //对gps周进行调整---此时week是北斗周，北斗周和GPS周差了1356个周，北斗 开始时间2006.1.1，那么北斗周+1356就是GPS周
        week = bdtWeek2GpsWeek(week);
        ephGps.setWeek(week);

        //System.out.println(ephGps.getWeek());

        //toe表示toe星历的参考时刻
        ephGps.setToe(bdtWeekSec2GpsWeekSec(toe));
        //System.out.println("toe" + toe);

        //toc表示卫星钟的参考时刻，和toe应该是一样的。
        ephGps.setToc(bdtWeekSec2GpsWeekSec(toc));
        //System.out.println("toc" + toc);
        //reftime  这个历元的时间
        ephGps.setRefTime(new Time(week, toc));

        //System.out.println("历元时刻" + ephGps.getRefTime().toString());
        //Log.d(TAG, "C" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString());
        Log.d(TAG, "C" + ephGps.getSatID() + "---" + ephGps.getRefTime().toString()+"sqrtA"+ephGps.getRootA());

        return ephGps;


    }

    public static EphGps decodeGnssEph(byte[] data) {


        int type = (int)getbitu(data, 0, 12);

        switch (type) {
            case 1019:
                return decodeGpsEph(data);
            case 1042:
                return decodeBeidouEph(data);
            case 1045:
                return decodeGalileoEph(data);
            case 1046:
                return decodeGalileo1046Eph(data);
            case 1020:
                return decodeGlonassEph(data);
            case 1044:
                return decodeQzssEph(data);
            default:
                return null;
        }
    }



}
