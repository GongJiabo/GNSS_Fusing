package  com.whu.gnss.gnsslogger.constellations;

import android.annotation.SuppressLint;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.util.Log;


import com.whu.gnss.gnsslogger.constellations.satellites.BdsSatellite;
import com.whu.gnss.gnsslogger.constellations.satellites.EpochMeasurement;
import com.whu.gnss.gnsslogger.constellations.satellites.GalileoSatellite;
import com.whu.gnss.gnsslogger.constellations.satellites.GlonassSatellite;
import com.whu.gnss.gnsslogger.constellations.satellites.GpsSatellite;
import com.whu.gnss.gnsslogger.constellations.satellites.QzssSatellite;
import com.whu.gnss.gnsslogger.ntrip.GNSSEphemericsNtrip;
import com.whu.gnss.gnsslogger.coordinates.Coordinates;

import java.util.ArrayList;
import java.util.List;

public class GnssConstellation extends Constellation {


    private static double L1_FREQUENCY = 1.57542e9;
    private static double L5_FREQUENCY = 1.17645e9;
    private static double B1_FREQUENCY = 1.561098e9;

    private static final double E1a_FREQUENCY = 1.57542e9;
    private static double E5a_FREQUENCY = 1.17645e9;
    private static double FREQUENCY_MATCH_RANGE = 0.1e9;

    private GpsConstellation gpsConstellation = new GpsConstellation();
    private GalileoConstellation galileoConstellation = new GalileoConstellation();

    private GpsL5Constellation gpsL5Constellation = new GpsL5Constellation();
    private GalileoE5aConstellation galileoE5aConstellation = new GalileoE5aConstellation();

    private GlonassR1Constellation glonassR1Constellation = new GlonassR1Constellation();

    private BdsConstellation bdsConstellation = new BdsConstellation();

    private QzssConstellation qzssConstellation = new QzssConstellation();

    private QzssL5Constellation qzssL5Constellation = new QzssL5Constellation();

    private static final String NAME = "Galileo + GPS + BDS + Glonass +Qzss";

    /**
     * @param a
     * @param b
     * @param eps
     * @return
     */

    public static boolean approximateEqual(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    /**
     * List holding observed satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();
    private List<SatelliteParameters> unusedSatellites = new ArrayList<>();

    /**
     * 以下参数是判断参与伪距单点/差分定位的系统，1表示参与，0表示未参与
     */
    private int GPS_SYSTEM;
    private int GAL_SYSTEM;

    private int GLO_SYSTEM;
    private int BDS_SYSTEM;
    private int QZSS_SYSTEM;


    public GnssConstellation(int GPS_SYSTEM, int GAL_SYSTEM, int GLO_SYSTEM, int BDS_SYSTEM,int QZSS_SYSTEM) {

        this.GPS_SYSTEM = GPS_SYSTEM;
        this.GAL_SYSTEM = GAL_SYSTEM;
        this.GLO_SYSTEM = GLO_SYSTEM;
        this.BDS_SYSTEM = BDS_SYSTEM;
        this.QZSS_SYSTEM=QZSS_SYSTEM;

    }


    @Override
    public Coordinates getRxPos() {
        synchronized (this) {
            if (this.GPS_SYSTEM == 1)
                return gpsConstellation.getRxPos();
            if (this.GAL_SYSTEM == 1)
                return galileoConstellation.getRxPos();
            if (this.GLO_SYSTEM == 1)
                return glonassR1Constellation.getRxPos();
            if (this.BDS_SYSTEM == 1)
                return bdsConstellation.getRxPos();
            if (this.QZSS_SYSTEM == 1)
                return qzssConstellation.getRxPos();
            return null;
        }
    }

    @Override
    public void setRxPos(Coordinates rxPos) {
        synchronized (this) {
            gpsConstellation.setRxPos(rxPos);
            galileoConstellation.setRxPos(rxPos);
        }
    }

    @Override
    public SatelliteParameters getSatellite(int index) {
        synchronized (this) {
            return SPP_UsedSatellites.get(index);
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

    @Override
    public List<SatelliteParameters> getSPPUsedSatellites() {
        return SPP_UsedSatellites;
    }

    @Override
    public int getVisibleConstellationSize() {
        synchronized (this) {
            return observedSatellites.size() + unusedSatellites.size();
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
        synchronized (this) {
            return observedSatellites.get(index).getSignalStrength();
        }
    }

    @Override
    public int getConstellationId() {
        return 0;
    }


    @Override
    public Time getTime() {
        synchronized (this) {
            if (this.GPS_SYSTEM == 1)
                return gpsConstellation.getTime();
            if (this.GAL_SYSTEM == 1)
                return galileoConstellation.getTime();
            if (this.GLO_SYSTEM == 1)
                return glonassR1Constellation.getTime();
            if (this.BDS_SYSTEM == 1)
                return bdsConstellation.getTime();
            if (this.QZSS_SYSTEM == 1)
                return qzssConstellation.getTime();
            return null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {
        synchronized (this) {
            observedSatellites = new ArrayList<>();
            gpsConstellation.updateMeasurements(event);
            galileoConstellation.updateMeasurements(event);
            galileoE5aConstellation.updateMeasurements(event);
            gpsL5Constellation.updateMeasurements(event);
            bdsConstellation.updateMeasurements(event);
            glonassR1Constellation.updateMeasurements(event);
            qzssConstellation.updateMeasurements(event);
            qzssL5Constellation.updateMeasurements(event);

            /**
             * 把一个历元的所有的观测原始数据集合在一起  ，进行处理。
             */
            observedSatellites.addAll(gpsConstellation.getSatellites());
            observedSatellites.addAll(gpsL5Constellation.getSatellites());
            observedSatellites.addAll(qzssConstellation.getSatellites());
            observedSatellites.addAll(qzssL5Constellation.getSatellites());


            observedSatellites.addAll(galileoConstellation.getSatellites());
            observedSatellites.addAll(galileoE5aConstellation.getSatellites());

            observedSatellites.addAll(bdsConstellation.getSatellites());
            observedSatellites.addAll(glonassR1Constellation.getSatellites());

            long stss = System.currentTimeMillis();
            convertToEpochMessurement();
            long edss = System.currentTimeMillis();
            Log.d("转换用时", edss - stss + "ms");
            Log.d(NAME, "长度：" + observedSatellites.size());

            //转换完成

        }
    }

    /**
     * 获取一个历元的数据
     */
    private EpochMeasurement mEpochMeasurement;

    public EpochMeasurement getEpochMeasurement() {
        return mEpochMeasurement;
    }

    public void setEpochMeasurement(EpochMeasurement epochMeasurement) {
        mEpochMeasurement = epochMeasurement;
    }

    /**
     * 处理一个历元的原始数据，使其变为一个历元数据，，，，注意   这是一个历元，。。。。。。
     */

    private void convertToEpochMessurement() {

        try {

            GpsTime initalgps = observedSatellites.get(0).getGpsTime();

            /**
             * 存放历元下的gps卫星--原始数据
             */
            List<SatelliteParameters> epochgps_observedSatellites = new ArrayList<>();
            /**
             * 存放历元下的qzss卫星--原始数据
             */
            List<SatelliteParameters> epochqzss_observedSatellites = new ArrayList<>();

            /**
             * 存放历元下的bds卫星--原始数据
             */
            List<SatelliteParameters> epochbds_observedSatellites = new ArrayList<>();

            /**
             * 存放历元下的galileo卫星--原始数据
             */
            List<SatelliteParameters> epochgalileo_observedSatellites = new ArrayList<>();

            /**
             * 存放历元下的glonass卫星--原始数据
             */
            List<SatelliteParameters> epochglonass_observedSatellites = new ArrayList<>();

            List<Integer> hasDoublesvid = new ArrayList<>();

            for (SatelliteParameters satelliteParameters : observedSatellites) {


                if (satelliteParameters.getGpsTime().getGpsTimeString().equals(initalgps.getGpsTimeString())) {

                    if (satelliteParameters.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
                        epochgps_observedSatellites.add(satelliteParameters);
                    }
                    if (satelliteParameters.getConstellationType() == GnssStatus.CONSTELLATION_QZSS) {
                        epochqzss_observedSatellites.add(satelliteParameters);
                    }
                    if (satelliteParameters.getConstellationType() == GnssStatus.CONSTELLATION_BEIDOU) {
                        epochbds_observedSatellites.add(satelliteParameters);
                    }
                    if (satelliteParameters.getConstellationType() == GnssStatus.CONSTELLATION_GALILEO) {
                        epochgalileo_observedSatellites.add(satelliteParameters);
                    }
                    if (satelliteParameters.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) {
                        epochglonass_observedSatellites.add(satelliteParameters);
                    }


                }
            }
            {
                //initalgps = satelliteParameters.getGpsTime();


                /**
                 * 存放历元下的gps卫星数据---处理后的
                 */
                List<GpsSatellite> epoch_gpsSatelliteList = new ArrayList<>();
                /**
                 * 存放历元下的qzss卫星数据---处理后的
                 */
                List<QzssSatellite> epoch_qzssSatelliteList = new ArrayList<>();
                /**
                 * 存放历元下的bds卫星数据---处理后的
                 */
                List<BdsSatellite> epoch_bdsSatelliteList = new ArrayList<>();
                /**
                 * 存放历元下的galileo卫星数据---处理后的
                 */
                List<GalileoSatellite> epoch_galileoSatelliteList = new ArrayList<>();
                /**
                 * 存放历元下的glonass卫星数据---处理后的
                 */
                List<GlonassSatellite> epoch_glonassSatelliteList = new ArrayList<>();


                /**
                 * 处理gps卫星
                 */
                if (epochgps_observedSatellites.size() == 1) {
                    GpsSatellite gpsSatellite = new GpsSatellite();
                    int i = 0;
                    int svid = epochgps_observedSatellites.get(0).getSatId();
                    if (approximateEqual(epochgps_observedSatellites.get(i).getCarrierFrequency(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                        gpsSatellite.setPrn(addprn('G', svid));
                        gpsSatellite.setC1(epochgps_observedSatellites.get(i).getPseudorange());
                        gpsSatellite.setL1(epochgps_observedSatellites.get(i).getPhase());

                        gpsSatellite.setS1(epochgps_observedSatellites.get(i).getSignalStrength());

                        gpsSatellite.setD1(epochgps_observedSatellites.get(i).getDoppler());
                        epoch_gpsSatelliteList.add(gpsSatellite);
                        //  Log.d("gps", "svid" + gpsSatellite.getPrn() + "  L1:" + gpsSatellite.getL1() + "  C1:" + gpsSatellite.getC1() + "  D1:" + gpsSatellite.getD1() + "  S1:" + gpsSatellite.getS1() + "  L5:" + gpsSatellite.getL5() + "  C5:" + gpsSatellite.getC5() + "  D5:" + gpsSatellite.getD5() + "  S5:" + gpsSatellite.getS5());


                    }
                    if (approximateEqual(epochgps_observedSatellites.get(i).getCarrierFrequency(), L5_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                        gpsSatellite.setPrn(addprn('G', svid));
                        gpsSatellite.setC5(epochgps_observedSatellites.get(i).getPseudorange());
                        gpsSatellite.setL5(epochgps_observedSatellites.get(i).getPhase());

                        gpsSatellite.setS5(epochgps_observedSatellites.get(i).getSignalStrength());

                        gpsSatellite.setD5(epochgps_observedSatellites.get(i).getDoppler());
                        epoch_gpsSatelliteList.add(gpsSatellite);
                        //  Log.d("gps", "svid" + gpsSatellite.getPrn() + "  L1:" + gpsSatellite.getL1() + "  C1:" + gpsSatellite.getC1() + "  D1:" + gpsSatellite.getD1() + "  S1:" + gpsSatellite.getS1() + "  L5:" + gpsSatellite.getL5() + "  C5:" + gpsSatellite.getC5() + "  D5:" + gpsSatellite.getD5() + "  S5:" + gpsSatellite.getS5());

                    }
                }
                if (epochgps_observedSatellites.size() > 1) {
                    for (int i = 0; i < epochgps_observedSatellites.size(); i++) {

                        int svid = epochgps_observedSatellites.get(i).getSatId();


                        for (int j = i + 1; j < epochgps_observedSatellites.size(); j++) {

                            GpsSatellite gpsSatellite = new GpsSatellite();
                         /*
                           表明这个卫星有两个频率
                         */
                            if (epochgps_observedSatellites.get(i).getSatId() == epochgps_observedSatellites.get(j).getSatId()) {

                                hasDoublesvid.add(epochgps_observedSatellites.get(i).getSatId());
                            /*
                            表明L1频率在前，L5频率在后
                             */
                                if (epochgps_observedSatellites.get(i).getCarrierFrequency() > epochgps_observedSatellites.get(j).getCarrierFrequency()) {


                                    gpsSatellite.setPrn(addprn('G', svid));
                                    gpsSatellite.setC1(epochgps_observedSatellites.get(i).getPseudorange());
                                    gpsSatellite.setC5(epochgps_observedSatellites.get(j).getPseudorange());
                                    gpsSatellite.setL1(epochgps_observedSatellites.get(i).getPhase());
                                    gpsSatellite.setL5(epochgps_observedSatellites.get(j).getPhase());
                                    gpsSatellite.setS1(epochgps_observedSatellites.get(i).getSignalStrength());
                                    gpsSatellite.setS5(epochgps_observedSatellites.get(j).getSignalStrength());
                                    gpsSatellite.setD1(epochgps_observedSatellites.get(i).getDoppler());
                                    gpsSatellite.setD5(epochgps_observedSatellites.get(j).getDoppler());
                                    epoch_gpsSatelliteList.add(gpsSatellite);
                                    //     Log.d("gps", "svid" + gpsSatellite.getPrn() + "  L1:" + gpsSatellite.getL1() + "  C1:" + gpsSatellite.getC1() + "  D1:" + gpsSatellite.getD1() + "  S1:" + gpsSatellite.getS1() + "  L5:" + gpsSatellite.getL5() + "  C5:" + gpsSatellite.getC5() + "  D5:" + gpsSatellite.getD5() + "  S5:" + gpsSatellite.getS5());
                                }
                            /*
                            表明L5频率在前，L1频率在后
                             */
                                if (epochgps_observedSatellites.get(i).getCarrierFrequency() < epochgps_observedSatellites.get(j).getCarrierFrequency()) {


                                    gpsSatellite.setPrn(addprn('G', svid));
                                    gpsSatellite.setC1(epochgps_observedSatellites.get(j).getPseudorange());
                                    gpsSatellite.setC5(epochgps_observedSatellites.get(i).getPseudorange());
                                    gpsSatellite.setL1(epochgps_observedSatellites.get(j).getPhase());
                                    gpsSatellite.setL5(epochgps_observedSatellites.get(i).getPhase());
                                    gpsSatellite.setS1(epochgps_observedSatellites.get(j).getSignalStrength());
                                    gpsSatellite.setS5(epochgps_observedSatellites.get(i).getSignalStrength());
                                    gpsSatellite.setD1(epochgps_observedSatellites.get(j).getDoppler());
                                    gpsSatellite.setD5(epochgps_observedSatellites.get(i).getDoppler());
                                    epoch_gpsSatelliteList.add(gpsSatellite);
                                    //    Log.d("gps", "svid" + gpsSatellite.getPrn() + "  L1:" + gpsSatellite.getL1() + "  C1:" + gpsSatellite.getC1() + "  D1:" + gpsSatellite.getD1() + "  S1:" + gpsSatellite.getS1() + "  L5:" + gpsSatellite.getL5() + "  C5:" + gpsSatellite.getC5() + "  D5:" + gpsSatellite.getD5() + "  S5:" + gpsSatellite.getS5());
                                }
                                break;

                            }
                        /*
                        表明这个卫星只有一个频率
                         */
                            if (j == epochgps_observedSatellites.size() - 1 && !hasDoublesvid.contains(epochgps_observedSatellites.get(i).getSatId())) {


                                if (approximateEqual(epochgps_observedSatellites.get(i).getCarrierFrequency(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                                    gpsSatellite.setPrn(addprn('G', svid));
                                    gpsSatellite.setC1(epochgps_observedSatellites.get(i).getPseudorange());
                                    gpsSatellite.setL1(epochgps_observedSatellites.get(i).getPhase());

                                    gpsSatellite.setS1(epochgps_observedSatellites.get(i).getSignalStrength());

                                    gpsSatellite.setD1(epochgps_observedSatellites.get(i).getDoppler());
                                    epoch_gpsSatelliteList.add(gpsSatellite);
                                    //     Log.d("gps", "svid" + gpsSatellite.getPrn() + "  L1:" + gpsSatellite.getL1() + "  C1:" + gpsSatellite.getC1() + "  D1:" + gpsSatellite.getD1() + "  S1:" + gpsSatellite.getS1() + "  L5:" + gpsSatellite.getL5() + "  C5:" + gpsSatellite.getC5() + "  D5:" + gpsSatellite.getD5() + "  S5:" + gpsSatellite.getS5());


                                }
                                if (approximateEqual(epochgps_observedSatellites.get(i).getCarrierFrequency(), L5_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                                    gpsSatellite.setPrn(addprn('G', svid));
                                    gpsSatellite.setC5(epochgps_observedSatellites.get(i).getPseudorange());
                                    gpsSatellite.setL5(epochgps_observedSatellites.get(i).getPhase());

                                    gpsSatellite.setS5(epochgps_observedSatellites.get(i).getSignalStrength());

                                    gpsSatellite.setD5(epochgps_observedSatellites.get(i).getDoppler());
                                    epoch_gpsSatelliteList.add(gpsSatellite);
                                    //    Log.d("gps", "svid" + gpsSatellite.getPrn() + "  L1:" + gpsSatellite.getL1() + "  C1:" + gpsSatellite.getC1() + "  D1:" + gpsSatellite.getD1() + "  S1:" + gpsSatellite.getS1() + "  L5:" + gpsSatellite.getL5() + "  C5:" + gpsSatellite.getC5() + "  D5:" + gpsSatellite.getD5() + "  S5:" + gpsSatellite.getS5());

                                }


                            }
                        }


                    }
                }

                /**
                 * 处理qzss卫星
                 */
                if (epochqzss_observedSatellites.size() == 1) {
                    QzssSatellite qzssSatellite = new QzssSatellite();
                    int i = 0;
                    int svid = epochqzss_observedSatellites.get(0).getSatId() - 192;
                    if (approximateEqual(epochqzss_observedSatellites.get(i).getCarrierFrequency(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                        qzssSatellite.setPrn(addprn('J', svid));
                        qzssSatellite.setC1(epochqzss_observedSatellites.get(i).getPseudorange());
                        qzssSatellite.setL1(epochqzss_observedSatellites.get(i).getPhase());

                        qzssSatellite.setS1(epochqzss_observedSatellites.get(i).getSignalStrength());

                        qzssSatellite.setD1(epochqzss_observedSatellites.get(i).getDoppler());
                        epoch_qzssSatelliteList.add(qzssSatellite);
                        // Log.d("qzss", "svid" + qzssSatellite.getPrn() + "  L1:" + qzssSatellite.getL1() + "  C1:" + qzssSatellite.getC1() + "  D1:" + qzssSatellite.getD1() + "  S1:" + qzssSatellite.getS1() + "  L5:" + qzssSatellite.getL5() + "  C5:" + qzssSatellite.getC5() + "  D5:" + qzssSatellite.getD5() + "  S5:" + qzssSatellite.getS5());


                    }
                    if (approximateEqual(epochqzss_observedSatellites.get(i).getCarrierFrequency(), L5_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                        qzssSatellite.setPrn(addprn('J', svid));
                        qzssSatellite.setC5(epochqzss_observedSatellites.get(i).getPseudorange());
                        qzssSatellite.setL5(epochqzss_observedSatellites.get(i).getPhase());

                        qzssSatellite.setS5(epochqzss_observedSatellites.get(i).getSignalStrength());

                        qzssSatellite.setD5(epochqzss_observedSatellites.get(i).getDoppler());
                        epoch_qzssSatelliteList.add(qzssSatellite);
                        //  Log.d("qzss", "svid" + qzssSatellite.getPrn() + "  L1:" + qzssSatellite.getL1() + "  C1:" + qzssSatellite.getC1() + "  D1:" + qzssSatellite.getD1() + "  S1:" + qzssSatellite.getS1() + "  L5:" + qzssSatellite.getL5() + "  C5:" + qzssSatellite.getC5() + "  D5:" + qzssSatellite.getD5() + "  S5:" + qzssSatellite.getS5());

                    }
                }
                if (epochqzss_observedSatellites.size() > 1) {
                    for (int i = 0; i < epochqzss_observedSatellites.size(); i++) {

                        int svid = epochqzss_observedSatellites.get(i).getSatId() - 192;


                        for (int j = i + 1; j < epochqzss_observedSatellites.size(); j++) {

                            QzssSatellite qzssSatellite = new QzssSatellite();
                         /*
                           表明这个卫星有两个频率
                         */
                            if (epochqzss_observedSatellites.get(i).getSatId() == epochqzss_observedSatellites.get(j).getSatId()) {

                                hasDoublesvid.add(epochqzss_observedSatellites.get(i).getSatId());
                            /*
                            表明L1频率在前，L5频率在后
                             */
                                if (epochqzss_observedSatellites.get(i).getCarrierFrequency() > epochqzss_observedSatellites.get(j).getCarrierFrequency()) {


                                    qzssSatellite.setPrn(addprn('J', svid));
                                    qzssSatellite.setC1(epochqzss_observedSatellites.get(i).getPseudorange());
                                    qzssSatellite.setC5(epochqzss_observedSatellites.get(j).getPseudorange());
                                    qzssSatellite.setL1(epochqzss_observedSatellites.get(i).getPhase());
                                    qzssSatellite.setL5(epochqzss_observedSatellites.get(j).getPhase());
                                    qzssSatellite.setS1(epochqzss_observedSatellites.get(i).getSignalStrength());
                                    qzssSatellite.setS5(epochqzss_observedSatellites.get(j).getSignalStrength());
                                    qzssSatellite.setD1(epochqzss_observedSatellites.get(i).getDoppler());
                                    qzssSatellite.setD5(epochqzss_observedSatellites.get(j).getDoppler());
                                    epoch_qzssSatelliteList.add(qzssSatellite);
                                    //      Log.d("qzss", "svid" + qzssSatellite.getPrn() + "  L1:" + qzssSatellite.getL1() + "  C1:" + qzssSatellite.getC1() + "  D1:" + qzssSatellite.getD1() + "  S1:" + qzssSatellite.getS1() + "  L5:" + qzssSatellite.getL5() + "  C5:" + qzssSatellite.getC5() + "  D5:" + qzssSatellite.getD5() + "  S5:" + qzssSatellite.getS5());
                                }
                            /*
                            表明L5频率在前，L1频率在后
                             */
                                if (epochqzss_observedSatellites.get(i).getCarrierFrequency() < epochqzss_observedSatellites.get(j).getCarrierFrequency()) {


                                    qzssSatellite.setPrn(addprn('J', svid));
                                    qzssSatellite.setC1(epochqzss_observedSatellites.get(j).getPseudorange());
                                    qzssSatellite.setC5(epochqzss_observedSatellites.get(i).getPseudorange());
                                    qzssSatellite.setL1(epochqzss_observedSatellites.get(j).getPhase());
                                    qzssSatellite.setL5(epochqzss_observedSatellites.get(i).getPhase());
                                    qzssSatellite.setS1(epochqzss_observedSatellites.get(j).getSignalStrength());
                                    qzssSatellite.setS5(epochqzss_observedSatellites.get(i).getSignalStrength());
                                    qzssSatellite.setD1(epochqzss_observedSatellites.get(j).getDoppler());
                                    qzssSatellite.setD5(epochqzss_observedSatellites.get(i).getDoppler());
                                    epoch_qzssSatelliteList.add(qzssSatellite);
                                    //    Log.d("qzss", "svid" + qzssSatellite.getPrn() + "  L1:" + qzssSatellite.getL1() + "  C1:" + qzssSatellite.getC1() + "  D1:" + qzssSatellite.getD1() + "  S1:" + qzssSatellite.getS1() + "  L5:" + qzssSatellite.getL5() + "  C5:" + qzssSatellite.getC5() + "  D5:" + qzssSatellite.getD5() + "  S5:" + qzssSatellite.getS5());
                                }
                                break;

                            }
                        /*
                        表明这个卫星只有一个频率
                         */
                            if (j == epochqzss_observedSatellites.size() - 1 && !hasDoublesvid.contains(epochqzss_observedSatellites.get(i).getSatId())) {


                                if (approximateEqual(epochqzss_observedSatellites.get(i).getCarrierFrequency(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                                    qzssSatellite.setPrn(addprn('J', svid));
                                    qzssSatellite.setC1(epochqzss_observedSatellites.get(i).getPseudorange());
                                    qzssSatellite.setL1(epochqzss_observedSatellites.get(i).getPhase());

                                    qzssSatellite.setS1(epochqzss_observedSatellites.get(i).getSignalStrength());

                                    qzssSatellite.setD1(epochqzss_observedSatellites.get(i).getDoppler());
                                    epoch_qzssSatelliteList.add(qzssSatellite);
                                    //    Log.d("qzss", "svid" + qzssSatellite.getPrn() + "  L1:" + qzssSatellite.getL1() + "  C1:" + qzssSatellite.getC1() + "  D1:" + qzssSatellite.getD1() + "  S1:" + qzssSatellite.getS1() + "  L5:" + qzssSatellite.getL5() + "  C5:" + qzssSatellite.getC5() + "  D5:" + qzssSatellite.getD5() + "  S5:" + qzssSatellite.getS5());


                                }
                                if (approximateEqual(epochqzss_observedSatellites.get(i).getCarrierFrequency(), L5_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                                    qzssSatellite.setPrn(addprn('J', svid));
                                    qzssSatellite.setC5(epochqzss_observedSatellites.get(i).getPseudorange());
                                    qzssSatellite.setL5(epochqzss_observedSatellites.get(i).getPhase());

                                    qzssSatellite.setS5(epochqzss_observedSatellites.get(i).getSignalStrength());

                                    qzssSatellite.setD5(epochqzss_observedSatellites.get(i).getDoppler());
                                    epoch_qzssSatelliteList.add(qzssSatellite);
                                    //    Log.d("qzss", "svid" + qzssSatellite.getPrn() + "  L1:" + qzssSatellite.getL1() + "  C1:" + qzssSatellite.getC1() + "  D1:" + qzssSatellite.getD1() + "  S1:" + qzssSatellite.getS1() + "  L5:" + qzssSatellite.getL5() + "  C5:" + qzssSatellite.getC5() + "  D5:" + qzssSatellite.getD5() + "  S5:" + qzssSatellite.getS5());

                                }


                            }
                        }


                    }
                }

                /**
                 * 处理galileo卫星
                 */
                if (epochgalileo_observedSatellites.size() == 1) {

                     /*
                      表明只有一个卫星，只有一个频率
                     */
                    int svid = epochgalileo_observedSatellites.get(0).getSatId();
                    int i = 0;
                    GalileoSatellite galileoSatellite = new GalileoSatellite();


                    if (approximateEqual(epochgalileo_observedSatellites.get(i).getCarrierFrequency(), E1a_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                        galileoSatellite.setPrn(addprn('E', svid));
                        galileoSatellite.setC1(epochgalileo_observedSatellites.get(i).getPseudorange());
                        galileoSatellite.setL1(epochgalileo_observedSatellites.get(i).getPhase());

                        galileoSatellite.setS1(epochgalileo_observedSatellites.get(i).getSignalStrength());

                        galileoSatellite.setD1(epochgalileo_observedSatellites.get(i).getDoppler());
                        epoch_galileoSatelliteList.add(galileoSatellite);
                        //   Log.d("galileo", "svid" + galileoSatellite.getPrn() + "  L1:" + galileoSatellite.getL1() + "  C1:" + galileoSatellite.getC1() + "  D1:" + galileoSatellite.getD1() + "  S1:" + galileoSatellite.getS1() + "  L5:" + galileoSatellite.getL5() + "  C5:" + galileoSatellite.getC5() + "  D5:" + galileoSatellite.getD5() + "  S5:" + galileoSatellite.getS5());


                    }
                    if (approximateEqual(epochgalileo_observedSatellites.get(i).getCarrierFrequency(), E5a_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                        galileoSatellite.setPrn(addprn('E', svid));
                        galileoSatellite.setC5(epochgalileo_observedSatellites.get(i).getPseudorange());
                        galileoSatellite.setL5(epochgalileo_observedSatellites.get(i).getPhase());

                        galileoSatellite.setS5(epochgalileo_observedSatellites.get(i).getSignalStrength());

                        galileoSatellite.setD5(epochgalileo_observedSatellites.get(i).getDoppler());
                        epoch_galileoSatelliteList.add(galileoSatellite);
                        //   Log.d("galileo", "svid" + galileoSatellite.getPrn() + "  L1:" + galileoSatellite.getL1() + "  C1:" + galileoSatellite.getC1() + "  D1:" + galileoSatellite.getD1() + "  S1:" + galileoSatellite.getS1() + "  L5:" + galileoSatellite.getL5() + "  C5:" + galileoSatellite.getC5() + "  D5:" + galileoSatellite.getD5() + "  S5:" + galileoSatellite.getS5());

                    }
                }

                if (epochgalileo_observedSatellites.size() > 1) {
                    for (int i = 0; i < epochgalileo_observedSatellites.size(); i++) {

                        int svid = epochgalileo_observedSatellites.get(i).getSatId();


                        for (int j = i + 1; j < epochgalileo_observedSatellites.size(); j++) {

                            GalileoSatellite galileoSatellite = new GalileoSatellite();
                         /*
                           表明这个卫星有两个频率
                         */
                            if (epochgalileo_observedSatellites.get(i).getSatId() == epochgalileo_observedSatellites.get(j).getSatId()) {

                                hasDoublesvid.add(epochgalileo_observedSatellites.get(i).getSatId());
                            /*
                            表明L1频率在前，L5频率在后
                             */
                                if (epochgalileo_observedSatellites.get(i).getCarrierFrequency() > epochgalileo_observedSatellites.get(j).getCarrierFrequency()) {


                                    galileoSatellite.setPrn(addprn('E', svid));
                                    galileoSatellite.setC1(epochgalileo_observedSatellites.get(i).getPseudorange());
                                    galileoSatellite.setC5(epochgalileo_observedSatellites.get(j).getPseudorange());
                                    galileoSatellite.setL1(epochgalileo_observedSatellites.get(i).getPhase());
                                    galileoSatellite.setL5(epochgalileo_observedSatellites.get(j).getPhase());
                                    galileoSatellite.setS1(epochgalileo_observedSatellites.get(i).getSignalStrength());
                                    galileoSatellite.setS5(epochgalileo_observedSatellites.get(j).getSignalStrength());
                                    galileoSatellite.setD1(epochgalileo_observedSatellites.get(i).getDoppler());
                                    galileoSatellite.setD5(epochgalileo_observedSatellites.get(j).getDoppler());
                                    epoch_galileoSatelliteList.add(galileoSatellite);
                                    //       Log.d(" galileo", "svid" + galileoSatellite.getPrn() + "  L1:" + galileoSatellite.getL1() + "  C1:" + galileoSatellite.getC1() + "  D1:" + galileoSatellite.getD1() + "  S1:" + galileoSatellite.getS1() + "  L5:" + galileoSatellite.getL5() + "  C5:" + galileoSatellite.getC5() + "  D5:" + galileoSatellite.getD5() + "  S5:" + galileoSatellite.getS5());
                                }
                            /*
                            表明L5频率在前，L1频率在后
                             */
                                if (epochgalileo_observedSatellites.get(i).getCarrierFrequency() < epochgalileo_observedSatellites.get(j).getCarrierFrequency()) {


                                    galileoSatellite.setPrn(addprn('E', svid));
                                    galileoSatellite.setC1(epochgalileo_observedSatellites.get(j).getPseudorange());
                                    galileoSatellite.setC5(epochgalileo_observedSatellites.get(i).getPseudorange());
                                    galileoSatellite.setL1(epochgalileo_observedSatellites.get(j).getPhase());
                                    galileoSatellite.setL5(epochgalileo_observedSatellites.get(i).getPhase());
                                    galileoSatellite.setS1(epochgalileo_observedSatellites.get(j).getSignalStrength());
                                    galileoSatellite.setS5(epochgalileo_observedSatellites.get(i).getSignalStrength());
                                    galileoSatellite.setD1(epochgalileo_observedSatellites.get(j).getDoppler());
                                    galileoSatellite.setD5(epochgalileo_observedSatellites.get(i).getDoppler());
                                    epoch_galileoSatelliteList.add(galileoSatellite);
                                    //    Log.d(" galileo", "svid" + galileoSatellite.getPrn() + "  L1:" + galileoSatellite.getL1() + "  C1:" + galileoSatellite.getC1() + "  D1:" + galileoSatellite.getD1() + "  S1:" + galileoSatellite.getS1() + "  L5:" + galileoSatellite.getL5() + "  C5:" + galileoSatellite.getC5() + "  D5:" + galileoSatellite.getD5() + "  S5:" + galileoSatellite.getS5());
                                }
                                break;

                            }
                        /*
                        表明这个卫星只有一个频率
                         */
                            if (j == epochgalileo_observedSatellites.size() - 1 && !hasDoublesvid.contains(epochgalileo_observedSatellites.get(i).getSatId())) {


                                if (approximateEqual(epochgalileo_observedSatellites.get(i).getCarrierFrequency(), E1a_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                                    galileoSatellite.setPrn(addprn('E', svid));
                                    galileoSatellite.setC1(epochgalileo_observedSatellites.get(i).getPseudorange());
                                    galileoSatellite.setL1(epochgalileo_observedSatellites.get(i).getPhase());

                                    galileoSatellite.setS1(epochgalileo_observedSatellites.get(i).getSignalStrength());

                                    galileoSatellite.setD1(epochgalileo_observedSatellites.get(i).getDoppler());
                                    epoch_galileoSatelliteList.add(galileoSatellite);
                                    //    Log.d("galileo", "svid" + galileoSatellite.getPrn() + "  L1:" + galileoSatellite.getL1() + "  C1:" + galileoSatellite.getC1() + "  D1:" + galileoSatellite.getD1() + "  S1:" + galileoSatellite.getS1() + "  L5:" + galileoSatellite.getL5() + "  C5:" + galileoSatellite.getC5() + "  D5:" + galileoSatellite.getD5() + "  S5:" + galileoSatellite.getS5());


                                }
                                if (approximateEqual(epochgalileo_observedSatellites.get(i).getCarrierFrequency(), E5a_FREQUENCY, FREQUENCY_MATCH_RANGE)) {

                                    galileoSatellite.setPrn(addprn('E', svid));
                                    galileoSatellite.setC5(epochgalileo_observedSatellites.get(i).getPseudorange());
                                    galileoSatellite.setL5(epochgalileo_observedSatellites.get(i).getPhase());

                                    galileoSatellite.setS5(epochgalileo_observedSatellites.get(i).getSignalStrength());

                                    galileoSatellite.setD5(epochgalileo_observedSatellites.get(i).getDoppler());
                                    epoch_galileoSatelliteList.add(galileoSatellite);
                                    //    Log.d("galileo", "svid" + galileoSatellite.getPrn() + "  L1:" + galileoSatellite.getL1() + "  C1:" + galileoSatellite.getC1() + "  D1:" + galileoSatellite.getD1() + "  S1:" + galileoSatellite.getS1() + "  L5:" + galileoSatellite.getL5() + "  C5:" + galileoSatellite.getC5() + "  D5:" + galileoSatellite.getD5() + "  S5:" + galileoSatellite.getS5());

                                }


                            }
                        }


                    }
                }

                /**
                 * 处理bds卫星,,,bds卫星只有一个频率   B1
                 */
                for (int i = 0; i < epochbds_observedSatellites.size(); i++) {
                    int svid = epochbds_observedSatellites.get(i).getSatId();

                    /**
                     * 表明带的是B1频率
                     */
                    if (approximateEqual(epochbds_observedSatellites.get(i).getCarrierFrequency(), B1_FREQUENCY, FREQUENCY_MATCH_RANGE)) {
                        BdsSatellite bdsSatellite = new BdsSatellite();

                        bdsSatellite.setPrn(addprn('C', svid));

                        bdsSatellite.setC2I(epochbds_observedSatellites.get(i).getPseudorange());

                        bdsSatellite.setL2I(epochbds_observedSatellites.get(i).getPhase());

                        bdsSatellite.setD2I(epochbds_observedSatellites.get(i).getDoppler());

                        bdsSatellite.setS2I(epochbds_observedSatellites.get(i).getSignalStrength());

                        epoch_bdsSatelliteList.add(bdsSatellite);


                        //    Log.d("beidou", "svid : " + bdsSatellite.getPrn() + "  C2I: " + bdsSatellite.getC2I() + "  L2I:" + bdsSatellite.getL2I() + "  D2I:" + bdsSatellite.getD2I() + "  S2I" + bdsSatellite.getS2I());

                    }

                }

                /**
                 * 处理glonass卫星,,,glonass卫星只有一个频率   R1
                 */
                for (int i = 0; i < epochglonass_observedSatellites.size(); i++) {
                    int svid = epochglonass_observedSatellites.get(i).getSatId();

                    /**
                     * 表明带的是R1频率
                     */

                    GlonassSatellite glonassSatellite = new GlonassSatellite();

                    glonassSatellite.setPrn(addprn('R', svid));

                    glonassSatellite.setC1C(epochglonass_observedSatellites.get(i).getPseudorange());

                    glonassSatellite.setL1C(epochglonass_observedSatellites.get(i).getPhase());

                    glonassSatellite.setD1C(epochglonass_observedSatellites.get(i).getDoppler());

                    glonassSatellite.setS1C(epochglonass_observedSatellites.get(i).getSignalStrength());

                    epoch_glonassSatelliteList.add(glonassSatellite);


                    //    Log.d("glonass", "svid : " + glonassSatellite.getPrn() + "  C1C: " + glonassSatellite.getC1C() + "  L1C:" + glonassSatellite.getL1C() + "  D1C:" + glonassSatellite.getD1C() + "  S1C" + glonassSatellite.getS1C());

                }


                EpochMeasurement epochMeasurement = new EpochMeasurement(initalgps, epoch_gpsSatelliteList, epoch_qzssSatelliteList, epoch_bdsSatelliteList, epoch_galileoSatelliteList, epoch_glonassSatelliteList);

                this.setEpochMeasurement(epochMeasurement);


                epochgps_observedSatellites.clear();
                epochqzss_observedSatellites.clear();
                epochbds_observedSatellites.clear();
                epochgalileo_observedSatellites.clear();
                epochglonass_observedSatellites.clear();
                hasDoublesvid.clear();
            }


        }
        catch (Exception e) {
            Log.d(NAME, "原始数据转换为历元数据出错，可能为原始数据为空");
        }
    }

    /**
     * 对于卫星的prn不足长度  补零的方法
     *
     * @param constellationLabel 系统标签   如 G   J    C   R   E
     * @param svid               messurement.getsvid()
     * @return
     */
    private String addprn(char constellationLabel, int svid) {

        @SuppressLint("DefaultLocale") String prn = String.format("%c%02d", constellationLabel, svid);
        return prn;
    }


    private List<SatelliteParameters> SPP_UsedSatellites = new ArrayList<>();

    public List<SatelliteParameters> getSPP_UsedSatellites() {
        return this.SPP_UsedSatellites;
    }

    @Override
    public void calculateSatPosition(GNSSEphemericsNtrip gnssEphemericsNtrip, Coordinates position) {


        synchronized (this) {
            SPP_UsedSatellites.clear();


            if (this.GPS_SYSTEM == 1) {
                gpsConstellation.calculateSatPosition(gnssEphemericsNtrip, position);
                SPP_UsedSatellites.addAll(gpsConstellation.getSPPUsedSatellites());
            }
            if (this.GAL_SYSTEM == 1) {
                galileoConstellation.calculateSatPosition(gnssEphemericsNtrip, position);
                SPP_UsedSatellites.addAll(galileoConstellation.getSPPUsedSatellites());
            }
            if (this.GLO_SYSTEM == 1) {
                glonassR1Constellation.calculateSatPosition(gnssEphemericsNtrip, position);
                SPP_UsedSatellites.addAll(glonassR1Constellation.getSPPUsedSatellites());
            }
            if (this.BDS_SYSTEM == 1) {
                bdsConstellation.calculateSatPosition(gnssEphemericsNtrip, position);
                SPP_UsedSatellites.addAll(bdsConstellation.getSPPUsedSatellites());
            }
            if (this.QZSS_SYSTEM == 1) {
                qzssConstellation.calculateSatPosition(gnssEphemericsNtrip, position);
                SPP_UsedSatellites.addAll(qzssConstellation.getSPPUsedSatellites());
            }



        }
    }
}
