package com.whu.gnss.gnsslogger.ntrip;

public interface RTCM3ClientListener {


    void onDataReceived(byte[] data);


}
