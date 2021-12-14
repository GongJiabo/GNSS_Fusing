package com.whu.gnss.gnsslogger.rinexFileLogger;

import android.os.Build;

import com.whu.gnss.gnsslogger.constellations.GpsTime;

public class RinexHeader {
    private String markName;
    private String markType;
    private String observerName;
    private String observerAgencyName;
    private String receiverNumber;
    private String receiverType;
    private String receiverVersion;
    private String antennaNumber;
    private String antennaType;
    private double antennaEccentricityEast;
    private double antennaEccentricityNorth;
    private double antennaHeight;
    private String cartesianX;
    private String cartesianY;
    private String cartesianZ;
    private GpsTime gpsTime;

    public RinexHeader(String markName, String markType, String observerName, String observerAgencyName, String receiverNumber, String receiverType, String receiverVersion, String antennaNumber, String antennaType, double antennaEccentricityEast, double antennaEccentricityNorth, double antennaHeight, String cartesianX, String cartesianY, String cartesianZ, GpsTime gpsTime) {
        this.markName = markName;
        this.markType = markType;
        this.observerName = observerName;
        this.observerAgencyName = observerAgencyName;
        this.receiverNumber = receiverNumber;
        this.receiverType = receiverType;
        this.receiverVersion = receiverVersion;
        this.antennaNumber = antennaNumber;
        this.antennaType = antennaType;
        this.antennaEccentricityEast = antennaEccentricityEast;
        this.antennaEccentricityNorth = antennaEccentricityNorth;
        this.antennaHeight = antennaHeight;
        this.cartesianX = cartesianX;
        this.cartesianY = cartesianY;
        this.cartesianZ = cartesianZ;
        this.gpsTime = gpsTime;
    }
    public String getMarkName() {
        return markName;
    }

    public String getMarkType() {
        return markType;
    }

    public String getObserverName() {
        return observerName;
    }

    public String getObserverAgencyName() {
        return observerAgencyName;
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public String getReceiverVersion() {
        return receiverVersion;
    }

    public String getAntennaNumber() {
        return antennaNumber;
    }

    public String getAntennaType() {
        return antennaType;
    }

    public double getAntennaEccentricityEast() {
        return antennaEccentricityEast;
    }

    public double getAntennaEccentricityNorth() {
        return antennaEccentricityNorth;
    }

    public double getAntennaHeight() {
        return antennaHeight;
    }

    public String getCartesianX() {
        return cartesianX;
    }

    public String getCartesianY() {
        return cartesianY;
    }

    public String getCartesianZ() {
        return cartesianZ;
    }

    public GpsTime getGpsTime() {
        return gpsTime;
    }
}
