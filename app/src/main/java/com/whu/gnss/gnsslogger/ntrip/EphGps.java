/*
 * Copyright (c) 2010, Eugenio Realini, Mirko Reguzzoni, Cryms sagl - Switzerland. All Rights Reserved.
 *
 * This file is part of goGPS Project (goGPS).
 *
 * goGPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * goGPS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with goGPS.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.whu.gnss.gnsslogger.ntrip;


import com.whu.gnss.gnsslogger.constellations.Time;

/**
 * <p>
 * GPS broadcast ephemerides--同时适用于galileo beidou qzss
 * </p>
 *
 * @author Eugenio Realini, Cryms.com, Daisuke Yoshida 
 */

public class EphGps {
	private final static int STREAM_V = 1;

	private Time refTime; /* Reference time of the dataset */

    public Time getRefTime() {
        return refTime;
    }

    public void setRefTime(Time refTime) {
        this.refTime = refTime;
    }

    private char satType; /* Satellite Type */
	private int satID; /* Satellite ID number */
	private int week; /* GPS week number */

	private int L2Code; /* Code on L2 */
	private int L2Flag; /* L2 P data flag */

	private int svAccur; /* SV accuracy (URA index) */
	private int svHealth; /* SV health */

	private int iode; /* Issue of data (ephemeris) */
	private int iodc; /* Issue of data (clock) */

	private double toc; /* clock data reference time */
	private double toe; /* ephemeris reference time */
	private double tom; /* transmission time of message */

	/* satellite clock parameters */
	private double af0;
	private double af1;
	private double af2;
	private double tgd;

	/* satellite orbital parameters */
	private double rootA; /* Square root of the semimajor axis */
	private double e; /* Eccentricity */
	private double i0; /* Inclination angle at reference time */
	private double iDot; /* Rate of inclination angle */
	private double omg; /* Argument of perigee */
	private double omega0; /*
	 * Longitude of ascending node of orbit plane at beginning
	 * of week
	 */
	private double omegaDot; /* Rate of right ascension */
	private double M0; /* Mean anomaly at reference time */
	private double deltaN; /* Mean motion difference from computed value */
	private double crc, crs, cuc, cus, cic, cis; /*
	 * Amplitude of second-order harmonic
	 * perturbations
	 */
	private long fitInt; /* Fit interval */

	/* for BEIDOU data */

	//群延迟参数
	private double tgdB1C;
	private double tgdB2a;

	public double getTgdB1C() {
		return tgdB1C;
	}

	public void setTgdB1C(double tgdB1C) {
		this.tgdB1C = tgdB1C;
	}

	public double getTgdB2a() {
		return tgdB2a;
	}

	public void setTgdB2a(double tgdB2a) {
		this.tgdB2a = tgdB2a;
	}


	/*for GALILEO data*/
	private double tgdE5a_E1;
	private double tgdE5b_E1;

	public double getTgdE5a_E1() {
		return tgdE5a_E1;
	}

	public void setTgdE5a_E1(double tgdE5a_E1) {
		this.tgdE5a_E1 = tgdE5a_E1;
	}

	public double getTgdE5b_E1() {
		return tgdE5b_E1;
	}

	public void setTgdE5b_E1(double tgdE5b_E1) {
		this.tgdE5b_E1 = tgdE5b_E1;
	}

	/* for GLONASS data */
	private float tow;

	private double  tauN;
	private double  gammaN;
	private double tk;

	private double X;
	private double Xv;
	private double Xa;
	private double Bn;

	private double Y;
	private double Yv;
	private double Ya;
	private int freq_num;
	private double tb;

	private double Z;
	private double Zv;
	private double Za;
	private double En;

  public static final EphGps UnhealthyEph = new EphGps();


	public EphGps(){

	}


    /**
	 * @return the refTime
	 */

	/**
	 * @param refTime the refTime to set
	 */

	/**
	 * @return the satType
	 */
	public char getSatType() {
		return satType;
	}
	/**
	 * @param satType the satType to set
	 */
	public void setSatType(char satType) {
		this.satType = satType;
	}
	/**
	 * @return the satID
	 */
	public int getSatID() {
		return satID;
	}
	/**
	 * @param satID the satID to set
	 */
	public void setSatID(int satID) {
		this.satID = satID;
	}
	/**
	 * @return the week
	 */
	public int getWeek() {
		return week;
	}
	/**
	 * @param week the week to set
	 */
	public void setWeek(int week) {
		this.week = week;
	}
	/**
	 * @return the l2Code
	 */
	public int getL2Code() {
		return L2Code;
	}
	/**
	 * @param l2Code the l2Code to set
	 */
	public void setL2Code(int l2Code) {
		L2Code = l2Code;
	}
	/**
	 * @return the l2Flag
	 */
	public int getL2Flag() {
		return L2Flag;
	}
	/**
	 * @param l2Flag the l2Flag to set
	 */
	public void setL2Flag(int l2Flag) {
		L2Flag = l2Flag;
	}
	/**
	 * @return the svAccur
	 */
	public int getSvAccur() {
		return svAccur;
	}
	/**
	 * @param svAccur the svAccur to set
	 */
	public void setSvAccur(int svAccur) {
		this.svAccur = svAccur;
	}
	/**
	 * @return the svHealth
	 */
	public int getSvHealth() {
		return svHealth;
	}
	/**
	 * @param svHealth the svHealth to set
	 */
	public void setSvHealth(int svHealth) {
		this.svHealth = svHealth;
	}
	/**
	 * @return the iode
	 */
	public int getIode() {
		return iode;
	}
	/**
	 * @param iode the iode to set
	 */
	public void setIode(int iode) {
		this.iode = iode;
	}
	/**
	 * @return the iodc
	 */
	public int getIodc() {
		return iodc;
	}
	/**
	 * @param iodc the iodc to set
	 */
	public void setIodc(int iodc) {
		this.iodc = iodc;
	}
	/**
	 * @return the toc
	 */
	public double getToc() {
		return toc;
	}
	/**
	 * @param toc the toc to set
	 */
	public void setToc(double toc) {
		this.toc = toc;
	}
	/**
	 * @return the toe
	 */
	public double getToe() {
		return toe;
	}
	/**
	 * @param toe the toe to set
	 */
	public void setToe(double toe) {
		this.toe = toe;
	}
	/**
	 * @return the tom
	 */
	public double getTom() {
		return tom;
	}
	/**
	 * @param tom the tom to set
	 */
	public void setTom(double tom) {
		this.tom = tom;
	}
	/**
	 * @return the af0
	 */
	public double getAf0() {
		return af0;
	}
	/**
	 * @param af0 the af0 to set
	 */
	public void setAf0(double af0) {
		this.af0 = af0;
	}
	/**
	 * @return the af1
	 */
	public double getAf1() {
		return af1;
	}
	/**
	 * @param af1 the af1 to set
	 */
	public void setAf1(double af1) {
		this.af1 = af1;
	}
	/**
	 * @return the af2
	 */
	public double getAf2() {
		return af2;
	}
	/**
	 * @param af2 the af2 to set
	 */
	public void setAf2(double af2) {
		this.af2 = af2;
	}
	/**
	 * @return the tgd
	 */
	public double getTgd() {
		return tgd;
	}
	/**
	 * @param tgd the tgd to set
	 */
	public void setTgd(double tgd) {
		this.tgd = tgd;
	}
	/**
	 * @return the rootA
	 */
	public double getRootA() {
		return rootA;
	}
	/**
	 * @param rootA the rootA to set
	 */
	public void setRootA(double rootA) {
		this.rootA = rootA;
	}
	/**
	 * @return the e
	 */
	public double getE() {
		return e;
	}
	/**
	 * @param e the e to set
	 */
	public void setE(double e) {
		this.e = e;
	}
	/**
	 * @return the i0
	 */
	public double getI0() {
		return i0;
	}
	/**
	 * @param i0 the i0 to set
	 */
	public void setI0(double i0) {
		this.i0 = i0;
	}
	/**
	 * @return the iDot
	 */
	public double getiDot() {
		return iDot;
	}
	/**
	 * @param iDot the iDot to set
	 */
	public void setiDot(double iDot) {
		this.iDot = iDot;
	}
	/**
	 * @return the omega
	 */
	public double getOmg() {
		return omg;
	}
	/**
	 * @param omega the omega to set
	 */
	public void setOmg(double omega) {
		this.omg = omega;
	}
	/**
	 * @return the omega0
	 */
	public double getOmega0() {
		return omega0;
	}
	/**
	 * @param omega0 the omega0 to set
	 */
	public void setOmega0(double omega0) {
		this.omega0 = omega0;
	}
	/**
	 * @return the omegaDot
	 */
	public double getOmegaDot() {
		return omegaDot;
	}
	/**
	 * @param omegaDot the omegaDot to set
	 */
	public void setOmegaDot(double omegaDot) {
		this.omegaDot = omegaDot;
	}
	/**
	 * @return the m0
	 */
	public double getM0() {
		return M0;
	}
	/**
	 * @param m0 the m0 to set
	 */
	public void setM0(double m0) {
		M0 = m0;
	}
	/**
	 * @return the deltaN
	 */
	public double getDeltaN() {
		return deltaN;
	}
	/**
	 * @param deltaN the deltaN to set
	 */
	public void setDeltaN(double deltaN) {
		this.deltaN = deltaN;
	}
	/**
	 * @return the crc
	 */
	public double getCrc() {
		return crc;
	}
	/**
	 * @param crc the crc to set
	 */
	public void setCrc(double crc) {
		this.crc = crc;
	}
	/**
	 * @return the crs
	 */
	public double getCrs() {
		return crs;
	}
	/**
	 * @param crs the crs to set
	 */
	public void setCrs(double crs) {
		this.crs = crs;
	}
	/**
	 * @return the cuc
	 */
	public double getCuc() {
		return cuc;
	}
	/**
	 * @param cuc the cuc to set
	 */
	public void setCuc(double cuc) {
		this.cuc = cuc;
	}
	/**
	 * @return the cus
	 */
	public double getCus() {
		return cus;
	}
	/**
	 * @param cus the cus to set
	 */
	public void setCus(double cus) {
		this.cus = cus;
	}
	/**
	 * @return the cic
	 */
	public double getCic() {
		return cic;
	}
	/**
	 * @param cic the cic to set
	 */
	public void setCic(double cic) {
		this.cic = cic;
	}
	/**
	 * @return the cis
	 */
	public double getCis() {
		return cis;
	}
	/**
	 * @param cis the cis to set
	 */
	public void setCis(double cis) {
		this.cis = cis;
	}
	/**
	 * @return the fitInt
	 */
	public long getFitInt() {
		return fitInt;
	}
	/**
	 * @param fitInt the fitInt to set
	 */
	public void setFitInt(long fitInt) {
		this.fitInt = fitInt;
	}


	/* for GLONASS data */

	public double  getTauN() {
		return tauN;
	}
	public void setTauN(double  tauN) {
		this.tauN = tauN;
	}

	public double  getGammaN() {
		return gammaN;
	}
	public void setGammaN(double  gammaN) {
		this.gammaN = gammaN;
	}

	public double gettk() {
		return tk;
	}
	public void settk(double tk) {
		this.tk = tk;
	}

	public double getX() {
		return X;
	}
	public void setX(double X) {
		this.X = X;
	}

	public double getXv() {
		return Xv;
	}
	public void setXv(double Xv) {
		this.Xv = Xv;
	}

	public double getXa() {
		return Xa;
	}
	public void setXa(double Xa) {
		this.Xa = Xa;
	}

	public double getBn() {
		return Bn;
	}
	public void setBn(double Bn) {
		this.Bn = Bn;
	}

	public double getY() {
		return Y;
	}
	public void setY(double Y) {
		this.Y = Y;
	}

	public double getYv() {
		return Yv;
	}
	public void setYv(double Yv) {
		this.Yv = Yv;
	}

	public double getYa() {
		return Ya;
	}
	public void setYa(double Ya) {
		this.Ya = Ya;
	}

	public int getfreq_num() {
		return freq_num;
	}
	public void setfreq_num(int freq_num) {
		this.freq_num = freq_num;
	}

	public double gettb() {
		return tb;
	}
	public void settb(double tb) {
		this.tb = tb;
	}

	public double getZ() {
		return Z;
	}
	public void setZ(double Z) {
		this.Z = Z;
	}

	public double getZv() {
		return Zv;
	}
	public void setZv(double Zv) {
		this.Zv = Zv;
	}

	public double getZa() {
		return Za;
	}
	public void setZa(double Za) {
		this.Za = Za;
	}

	public double getEn() {
		return En;
	}
	public void setEn(double En) {
		this.En = En;
	}


}
