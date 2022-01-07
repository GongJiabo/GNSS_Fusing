package com.whu.gnss.gnsslogger;

import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Created by KuboLab on 2018/01/26.
 */

public class CoodinateConv {
    private double Lat;   //rad
    private double Long;  //rad
    private double Alt;//WGS84椭球面  m

    //度转为弧度表示
    private double PI = Math.PI;
    private static double API = Math.PI / 180.0;

    public static double[] WGS84LLAtoXYZ(double Lat, double Long, double Alt) {
        final double a = 6378137.0;
        final double f = 1 / 298.257223563;
        final double b = a * (1 - f);
        double e1 = Math.pow(a * a - b * b, 0.5) / a;
        double e2 = Math.pow(a * a - b * b, 0.5) / b;

        double Lat_rad=Lat*API;

        //子午线弧长计算步骤
        double m0=a*(1-Math.pow(e1,2.0));
        double m2 = 1.5 * Math.pow(e1, 2.0) * m0;
        double m4 = 1.25 * Math.pow(e1, 2.0) * m2;
        double m6 = 7.0 / 6.0 * Math.pow(e1, 2.0) * m4;
        double m8 = 9.0 / 8.0 * Math.pow(e1, 2.0) * m6;

        double a0 = m0 + m2 / 2.0 + 3.0 / 8.0 * m4 + 5.0 / 16.0 * m6 + 35.0 / 128.0 * m8;
        double a2 = m2 / 2.0 + m4 / 2.0 + 15.0 / 32.0 * m6 + 7.0 / 16.0 * m8;
        double a4 = m4 / 8.0 + 3.0 / 16.0 * m6 + 7.0 / 32.0 * m8;
        double a6 = m6 / 32.0 + m8 / 16.0;
        double a8 = m8 / 128.0;
        double X = a0 * Lat_rad - a2 / 2.0 * Math.sin(2.0 * Lat_rad) + a4 / 4.0 * Math.sin(4.0 * Lat_rad) - a6 / 6.0 * Math.sin(6.0 * Lat_rad) + a8 / 8.0 * Math.sin(8.0 * Lat_rad);


        //获取三度带的中心子午线
        int n=(int)Math.round(Long/3);
        double Long0=3*n;

        double l=(Long-Long0)*API;


        double t = Math.tan(Lat_rad);
        double η = e2 * Math.cos(Lat_rad);
        double N = a / Math.sqrt(1 - e1 *e1* Math.sin(Lat_rad) * Math.sin(Lat_rad));    //卯酉圈半径长度

//        double x=X+N/2*Math.sin(Lat_rad)*Math.cos(Lat_rad)*Math.pow(l,2)+N/24*Math.sin(Lat_rad)*Math.pow(Math.cos(Lat_rad),3)*(5-Math.pow(t,2)+Math.pow(η,2)*9+4*Math.pow(η,4))*Math.pow(l,4)+N/720*Math.sin(Lat_rad)*Math.pow(Math.cos(Lat_rad),5)*(61-58*Math.pow(t,2)+Math.pow(t,4)+270*Math.pow(η,2)-330*Math.pow(η,2)*Math.pow(t,2))*Math.pow(l,6);
//
//        double y = N * l * Math.cos(Lat_rad) + N / 6 * Math.pow(Math.cos(Lat_rad), 3) * (1 - Math.pow(t, 2) + Math.pow(η, 2)) * Math.pow(l, 3) + N / 120 * Math.pow(Math.cos(Lat_rad), 5) * (5 - 18 * Math.pow(t, 2) + Math.pow(t, 4) + 14 * Math.pow(η, 2) - 58 * Math.pow(η, 2) * Math.pow(t, 2)) * Math.pow(l, 5);

        double x=(N+Alt)*Math.cos(Lat_rad)*Math.cos(Long*API);
        double y=(N+Alt)*Math.cos(Lat_rad)*Math.sin(Long*API);
        double z=(N*(1-e1*e1)+Alt)*Math.sin(Lat_rad);
        double[] XYZ=new double[]{x,y,z};
        return XYZ;
    }

    public static double[] enu2xyz(double enu[], double orgxyz[]){
        //[Argin]
        //enu   : [e; n; u] ENU座標[m]
        //orgxyz: [x; y; z] ENU座標系の原点のXYZ座標[m]

        // [Argout]
        // posxyz: [x; y; z] ECEF座標系でのXYZ座標[m]

        // [Description]
        // orgxyzを原点とするENU座標系の座標enuを，ECEF座標系（WGS-84）の座標posxyzに変換します．
        double[] orgllh = xyz2llh(orgxyz,0);
        double phi = orgllh[0];
        double lam = orgllh[1];

        double cosphi = Math.cos(phi);
        double coslam = Math.cos(lam);
        double sinphi = Math.sin(phi);
        double sinlam = Math.sin(lam);
        RealMatrix Ti = MatrixUtils.createRealMatrix(new double[][] {{-sinlam, -sinphi*coslam, cosphi*coslam},{coslam, -sinphi*sinlam, cosphi*sinlam},{0, cosphi, sinphi}});
        RealMatrix matenu = MatrixUtils.createRealMatrix(new double[][]{{enu[0],enu[1],enu[2]}});
        RealMatrix matorgxyz = MatrixUtils.createRealMatrix(new double[][]{{orgxyz[0],orgxyz[1],orgxyz[2]}});

        RealMatrix matposxyz = (Ti.multiply(matenu)).add(matorgxyz);
        double[] posxyz = new double[]{matposxyz.getEntry(1,1),matposxyz.getEntry(1,2),matposxyz.getEntry(1,3)};
        return posxyz;

    }

    public static double[] xyz2llh(double posxyz[], int method){
        //[Argin]
        //posxyz: [x; y; z] XYZ座標[m]
        //method: 変換アルゴリズム指定子(Bowring,Bowring2,Iteration,Heikkinen)

        // [Argout]
        // posllh   : [lat; lon; h] lat: 緯度[rad], lon: 経度[rad]，h: 楕円体高[m]

        // [Description]
        // ECEF座標系（WGS-84座標系）の座標posxyzを測地座標（緯度，経度，楕円体高）に変換します．
        // 変換アルゴリズムをmethodにより指定することができます．methodを省略した場合はBowringが
        // 使用されます．
        double posllh[] = new double[3];
        double x = posxyz[0];
        double y = posxyz[1];
        double z = posxyz[2];
        double a = 6378137.0;
        double f = 1/298.257223563;
        double b = a*(1-f);
        double e2 = f*(2-f);
        double m2 = 1/(1-e2) - 1;
        double p = sqrt(Math.pow(x,2) + Math.pow(y,2));
        switch (method){
            case 0:
                double theta = Math.atan(z*a/(p*b));
                double phi = Math.atan((Math.pow(z+m2*b* sin(theta),3))/(Math.pow((p-e2*a*Math.cos(theta)),3)));
                double N = a/sqrt(Math.pow(1-e2* sin(phi),2));
                double h = p/Math.cos(phi)-N;
                posllh[0] = phi;
                posllh[1] = Math.atan2(y,x);
                posllh[2] = h;
        }
        return posllh;
    }
}
