package  com.whu.gnss.gnsslogger.constellations.satellites;

/**
 * bds卫星数据
 * 2020/3/15
 * butterflying10
 */

/**
 * rinex2.11 不存在   rinex3.03  C2I  L2I   S2I   D2I
 */
public class BdsSatellite {


    private String prn;

    public String getPrn() {
        return prn;
    }

    public void setPrn(String prn) {
        this.prn = prn;
    }


    private double C2I;

    public double getC2I() {
        return C2I;
    }

    public void setC2I(double c2I) {
        C2I = c2I;
    }

    private double L2I;

    public double getL2I() {
        return L2I;
    }

    public void setL2I(double l2I) {
        L2I = l2I;
    }

    private double S2I;

    public void setS2I(double s2I) {
        S2I = s2I;
    }

    public double getS2I() {
        return S2I;
    }

    private double D2I;

    public void setD2I(double d2I) {
        D2I = d2I;
    }

    public double getD2I() {
        return D2I;
    }

}
