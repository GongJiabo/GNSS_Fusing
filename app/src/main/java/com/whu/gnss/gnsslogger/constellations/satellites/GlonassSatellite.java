package  com.whu.gnss.gnsslogger.constellations.satellites;

/**
 * glonass卫星数据  R1
 * 2020/3/16
 * butterflying10
 */

/**
 *    rinex2.11  C1   L1    S1    D1
 *    rinex3.03  C1C  L1C   S1C   D1C
 *
 */
public class GlonassSatellite {



    private String prn;

    public String getPrn() {
        return prn;
    }

    public void setPrn(String prn) {
        this.prn = prn;
    }
    private double C1C;

    public double getC1C() {
        return C1C;
    }

    public void setC1C(double c1C) {
        C1C = c1C;
    }
    private double L1C;

    public double getL1C() {
        return L1C;
    }

    public void setL1C(double l1C) {
        L1C = l1C;
    }
    private double S1C;

    public void setS1C(double s1C) {
        S1C = s1C;
    }

    public double getS1C() {
        return S1C;
    }
    private double D1C;

    public void setD1C(double d1C) {
        D1C = d1C;
    }

    public double getD1C() {
        return D1C;
    }

}
