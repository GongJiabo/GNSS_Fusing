package  com.whu.gnss.gnsslogger.constellations;

import android.annotation.SuppressLint;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
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

public class GlonassR1Constellation extends Constellation {
    private final static char satType = ConstantSystem.GLONASS_SYSTEM;
    private static final String NAME = "Glonass R1";
    private static final String TAG = "GlonassConstellation";
    private long FullBiasNanos;


    /**
     * Time of the measurement
     */
    private Time timeRefMsec;
    private Coordinates rxPos;
    private boolean fullBiasNanosInitialized = false;
    protected double tRxGlonass;
    protected double weekNumberNanos;
    protected double tRx;
    private List<SatelliteParameters> unusedSatellites = new ArrayList<>();

    private static final int constellationId = GnssStatus.CONSTELLATION_GLONASS;


    protected int visibleButNotUsed = 0;

    // Condition for the pseudoranges that takes into account a maximum uncertainty for the TOW
    // (as done in gps-measurement-tools MATLAB code)
    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]



    /**
     * List holding observed satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();



    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections = new ArrayList<>();

    private static double MASK_ELEVATION = 20; // degrees


    public GlonassR1Constellation()
    {
        corrections.add(new ShapiroCorrection());
        corrections.add(new TropoCorrection());
    }




    @Override
    public String getName() {
        synchronized (this) {
            return NAME;
        }
    }

    public static boolean approximateEqual(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {

        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            unusedSatellites.clear();
            GnssClock gnssClock = event.getClock();
            long TimeNanos = gnssClock.getTimeNanos();
            timeRefMsec = new Time(System.currentTimeMillis());
            double BiasNanos = gnssClock.getBiasNanos();
            double gpsTime, pseudorange;

            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
            FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }

            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                //&& (measurement.getState() & (1L << 3)) != 0

                //if (measurement.getCn0DbHz() >= 18)
                //{
                    int measState = measurement.getState();

                    long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                    double TimeOffsetNanos = measurement.getTimeOffsetNanos();

                    // GPS Time generation (GSA White Paper - page 20)
                    gpsTime = TimeNanos  - (FullBiasNanos + BiasNanos); // TODO intersystem bias?

                    // Measurement time in full GPS time without taking into account weekNumberNanos(the number of
                    // nanoseconds that have occurred from the beginning of GPS time to the current
                    // week number)
                    double dayNumberNanos = Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_DAY)
                            * GNSSConstants.NUMBER_NANO_SECONDS_PER_DAY;
                    weekNumberNanos = Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK)
                            * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK;

                    double tRxNanos = gnssClock.getTimeNanos() - FullBiasNanos - weekNumberNanos;
                    tRx= gpsTime+ TimeOffsetNanos;
                    tRxGlonass = tRx - dayNumberNanos + 10800e9 - 18e9;

                    //Log.d(TAG,"tRxGlonass:"+tRxGlonass);


                    double tRxSeconds = (tRxNanos + measurement.getTimeOffsetNanos()) * 1e-9;
                    double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
                    double tRxSeconds_GLO = tRxSeconds % 86400;
                    double tTxSeconds_GLO = tTxSeconds - 10800 + 18;

                    if(tTxSeconds_GLO < 0){
                        tTxSeconds_GLO = tTxSeconds_GLO + 86400;
                    }
                    tRxSeconds = tRxSeconds_GLO;
                    tTxSeconds = tTxSeconds_GLO;
//                    double txSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
//                    double txSeconds_GLO = txSeconds - 10800 + gnssClock.getLeapSecond();
//                    if (txSeconds_GLO < 0) {
//                        tRxGlonass -= 86400e9;
//                    }

                    /*另一种方法*/

                     pseudorange=(tRxGlonass-measurement.getReceivedSvTimeNanos())*1e-9*GNSSConstants.SPEED_OF_LIGHT;

                    // GPS pseudorange computation
                    double pseudorange1 = (tRxSeconds-tTxSeconds)
                            * GNSSConstants.SPEED_OF_LIGHT;

                    Log.d(TAG,"旧的方法："+pseudorange1+"新方法："+pseudorange);




//
//                    // Bitwise AND to identify the states
                    boolean codeLock = (measState & GnssMeasurement.STATE_CODE_LOCK) != 0;

                    if ( codeLock &&pseudorange1 < 1e9 && pseudorange1 > 0) { // && towUncertainty
                        SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                                measurement.getSvid(),
                                new Pseudorange(pseudorange1, 0.0));

                        satelliteParameters.setUniqueSatId(String.format("R%02d_R1", satelliteParameters.getSatId()));

                        satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                        satelliteParameters.setConstellationType(measurement.getConstellationType());

                        if (measurement.hasCarrierFrequencyHz())
                            satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                        /**
                         * 获取载波相位观测值
                         */

                        if (measurement.hasCarrierFrequencyHz()) {
                            double ADR = measurement.getAccumulatedDeltaRangeMeters();
                            double λ = GNSSConstants.SPEED_OF_LIGHT / measurement.getCarrierFrequencyHz();
                            double phase = ADR / λ;
                            satelliteParameters.setPhase(phase);
                        }

                        /**
                         获取SNR
                         */
                        if (measurement.hasSnrInDb()) {
                            satelliteParameters.setSnr(measurement.getSnrInDb());
                        }
                        /**
                         获取多普勒值
                         */
                        if (measurement.hasCarrierFrequencyHz()) {
                            double λ = GNSSConstants.SPEED_OF_LIGHT / measurement.getCarrierFrequencyHz();
                            double doppler = measurement.getPseudorangeRateMetersPerSecond() / λ;
                            satelliteParameters.setDoppler(doppler);

                        }

                        observedSatellites.add(satelliteParameters);
                        Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGlonass + ", " + pseudorange);
                        Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
                        Log.d(TAG, "Time: " + satelliteParameters.getGpsTime().getGpsTimeString()+",phase"+satelliteParameters.getPhase()+",snr"+satelliteParameters.getSnr()+",doppler"+satelliteParameters.getDoppler());
                    }


                    else {
                        SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                                measurement.getSvid(),
                                null);

                        satelliteParameters.setUniqueSatId(String.format("R%02d_R1", satelliteParameters.getSatId()));

                        Log.d(TAG,",,,,,,,,,,,");
                        satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                        satelliteParameters.setConstellationType(measurement.getConstellationType());

                        if (measurement.hasCarrierFrequencyHz())
                            satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                        unusedSatellites.add(satelliteParameters);
                        visibleButNotUsed++;
                    }

                }


            }
        }
    //}



    /**
     * @param glonassEphemerisNtrip
     * @param position  接收机的近似位置
     */

    @Override
    public void calculateSatPosition(GNSSEphemericsNtrip glonassEphemerisNtrip, Coordinates position) {

        // Make a list to hold the satellites that are to be excluded based on elevation/CN0 masking criteria
        List<SatelliteParameters> excludedSatellites = new ArrayList<>();



        synchronized (this) {
            System.out.println("calculateSatPosition  此历元卫星数：" + observedSatellites.size());


            //接收机的位置，这里用接收机的位置主要是为了计算对流层延迟
            rxPos = Coordinates.globalXYZInstance(position.getX(), position.getY(), position.getZ());

            System.out.println("calculateSatPosition   接收机近似位置：" + position.getX() + "," + position.getY() + "," + position.getZ());

            for (SatelliteParameters observedSatellite : observedSatellites) {
                // Computation of the GPS satellite coordinates in ECEF frame

                // Determine the current GPS week number
                int gpsWeek = (int) (weekNumberNanos / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK);

                // Time of signal reception in GPS Seconds of the Week (SoW)
                double gpsSow = (tRxGlonass - weekNumberNanos) * 1e-9;
                Time tGPS = new Time(gpsWeek, gpsSow);

                Log.d(TAG,"calculateSatPosition"+tGPS.toString());

                // Convert the time of reception from GPS SoW to UNIX time (milliseconds)
                long timeRx = tGPS.getMsec();

                SatellitePosition sp = glonassEphemerisNtrip.getSatPositionAndVelocities(
                        timeRx,
                        observedSatellite.getPseudorange(),
                        observedSatellite.getSatId(),
                        satType,
                        0.0
                );

                if (sp == null) {
                    excludedSatellites.add(observedSatellite);
                    //GnssCoreService.notifyUser("Failed getting ephemeris data!", Snackbar.LENGTH_SHORT, RNP_NULL_MESSAGE);
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


                System.out.println("calculateSatPosition 此卫星误差为：" + observedSatellite.getSatId() + "," + accumulatedCorrection);

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
    public double getSatelliteSignalStrength(int index) {
        synchronized (this) {
            return observedSatellites.get(index).getSignalStrength();
        }
    }

    @Override
    public int getConstellationId() {
        synchronized (this) {
            return constellationId;
        }
    }


    private List<SatelliteParameters> SPPUsedSatellites =new ArrayList<>();
    @Override
    public List<SatelliteParameters> getSPPUsedSatellites() {
        return SPPUsedSatellites;
    }

    @Override
    public Time getTime() {
        return timeRefMsec;
    }

    @Override
    public Coordinates getRxPos() {
        synchronized (this) {
            return rxPos;
        }
    }

    @Override
    public void setRxPos(Coordinates rxPos) {
        synchronized (this) {
            this.rxPos = rxPos;
        }
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
        return null;
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



}
