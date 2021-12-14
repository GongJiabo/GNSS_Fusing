package  com.whu.gnss.gnsslogger.constellations.satellites;

/**
 * 伽利略卫星数据
 * 2020/3/15
 * butterfly ing 10
 */

 /**
 * rinex2.11   L1    C1   S1  D1   L5   C5   S5   D5
 * rinex3.03  L1C  C1C  S1C D1C  L5Q  C5Q  S5Q  D5Q
 */

public class GalileoSatellite {

     /**
      * 卫星prn
      */
     private String prn;

     public void setPrn(String prn) {
         this.prn = prn;
     }

     public String getPrn() {
         return prn;
     }

     /**
      * 是否有双频数据
      */
     private boolean hasC1;

     public boolean isHasC1() {
         if (this.getC1() != 0) {
             return true;
         } else {
             return false;
         }
     }
     private boolean isHasC5;

     public boolean isHasC5() {
         if (this.getC5() != 0) {
             return true;
         } else {
             return false;
         }
     }
     /**
      * L1,L5载波相位观测值
      */
     private double L1;
     private double L5;

     public double getL1() {
         return L1;
     }

     public void setL1(double l1) {
         L1 = l1;
     }

     public double getL5() {
         return L5;
     }

     public void setL5(double l5) {
         L5 = l5;
     }
     /**
      * L1,L5伪距观测值
      */
     private double C1;
     private double C5;

     public void setC1(double c1) {
         C1 = c1;
     }

     public double getC1() {
         return C1;
     }

     public void setC5(double c5) {
         C5 = c5;
     }

     public double getC5() {
         return C5;
     }

     /**
      * 多普勒观测值
      */
     private double D1;
     private double D5;

     public void setD1(double d1) {
         D1 = d1;
     }

     public double getD1() {
         return D1;
     }

     public void setD5(double d5) {
         D5 = d5;
     }

     public double getD5() {
         return D5;
     }
     /**
      * 信噪比,snr
      */
     private double S1;
     private double S5;

     public void setS1(double s1) {
         S1 = s1;
     }

     public double getS1() {
         return S1;
     }

     public void setS5(double s5) {
         S5 = s5;
     }

     public double getS5() {
         return S5;
     }



}
