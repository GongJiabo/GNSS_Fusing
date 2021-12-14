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
 * Created by Sebastian Ciuban on 8/10/2018.
 */

public class GalileoConstellation extends Constellation {
    private final static char satType = ConstantSystem.GALILEO_SYSTEM;
    protected static final String NAME = "Galileo E1";
    private static final String TAG = "GalileoE1Constellation";
    private static int constellationId = GnssStatus.CONSTELLATION_GALILEO;
    private static final double E1a_FREQUENCY = 1.57542e9;
    private static final double FREQUENCY_MATCH_RANGE = 0.1e9;
    private static final double MASK_ELEVATION = 20; // degrees
    private static final double MASK_CN0 = 10; // dB-Hz





    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    private Coordinates rxPos;


    protected double tRxGalileoTOW;
    private double tRxGalileoE1_2nd;
    protected double weekNumber;

    public double getWeekNumber() {
        return weekNumber;
    }

    public double gettRxGalileoTOW() {
        return tRxGalileoTOW;
    }

    /**
     * Time of the measurement
     */
    private Time timeRefMsec;

    protected int visibleButNotUsed = 0;

    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]


    /**
     * List holding used satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();

    /**
     * List holding unused satellites
     */
    protected List<SatelliteParameters> unusedSatellites = new ArrayList<>();


//    private long timeRx;

    //private NavigationProducer rinexNavGalileo = null;

    /**
     * Corrections which are to be applied to received pseudoranges
     */
    private ArrayList<Correction> corrections = new ArrayList<>();

    public GalileoConstellation()
    {
        corrections.add(new ShapiroCorrection());
        corrections.add(new TropoCorrection());
    }

    @Override
    public Time getTime() {
        synchronized (this) {
            return timeRefMsec;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static boolean approximateEqual(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }





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
            double galileoTime, pseudorangeTOW, pseudorangeE1_2nd, tTxGalileo;

            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
                FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }

            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                if (measurement.hasCarrierFrequencyHz())
                    if (!approximateEqual(measurement.getCarrierFrequencyHz(), E1a_FREQUENCY, FREQUENCY_MATCH_RANGE))
                        continue;

                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                double TimeOffsetNanos = measurement.getTimeOffsetNanos();

                // Galileo Time generation (GSA White Paper - page 20)
                galileoTime = TimeNanos - (FullBiasNanos + BiasNanos);

                // Compute the time of signal reception for when  GNSS_MEASUREMENT_STATE_TOW_KNOWN or GNSS_MEASUREMENT_STATE_TOW_DECODED are true
                tRxGalileoTOW = galileoTime % GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK;

                // Measurement time in full Galileo time without taking into account weekNumberNanos(the number of
                // nanoseconds that have occurred from the beginning of GPS time to the current
                // week number)
                weekNumber =
                        Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK);

                // Compute the signal reception for when GNSS_MEASUREMENT_STATE_GAL_E1C_2ND_CODE_LOCK is true
                tRxGalileoE1_2nd = galileoTime % GNSSConstants.NumberNanoSeconds100Milli;

                tTxGalileo = ReceivedSvTimeNanos + TimeOffsetNanos;

                // Valid only if GNSS_MEASUREMENT_STATE_TOW_KNOWN or GNSS_MEASUREMENT_STATE_TOW_DECODED are true
                pseudorangeTOW = (tRxGalileoTOW - tTxGalileo) * 1e-9 * GNSSConstants.SPEED_OF_LIGHT;

                // Valid only if GNSS_MEASUREMENT_STATE_GAL_E1C_2ND_CODE_LOCK
                pseudorangeE1_2nd = ((galileoTime - tTxGalileo) % GNSSConstants.NumberNanoSeconds100Milli) * 1e-9 * GNSSConstants.SPEED_OF_LIGHT;


                /*

                According to https://developer.android.com/ and GSA White Paper (pg.20)
                the GnssMeasurements States required for GALILEO valid pseudoranges are:

                STATE_TOW_KNOWN                   = 16384                            (1 << 11)
                STATE_TOW_DECODED                 =     8                            (1 <<  3)
                STATE_GAL_E1C_2ND_CODE_LOCK       =  2048                            (1 << 11)

                */

                // Get the measurement state
                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean towKnown = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) != 0;
                }

                boolean towDecoded = (measState & GnssMeasurement.STATE_TOW_DECODED) != 0;

                boolean codeLockE1BC = (measState & GnssMeasurement.STATE_GAL_E1BC_CODE_LOCK) != 0;
                boolean codeLockE1C = (measState & GnssMeasurement.STATE_GAL_E1C_2ND_CODE_LOCK) != 0;

                // Variables for debugging
                double prTOW = pseudorangeTOW;
                double prE1_2nd = pseudorangeE1_2nd;
                double diffPR = prTOW - prE1_2nd;
                int svID = measurement.getSvid();

                if (towDecoded || towKnown) {

                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            new Pseudorange(pseudorangeTOW, 0.0));

                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E1");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if (measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());
                    /**
                     * 获取载波相位观测值
                     */

                    double ADR = measurement.getAccumulatedDeltaRangeMeters();
                    double λ = GNSSConstants.SPEED_OF_LIGHT / E1a_FREQUENCY;
                    double phase = ADR / λ;
                    satelliteParameters.setPhase(phase);


                    /**
                     获取SNR
                     */
                    if (measurement.hasSnrInDb()) {
                        satelliteParameters.setSnr(measurement.getSnrInDb());
                    }
                    /**
                     获取多普勒值
                     */
                    double doppler = measurement.getPseudorangeRateMetersPerSecond() / λ;
                    satelliteParameters.setDoppler(doppler);
                    observedSatellites.add(satelliteParameters);
//                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumber + ", " + tRxGalileoTOW + ", " + pseudorangeTOW);
//                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);


                } else if (codeLockE1C) {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            new Pseudorange(pseudorangeE1_2nd, 0.0)
                    );

                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E1");
                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());
                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if (measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    /**
                     * 获取载波相位观测值
                     */

                    double ADR = measurement.getAccumulatedDeltaRangeMeters();
                    double λ = GNSSConstants.SPEED_OF_LIGHT / E1a_FREQUENCY;
                    double phase = ADR / λ;
                    satelliteParameters.setPhase(phase);


                    /**
                     获取SNR
                     */
                    if (measurement.hasSnrInDb()) {
                        satelliteParameters.setSnr(measurement.getSnrInDb());
                    }
                    /**
                     获取多普勒值
                     */
                    double doppler = measurement.getPseudorangeRateMetersPerSecond() / λ;
                    satelliteParameters.setDoppler(doppler);
                    observedSatellites.add(satelliteParameters);
                  //  Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumber + ", " + tRxGalileoTOW + ", " + pseudorangeE1_2nd);
                  //  Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
                 //   Log.d(TAG, "Time: " + satelliteParameters.getGpsTime().getGpsTimeString() + ",phase" + satelliteParameters.getPhase() + ",snr" + satelliteParameters.getSnr() + ",doppler" + satelliteParameters.getDoppler());
                } else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            null
                    );

                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E1");
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

    @Override
    public void calculateSatPosition(GNSSEphemericsNtrip galileoEphemerisNtrip, Coordinates position) {

        // Make a list to hold the satellites that are to be excluded based on elevation/CN0 masking criteria
        List<SatelliteParameters> excludedSatellites = new ArrayList<>();


        synchronized (this) {
            System.out.println("此历元galileo卫星数：" + observedSatellites.size());


            //接收机的位置，这里用接收机的位置主要是为了计算对流层延迟
            rxPos = Coordinates.globalXYZInstance(position.getX(), position.getY(), position.getZ());

            //System.out.println("接收机近似位置：" + position.getX() + "," + position.getY() + "," + position.getZ());

            for (SatelliteParameters observedSatellite : observedSatellites) {
                // Computation of the GPS satellite coordinates in ECEF frame

                // Determine the current GPS week number
                int galileoWeek =(int) weekNumber;

                double galileoSow = (tRxGalileoTOW) * 1e-9;
                Time tGalileo = new Time(galileoWeek, galileoSow);

                // Convert the time of reception from GPS SoW to UNIX time (milliseconds)
                long timeRx = tGalileo.getMsec();
                //System.out.println("卫星"+observedSatellite.getUniqueSatId()+"   "+timeRx);


                SatellitePosition rnp = galileoEphemerisNtrip.getSatPositionAndVelocities(
                        timeRx,
                        observedSatellite.getPseudorange(),
                        observedSatellite.getSatId(),
                        satType,
                        0.0
                );

                if (rnp == null) {
                    excludedSatellites.add(observedSatellite);
                    //GnssCoreService.notifyUser("Failed getting ephemeris data!", Snackbar.LENGTH_SHORT, RNP_NULL_MESSAGE);
                    continue;
                }

                observedSatellite.setSatellitePosition(rnp);

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
                //计算累计的误差，包括对流层延迟
                for (Correction correction : corrections) {

                    correction.calculateCorrection(
                            new Time(timeRx),
                            rxPos,
                            observedSatellite.getSatellitePosition()
                    );

                    accumulatedCorrection += correction.getCorrection();

                }
                // System.out.println("galileo此卫星误差为E：" + observedSatellite.getSatId() + "," + accumulatedCorrection);


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
        return unusedSatellites;
    }

    private List<SatelliteParameters> SPPUsedSatellites=new ArrayList<>();
    @Override
    public List<SatelliteParameters> getSPPUsedSatellites() {
        return this.SPPUsedSatellites;
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
