package  com.whu.gnss.gnsslogger.constellations;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.os.Build;


import com.whu.gnss.gnsslogger.GNSSConstants;
import com.whu.gnss.gnsslogger.ntrip.GNSSEphemericsNtrip;
import com.whu.gnss.gnsslogger.coordinates.Coordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sebastian Ciuban on 8/10/2018.
 */

public class GalileoE5aConstellation extends Constellation {
    private final static char satType = 'E';
    private static final String NAME = "Galileo E5a";
    private static final String TAG = "GalileoE5aConstellation";
    private static int constellationId = GnssStatus.CONSTELLATION_GALILEO;
    private static double E5a_FREQUENCY = 1.17645e9;
    private static double FREQUENCY_MATCH_RANGE = 0.1e9;
    private static double MASK_ELEVATION = 15; // degrees
    private static double MASK_CN0 = 10; // dB-Hz

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;



    private double tRxGalileoTOW;
    private double weekNumber;

    /**
     * Time of the measurement
     */


    private int visibleButNotUsed = 0;

    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]


    /**
     * List holding observed satellites
     */
    public List<SatelliteParameters> observedSatellites = new ArrayList<>();

    protected List<SatelliteParameters> unusedSatellites = new ArrayList<>();

//    private long timeRx;



    /**
     * Corrections which are to be applied to received pseudoranges
     */




    public static boolean approximateEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }




    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event){
        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            unusedSatellites.clear();
            GnssClock gnssClock       = event.getClock();
            long TimeNanos            = gnssClock.getTimeNanos();
//            timeRefMsec               = new Time(System.currentTimeMillis());
            double BiasNanos          = gnssClock.getBiasNanos();
            String strFullBiasNanos   = Long.toString(gnssClock.getFullBiasNanos());
            long dayFullBias          = Long.valueOf(strFullBiasNanos.substring(0,11));
            long podFullBiasNanos     = (long) -1.0 * Long.valueOf(strFullBiasNanos.substring(11,20));

            double tRx, tTx, modDayFullBias, PrSeconds, pseudorangeE5a, galileoTime;

//            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
                FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }



            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                if(measurement.getSvid() == 27 || measurement.getSvid() == 25) //todo: hardcoded exlusion of a faulty satellite (SUPL not working)
                    continue;

                if(!(measurement.hasCarrierFrequencyHz()
                        && approximateEqual(measurement.getCarrierFrequencyHz(), E5a_FREQUENCY, FREQUENCY_MATCH_RANGE)))
                    continue;


                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                double TimeOffsetNanos = measurement.getTimeOffsetNanos();

                // Compute the reception time in nanoseconds (this method is needed for later processing, is not a duplicate)
                galileoTime             = TimeNanos - (FullBiasNanos + BiasNanos);
                tRxGalileoTOW           = galileoTime % GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK;

                // Compute the time of reception in seconds
                tRx                     = 1e-9 * (TimeNanos - (podFullBiasNanos + BiasNanos));

                // Compute the weeknumber
                weekNumber              = Math.floor(-dayFullBias / GNSSConstants.WEEKSEC);
                modDayFullBias          = (dayFullBias + weekNumber * GNSSConstants.WEEKSEC);

                // Compute the time of signal transmission
                tTx = 1e-9 * (ReceivedSvTimeNanos + TimeOffsetNanos);
                tTx = tTx + modDayFullBias;

                // Galileo pseudorange computation
                PrSeconds = tRx - tTx;

                // Get the measurement state
                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean towKnown        = false;
                boolean towKnownValid   = false;

                // this is just a security measure, so that the code will not be crashing
                // if someone uses it on a lower API phone.
                // No phones with dual frequency will fail this condition
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) > 0;
                    towKnownValid = true;
                }

                boolean towDecoded      = (measState & GnssMeasurement.STATE_TOW_DECODED) > 0;

                boolean codeLockE5a = (measState & GnssMeasurement.STATE_CODE_LOCK) > 0;
                boolean msecAmbiguity = (measState & GnssMeasurement.STATE_MSEC_AMBIGUOUS) > 0;

                if (towKnownValid) {
                    // Solve for the 100 millisecond ambiguity
                    if (towDecoded && towKnown)
                        PrSeconds = PrSeconds % GNSSConstants.HUNDREDSMILLI;

                    // Solve for the 1 millisecond ambiguity
                    if (!towDecoded && !towKnown && msecAmbiguity)
                        PrSeconds = Math.floor(PrSeconds / 1e-3) * 1e-3 + PrSeconds % GNSSConstants.ONEMILLI;
                }

                // Compute the pseudorange in meters
                pseudorangeE5a = PrSeconds * GNSSConstants.SPEED_OF_LIGHT;


                // Variables for debugging
                int svID = measurement.getSvid();
                boolean condition = false;
                if (towKnownValid)
                    condition = towDecoded || msecAmbiguity ||towKnown;
                else
                    condition = towDecoded;

                if ( condition && pseudorangeE5a < 1e9 ) {

                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            new Pseudorange(pseudorangeE5a, 0.0));

//                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "<sub>E5a</sub>");
                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E5a");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());


                    /**
                     * 获取载波相位观测值
                     */

                    double ADR = measurement.getAccumulatedDeltaRangeMeters();
                    double λ = GNSSConstants.SPEED_OF_LIGHT / E5a_FREQUENCY;
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
//                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumber + ", " + tRxGalileoTOW + ", " + pseudorangeE5a);
//                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
//                    Log.d(TAG, "Time: " + satelliteParameters.getGpsTime().getGpsTimeString()+",phase"+satelliteParameters.getPhase()+",snr"+satelliteParameters.getSnr()+",doppler"+satelliteParameters.getDoppler());

                } else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            null
                    );
                    satelliteParameters.setUniqueSatId("E" + satelliteParameters.getSatId() + "_E5a");
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
    public void calculateSatPosition(GNSSEphemericsNtrip gnssEphemerisNtrip, Coordinates position) {

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
    public Time getTime() {
        return null;
    }

    @Override
    public Coordinates getRxPos() {
        return null;
    }

    @Override
    public void setRxPos(Coordinates rxPos) {

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
    public List<SatelliteParameters> getSPPUsedSatellites() {
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
