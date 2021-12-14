/*
 * Copyright 2018 TFI Systems

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package  com.whu.gnss.gnsslogger.constellations;

import android.location.GnssMeasurementsEvent;


import com.whu.gnss.gnsslogger.ntrip.GNSSEphemericsNtrip;
import com.whu.gnss.gnsslogger.coordinates.Coordinates;

import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * Created by Mateusz Krainski on 1/28/2018.
 * This class implements an abstract class Constellation, which should be extended by each
 * implemented satellite constellation which is to be used.
 */

public abstract class Constellation {

    /**
     * Indicates if initialization has already been performed
     */
    private static boolean initialized = false;

    private static String TAG = "Constellation";

    protected static String RNP_NULL_MESSAGE = "RNP_NULL_MESSAGE";



    /**
     * Additional definition of an ID for a new constellation type
     */
    public static final int CONSTELLATION_GALILEO_GPS = 999; //todo is there a better way to define this?
    public static final int CONSTELLATION_GALILEO_IonoFree = 998; //todo is there a better way to define this?
    public static final int CONSTELLATION_GPS_IonoFree = 997; //todo is there a better way to define this?

    /**
     *
     * @return the estimated receiver position
     */
    public abstract Coordinates getRxPos();

    /**
     *
     * @param rxPos new estimated receiver position
     */
    public abstract void setRxPos(Coordinates rxPos);

    /**
     * Factory method for converting RxPos to a SimpleMatrix
     * @param rxPos rxPos Coordinates object
     * @return RxPos as a 4z1 vector
     */
    public static SimpleMatrix getRxPosAsVector(Coordinates rxPos){
        SimpleMatrix rxPosSimpleVector = new SimpleMatrix(4, 1);
        rxPosSimpleVector.set(0, rxPos.getX());
        rxPosSimpleVector.set(1, rxPos.getY());
        rxPosSimpleVector.set(2, rxPos.getZ());
        rxPosSimpleVector.set(3, 0);

        return rxPosSimpleVector;
    }

    /**
     *
     * @param index id
     * @return satellite of that id
     */
    public abstract SatelliteParameters getSatellite(int index);

    /**
     *
     * @return all satellites registered in the object
     */
    public abstract List<SatelliteParameters> getSatellites();

    public abstract List<SatelliteParameters> getUnusedSatellites();


    /**
     * 参与伪距定位的卫星列表
     */
    public abstract List<SatelliteParameters> getSPPUsedSatellites();

    /**
     *
     * @return size of the visible constellation
     */
    public abstract int getVisibleConstellationSize();

    /**
     *
     * @return size of the used constellation
     */
    public abstract int getUsedConstellationSize();


    /**
     * Returns signal strength to a satellite given by an index.
     * Warning: index is the index of the satellite as stored in internal list, not it's id.
     * @param index index of satellite
     * @return signal strength for the satellite given by {@code index}.
     */
    public abstract double getSatelliteSignalStrength(int index);

    /**
     * @return ID of the constellation
     */
    public abstract int getConstellationId();



    /**
     *
     * @return time of measurement
     */
    public abstract Time getTime();

    /**
     *
     * @return name of the constellation
     */
    public abstract String getName();

    /**
     * method invoked on every GNSS measurement event update. It should update satellite's internal
     * parameters.
     * @param event GNSS event
     */
    public abstract void updateMeasurements(GnssMeasurementsEvent event);

    public abstract void calculateSatPosition(GNSSEphemericsNtrip gnssEphemerisNtrip , Coordinates position);


}
