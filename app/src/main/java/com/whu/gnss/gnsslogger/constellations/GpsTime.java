package  com.whu.gnss.gnsslogger.constellations;

import android.annotation.SuppressLint;
import android.location.GnssClock;

import com.whu.gnss.gnsslogger.GNSSConstants;


/**
 * gpstime的类，获取gpstime的年月日时分秒  gps周  和周内秒;;;gpstime  没有跳秒
 * 2020/3/16
 * butterflying10
 */
public class GpsTime {

    private int year;

    public void setYear(int year) {
        this.year = year;
    }

    public int getYear() {
        return year;
    }

    private int month;

    public void setMonth(int month) {
        this.month = month;
    }

    public int getMonth() {
        return month;
    }

    private int day;

    public void setDay(int day) {
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    private int hour;

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getHour() {
        return hour;
    }

    private int minute;

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getMinute() {
        return minute;
    }

    private double second;

    public void setSecond(double second) {
        this.second = second;
    }

    public double getSecond() {
        return second;
    }

    /*
            gps周
            */
    private int weeknumber;
    /*
    周内秒
     */
    private double weeksec;

    /**
     * 自1980.1.6起算的纳秒时间,这个时间可以用gnssclock接口得到
     */
    private double nsec;


    public GpsTime(double nsec) {
        this.nsec = nsec;
        calculateGpsTime();
    }

    public GpsTime(GnssClock gnssClock) {
        long TimeNanos = gnssClock.getTimeNanos();
        long FullBiasNanos = gnssClock.getFullBiasNanos();
        double BiasNanos = gnssClock.getBiasNanos();

        this.nsec = TimeNanos - (FullBiasNanos + BiasNanos);
        calculateGpsTime();

    }

    /**
     * 年的简化，如2020 简化为20   2019 简化为19
     */

    private int yearSimplify;

    public int getYearSimplify() {
        return Integer.parseInt(String.valueOf(this.year).substring(2));
    }

    /**
     * 年积日
     */
    private int DOY;

    public int getDOY() {
        return DOY;
    }

    private void setDOY(int DOY) {
        this.DOY = DOY;
    }

    private char HourLabel;

    public char getHourLabel(int hour) {
        switch (hour) {
            case 1:
                return 'A';
            case 2:
                return 'B';
            case 3:
                return 'C';
            case 4:
                return 'D';
            case 5:
                return 'E';
            case 6:
                return 'F';
            case 7:
                return 'G';
            case 8:
                return 'H';
            case 9:
                return 'I';
            case 10:
                return 'J';
            case 11:
                return 'K';
            case 12:
                return 'L';
            case 13:
                return 'M';
            case 14:
                return 'N';
            case 15:
                return 'O';
            case 16:
                return 'P';
            case 17:
                return 'Q';
            case 18:
                return 'R';
            case 19:
                return 'S';
            case 20:
                return 'T';
            case 21:
                return 'U';
            case 22:
                return 'V';
            case 23:
                return 'W';
            case 24:
                return 'X';
        }
        return (char) hour;

    }

    public int getWeeknumber() {


        return (int) Math.floor(this.nsec / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK);
    }

    public double getWeeksec() {
        return this.nsec % GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK / 1000000000.0;
    }

    private double getSumsec() {
        return this.nsec / 1000000000.0;
    }


    private void calculateGpsTime() {
        int year = 0, month = 0, hour = 0, day = 0, minute = 0;
        double second = 0;
        //获取gps时间的总秒数
        double sumsec = getSumsec();
        //从一月6日0时算起，前面有6天，先加上
        sumsec = sumsec + 6 * 24 * 60 * 60;

        //总共有多少天
        int dayNum = (int) Math.floor(sumsec / (3600.0 * 24));
        //剩余的小天
        double minday = (sumsec / (3600.0 * 24) - dayNum);


        for (int i = 1980; ; i++) {
            if ((i % 4 == 0 && i % 100 != 0) || i % 400 == 0) {
                dayNum = dayNum - 366;
            } else {
                dayNum = dayNum - 365;
            }
            if (dayNum < 365) {
                year = i + 1;
                break;
            }
        }
        this.setDOY(dayNum);


        for (int i = 1; ; i++) {
            if (i == 1 || i == 3 || i == 5 || i == 7 || i == 8 || i == 10 || i == 12) {

                if (dayNum <= 31) {
                    month = i;
                    break;
                }
                dayNum = dayNum - 31;
            } else if (i == 4 || i == 6 || i == 9 || i == 11) {

                if (dayNum <= 30) {
                    month = i ;
                    break;
                }
                dayNum = dayNum - 30;
            } else {
                if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {

                    if (dayNum <= 29) {
                        month = i ;
                        break;
                    }
                    dayNum = dayNum - 29;

                } else {

                    if (dayNum <= 28) {
                        month = i ;
                        break;
                    }
                    dayNum = dayNum - 28;
                }

            }
//            if(dayNum<30)
//            {
//                month=i+1;
//                break;
//            }
        }
        day = (int) Math.floor(dayNum);
        hour = (int) Math.floor(minday * 24);
        minday = minday * 24 - hour;
        minute = (int) Math.floor(minday * 60);
        minday = minday * 60 - minute;
        second = minday * 60;


        this.setYear(year);
        this.setMonth(month);
        this.setDay(day);
        this.setHour(hour);
        this.setMinute(minute);
        this.setSecond(second);
    }


    private String GpsTimeString;


    @SuppressLint("DefaultLocale")
    public String getGpsTimeString() {
        return GpsTimeString = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + String.format("%.2f", second);

    }

    @SuppressLint("DefaultLocale")
    public String getGpsTimeStringForFileDate() {
        return GpsTimeString = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + String.format("%.0f", second);

    }
}
