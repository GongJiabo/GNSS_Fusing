package com.whu.gnss.gnsslogger.ntrip;

import android.util.Log;

import com.whu.gnss.gnsslogger.ConstantSystem;
import com.whu.gnss.gnsslogger.constellations.Time;
import com.whu.gnss.gnsslogger.coordinates.SatellitePosition;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

/**
 * 通过ntrip协议获取各个系统的广播星历
 * 支持GPS/GLO/GAL/BDS/QZSS
 * butterflying10
 * 2020/5/14
 */
public class GNSSEphemericsNtrip extends EphemerisSystem implements RTCM3ClientListener ,Runnable{

    private static final String TAG="GNSSEphemericsNtrip";


    /*
    存放GNSS广播星历
     */
    private List<EphGps> mEphGnssList=new ArrayList<>();

    public List<EphGps> getEphGnssList() {
        return mEphGnssList;
    }

    /*
        存放GPS广播星历
         */
    private List<EphGps> mEphGpsList=new ArrayList<>();

    public List<EphGps> getEphGpsList() {
        return mEphGpsList;
    }

    /*
        存放GAL广播星历
         */
    private List<EphGps> mEphGalList=new ArrayList<>();

    public List<EphGps> getEphGalList() {
        return mEphGalList;
    }

    /*
        存放GLO广播星历
         */
    private List<EphGps> mEphGloList=new ArrayList<>();

    public List<EphGps> getEphGloList() {
        return mEphGloList;
    }

    /*
        存放BDS广播星历
         */
    private List<EphGps> mEphBdsList=new ArrayList<>();

    public List<EphGps> getEphBdsList() {
        return mEphBdsList;
    }

    /*
        存放QZSS广播星历
         */
    private List<EphGps> mEphQzssList=new ArrayList<>();

    public List<EphGps> getEphQzssList() {
        return mEphQzssList;
    }

    /*
        ntrip
         */
    private RTCM3Client mRTCM3Client;
    public GNSSEphemericsNtrip(RTCM3Client rtcm3Client)
    {
        this.mRTCM3Client=rtcm3Client;
    }

    //哈希表---主要用于判断这个卫星和这个历元的数据是否已经接收到
    private Hashtable<Integer, Time> hashtable=new Hashtable<Integer, Time>();




    @Override
    public void onDataReceived(byte[] data) {


        EphGps eph= DecodeEphData.decodeGnssEph(data);

        if(eph!=null)
        {
            if (!hashtable.containsKey(eph.getSatType() + eph.getSatID()) || Objects.requireNonNull(hashtable.get(eph.getSatType() + eph.getSatID())).getGpsWeekSec() != eph.getRefTime().getGpsWeekSec()) {
//                if(eph.getSatType()== ConstantSystem.GPS_SYSTEM) {
//                    mEphGpsList.add(eph);
//                    hashtable.put(eph.getSatType() + eph.getSatID(), eph.getRefTime());
//                }
//                if(eph.getSatType()==ConstantSystem.GLONASS_SYSTEM) {
//                    mEphGloList.add(eph);
//                    hashtable.put(eph.getSatType() + eph.getSatID(), eph.getRefTime());
//                }
//                if(eph.getSatType()==ConstantSystem.GALILEO_SYSTEM) {
//                    mEphGalList.add(eph);
//                    hashtable.put(eph.getSatType() + eph.getSatID(), eph.getRefTime());
//                }
//                if(eph.getSatType()==ConstantSystem.BEIDOU_SYSTEM) {
//                    mEphBdsList.add(eph);
//                    hashtable.put(eph.getSatType() + eph.getSatID(), eph.getRefTime());
//                }
//                if(eph.getSatType()==ConstantSystem.QZSS_SYSTEM) {
//                    mEphQzssList.add(eph);
//                    hashtable.put(eph.getSatType() + eph.getSatID(), eph.getRefTime());
//                }
                mEphGnssList.add(eph);
                hashtable.put(eph.getSatType() + eph.getSatID(), eph.getRefTime());



            }

        }
    }

    @Override
    public void run() {

        //建立ntrip-scoket连接
        mRTCM3Client.run();
    }

    public void stopNtrip()
    {
        this.mRTCM3Client.stop();
    }

    //找对应的广播星历

    /**
     *
     * @param unixTime
     * @param satID
     * @param satType
     * @return
     */
    public EphGps findEphGps(long unixTime,int satID,char satType)
    {
        long dt = 0;
        long dtMin = 0;
        long dtMax = 0;
        long delta = 0;
        EphGps refEph = null;

        //long gpsTime = (new Time(unixTime)).getGpsTime();

        for (int i = 0; i < mEphGnssList.size(); i++) {
            // Find ephemeris sets for given satellite
            if (mEphGnssList.get(i).getSatID() == satID && mEphGnssList.get(i).getSatType() == satType) {
                // Compare current time and ephemeris reference time
                dt = Math.abs(mEphGnssList.get(i).getRefTime().getMsec() - unixTime /*getGpsTime() - gpsTime*/) / 1000;

                // System.out.println("G"+ephBeidouList.get(i).getSatID()+"   "+ephBeidouList.get(i).getRefTime().getMsec()+"   "+unixTime);
                // If it's the first round, set the minimum time difference and
                // select the first ephemeris set candidate; if the current ephemeris set
                // is closer in time than the previous candidate, select new candidate
                if (refEph == null || dt < dtMin) {
                    dtMin = dt;
                    refEph = mEphGnssList.get(i);
                }
            }
        }

        if (refEph == null)
            return null;

        if (refEph.getSvHealth() != 0) {
            return EphGps.UnhealthyEph;
        }

        //maximum allowed interval from ephemeris reference time
        long fitInterval = refEph.getFitInt();

        if (fitInterval != 0) {
            dtMax = fitInterval * 3600 / 2;
        } else {
            switch (refEph.getSatType()) {
                case ConstantSystem.GLONASS_SYSTEM:
                    dtMax = 950;
                case ConstantSystem.QZSS_SYSTEM:
                    dtMax = 3600;
                default:
                    dtMax = 7200;
            }
        }
        if (dtMin > dtMax) {
            refEph = null;
        }

        return refEph;
    }

    /**
     *
     * @param unixTime   时间戳
     * @param range        伪距
     * @param satID
     * @param satType
     * @param receiverClockError       接收机钟差，一般定为0.0
     * @return
     */

    public SatellitePosition getSatPositionAndVelocities(long unixTime, double range, int satID, char satType, double receiverClockError)
    {

        EphGps ephGps=findEphGps(unixTime,satID,satType);


        //Log.d(TAG,"getSatPositionAndVelocities  "+ephGps.getRefTime().toString());


        if(ephGps==null)
        {
            Log.d(TAG, "未找到此卫星的广播星历"+satType+satID);
            return null;
        }
        if(ephGps.equals(EphGps.UnhealthyEph))
            return SatellitePosition.UnhealthySat;
        SatellitePosition satellitePosition=computeSatPositionAndVelocities(unixTime,range,satID,satType, ephGps,receiverClockError);

        if(satellitePosition==null)
        {
            return null;
        }
        return satellitePosition;

    }

}
