package  com.whu.gnss.gnsslogger.constellations;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.os.Build;
import android.util.Log;

import com.whu.gnss.gnsslogger.ConstantSystem;
import com.whu.gnss.gnsslogger.GNSSConstants;
import com.whu.gnss.gnsslogger.ntrip.GNSSEphemericsNtrip;
import com.whu.gnss.gnsslogger.coordinates.Coordinates;
import com.whu.gnss.gnsslogger.coordinates.SatellitePosition;
import com.whu.gnss.gnsslogger.corrections.Correction;
import com.whu.gnss.gnsslogger.corrections.ShapiroCorrection;
import com.whu.gnss.gnsslogger.corrections.TopocentricCoordinates;
import com.whu.gnss.gnsslogger.corrections.TropoCorrection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mateusz Krainski on 17/02/2018.
 * This class is for...
 * <p>
 * GPS Pseudorange computation algorithm by: Mareike Burba
 * - variable name changes and comments were added
 * to fit the description in the GSA white paper
 * by: Sebastian Ciuban
 */

public class GpsConstellation extends Constellation {

    private final static char satType = ConstantSystem.GPS_SYSTEM;
    private static final String NAME = "GPS L1";
    private static final String TAG = "GpsConstellation";
    private static double L1_FREQUENCY = 1.57542e9;
    private static double FREQUENCY_MATCH_RANGE = 0.1e9;

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    //接收机位置
    private Coordinates rxPos;

    public Coordinates getRxPos() {
        synchronized (this) {
            return rxPos;
        }
    }
    public void setRxPos(Coordinates rxPos) {
        synchronized (this) {
            this.rxPos = rxPos;
        }
    }
    protected double tRxGPS;
    protected double weekNumberNanos;
    private List<SatelliteParameters> unusedSatellites = new ArrayList<>();

    public double getWeekNumber(){
        return weekNumberNanos;
    }

    public double gettRxGPS(){
        return tRxGPS;
    }

    private static final int constellationId = GnssStatus.CONSTELLATION_GPS;
    private static double MASK_ELEVATION = 20; // degrees
    private static double MASK_CN0 = 10; // dB-Hz



    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections = new ArrayList<>();

    public GpsConstellation() {
        corrections.add(new ShapiroCorrection());
        corrections.add(new TropoCorrection());
    }

    /**
     * Time of the measurement
     */
    private Time timeRefMsec;


    protected int visibleButNotUsed = 0;

    // Condition for the pseudoranges that takes into account a maximum uncertainty for the TOW
    // (as done in gps-measurement-tools MATLAB code)
    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]



    /**
     * List holding observed satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();








    @Override
    public String getName() {
        synchronized (this) {
            return NAME;
        }
    }

    public static boolean approximateEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {

        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            unusedSatellites.clear();
            GnssClock gnssClock = event.getClock();
            long TimeNanos = gnssClock.getTimeNanos();
            double BiasNanos = gnssClock.getBiasNanos();
            double gpsTime, pseudorange;
            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
                FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }

            timeRefMsec = new Time(System.currentTimeMillis());

            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                if (measurement.hasCarrierFrequencyHz())
                    if (!approximateEqual(measurement.getCarrierFrequencyHz(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE))
                        continue;
                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();

                //Log.d(TAG,"G"+measurement.getSvid()+":"+measurement.getReceivedSvTimeNanos());

                double TimeOffsetNanos = measurement.getTimeOffsetNanos();

                // GPS Time generation (GSA White Paper - page 20)
                gpsTime =
                        TimeNanos - (FullBiasNanos + BiasNanos); // TODO intersystem bias?

                // Measurement time in full GPS time without taking into account weekNumberNanos(the number of
                // nanoseconds that have occurred from the beginning of GPS time to the current
                // week number)
                tRxGPS =
                        gpsTime + TimeOffsetNanos;

                weekNumberNanos =
                        Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK)
                                * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK;

                // GPS pseudorange computation
                pseudorange =
                        (tRxGPS - weekNumberNanos - ReceivedSvTimeNanos) / 1.0E9
                                * GNSSConstants.SPEED_OF_LIGHT;

                // TODO Check that the measurement have a valid state such that valid pseudoranges are used in the PVT algorithm

                /*

                According to https://developer.android.com/ the GnssMeasurements States required
                for GPS valid pseudoranges are:

                int STATE_CODE_LOCK         = 1      (1 << 0)
                int int STATE_TOW_DECODED   = 8      (1 << 3)

                */

                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean codeLock = (measState & GnssMeasurement.STATE_CODE_LOCK) != 0;
                boolean towDecoded = (measState & GnssMeasurement.STATE_TOW_DECODED) != 0;
                boolean towKnown      = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) != 0;
                }
//                boolean towUncertainty = measurement.getReceivedSvTimeUncertaintyNanos() <  MAXTOWUNCNS;
//
                if ( codeLock && (towDecoded || towKnown)  && pseudorange < 1e9) { // && towUncertainty
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            new Pseudorange(pseudorange, 0.0));

                    satelliteParameters.setUniqueSatId("G" + satelliteParameters.getSatId() + "_L1");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    /**
                     * 获取载波相位观测值
                     */

                    double ADR = measurement.getAccumulatedDeltaRangeMeters();
                    double λ = GNSSConstants.SPEED_OF_LIGHT / L1_FREQUENCY;
                    double phase = ADR / λ;
                    satelliteParameters.setPhase(phase);


                    /**
                     获取SNR
                     */
                    if (measurement.hasSnrInDb()) {
                        satelliteParameters.setSnr(measurement.getSnrInDb());
                    }
                    Log.d(TAG,"snr:"+measurement.getSnrInDb());
                    /**
                     获取多普勒值
                     */
                    double doppler = measurement.getPseudorangeRateMetersPerSecond() / λ;
                    satelliteParameters.setDoppler(doppler);

                    observedSatellites.add(satelliteParameters);

//                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGPS + ", " + pseudorange);
//                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
//                    Log.d(TAG, "Time: " + satelliteParameters.getGpsTime().getGpsTimeString()+",phase"+satelliteParameters.getPhase()+",snr"+satelliteParameters.getSnr()+",doppler"+satelliteParameters.getDoppler());

                }
                else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                        measurement.getSvid(),
                        null);

                    satelliteParameters.setUniqueSatId("G" + satelliteParameters.getSatId() + "_L1");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    unusedSatellites.add(satelliteParameters);
                    visibleButNotUsed++;
                    Log.d(TAG, visibleButNotUsed+"---");
                }
            }
        }
    }

    /**
     * @param gpsEphemerisNtrip
     * @param position  接收机的近似位置
     */

    @Override
    public void calculateSatPosition(GNSSEphemericsNtrip gpsEphemerisNtrip, Coordinates position) {

        // Make a list to hold the satellites that are to be excluded based on elevation/CN0 masking criteria
        List<SatelliteParameters> excludedSatellites = new ArrayList<>();



        synchronized (this) {
            //System.out.println("calculateSatPosition  此历元卫星数：" + observedSatellites.size());


            //接收机的位置，这里用接收机的位置主要是为了计算对流层延迟
            rxPos = Coordinates.globalXYZInstance(position.getX(), position.getY(), position.getZ());

            System.out.println("calculateSatPosition   接收机近似位置：" + position.getX() + "," + position.getY() + "," + position.getZ());

            for (SatelliteParameters observedSatellite : observedSatellites) {
                // Computation of the GPS satellite coordinates in ECEF frame

                // Determine the current GPS week number
                int gpsWeek = (int) (weekNumberNanos / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK);

                // Time of signal reception in GPS Seconds of the Week (SoW)
                double gpsSow = (tRxGPS - weekNumberNanos) * 1e-9;
                Time tGPS = new Time(gpsWeek, gpsSow);

                //Log.d(TAG,"calculateSatPosition"+tGPS.toString());

                // Convert the time of reception from GPS SoW to UNIX time (milliseconds)
                long timeRx = tGPS.getMsec();

                SatellitePosition sp = gpsEphemerisNtrip.getSatPositionAndVelocities(
                        timeRx,
                        observedSatellite.getPseudorange(),
                        observedSatellite.getSatId(),
                        satType,
                        0.0
                );

                if (sp == null) {
                    excludedSatellites.add(observedSatellite);
                    //GnssCoreService.notifyUser("Failed getting ephemeris data!", Snackbar.LENGTH_SHORT, RNP_NULL_MESSAGE);
                    //跳出循环
                    continue;
                }


                observedSatellite.setSatellitePosition(sp);

                observedSatellite.setRxTopo(
                        new TopocentricCoordinates(
                                rxPos,
                                observedSatellite.getSatellitePosition()));

                //Add to the exclusion list the satellites that do not pass the masking criteria
                if (observedSatellite.getRxTopo().getElevation() < MASK_ELEVATION) {
                    excludedSatellites.add(observedSatellite);
                }
                System.out.println("calculateSatPosition  此卫星高度角"+observedSatellite.getRxTopo().getElevation()+"\\"+observedSatellite.getRxTopo().getAzimuth());
                double accumulatedCorrection = 0;
                //计算累计的误差，包括对流层延迟,和相对论效应
                for (Correction correction : corrections) {

                    correction.calculateCorrection(
                            new Time(timeRx),
                            rxPos,
                            observedSatellite.getSatellitePosition()
                    );
                    accumulatedCorrection += correction.getCorrection();
                }


                //System.out.println("calculateSatPosition 此卫星误差为G：" + observedSatellite.getSatId() + "," + accumulatedCorrection);

                observedSatellite.setAccumulatedCorrection(accumulatedCorrection);
            }






            // Remove from the list all the satellites that did not pass the masking criteria
            visibleButNotUsed += excludedSatellites.size();
            observedSatellites.removeAll(excludedSatellites);
            unusedSatellites.addAll(excludedSatellites);

            //这是伪距定位时用到的卫星
            //实时定位，所以清理之前的
            SPPUsedSatellites.clear();

            SPPUsedSatellites.addAll(observedSatellites);
        }
    }


    @Override
    public int getConstellationId() {
        synchronized (this) {
            return constellationId;
        }
    }

    @Override
    public Time getTime() {
        return timeRefMsec;
    }



    @Override
    public SatelliteParameters getSatellite(int index) {
        synchronized (this) {
            return observedSatellites.get(index);
        }
    }

    @Override
    public List<SatelliteParameters> getSatellites() {
        synchronized (this) {
            return observedSatellites;
        }
    }

    @Override
    public List<SatelliteParameters> getUnusedSatellites() {
        return unusedSatellites;
    }

    private List<SatelliteParameters> SPPUsedSatellites =new ArrayList<>();
    @Override
    public List<SatelliteParameters> getSPPUsedSatellites() {
        return SPPUsedSatellites;
    }


    @Override
    public int getVisibleConstellationSize() {
        synchronized (this) {
            return getUsedConstellationSize() + visibleButNotUsed;
        }
    }

    @Override
    public int getUsedConstellationSize() {
        synchronized (this) {
            return observedSatellites.size();
        }
    }

    @Override
    public double getSatelliteSignalStrength(int index) {
        return observedSatellites.get(index).getSignalStrength();
    }

}
