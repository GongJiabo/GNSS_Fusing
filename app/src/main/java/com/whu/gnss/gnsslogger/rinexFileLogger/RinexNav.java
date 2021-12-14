package com.whu.gnss.gnsslogger.rinexFileLogger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.whu.gnss.gnsslogger.Constants;
import com.whu.gnss.gnsslogger.constellations.GpsTime;
import com.whu.gnss.gnsslogger.GNSSConstants;
import com.whu.gnss.gnsslogger.nav.SQLiteManager;
import com.whu.gnss.gnsslogger.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 此类实现导航电文的写入文件
 * 白腾飞
 * 2020/4/1
 */
public class RinexNav {

    private static final String TAG = Rinex.class.getSimpleName();



    private FileWriter outNav = null;
    private Context context;
    private int ver;

    public RinexNav(Context context, int ver)
    {
        this.context=context;
        this.ver=ver;

        createFile();
    }
    private void createFile() {
        Date date = new Date();

        String dateString = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(date);
        String type = "n"; //Observable file
        int year = Integer.parseInt(new SimpleDateFormat("yy", Locale.US).format(date));
        String yearString;
        if (year - 10 < 0)
            yearString = "0" + year;
        else
            yearString = "" + year;
        String fileName = "NA" + dateString + ver + "." + yearString + type;

        try {
            File rootFile = new File(context.getFilesDir().getAbsolutePath(), context.getString(R.string.app_name) + "_Rinex");
            if (!rootFile.exists()) rootFile.mkdirs();

            File file = new File(rootFile, fileName);
            outNav = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "CreateFile, File name = " + fileName);
    }
    public void closeFile() {
        Log.i(TAG, "CloseFile");
        try {
            outNav.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("DefaultLocale")
    public void writeHeader(SQLiteManager sqLiteManager)
    {
        SQLiteDatabase NavDB=sqLiteManager.getWritableDatabase();

        Log.i(TAG, "writeHeader");
        //RINEX VERSION / TYPE
        String version="2.11";
        if(ver== Constants.VER_3_03) version="3.03";
        String type = "N: GNSS NAV DATA";
        String source = "G: GPS";
        try {
            outNav.write(String.format("     %-15s%-20s%-20sRINEX VERSION / TYPE",version,type,source));
            outNav.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //PGM / RUN BY / DATE
        String program = "GnssData";
        String agency = "Butterflying10";
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyyMMdd hhmmss", Locale.US);
        formatDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateCreation = formatDate.format(new Date()) + " UTC";
        try {
            outNav.write(String.format("%-20s%-20s%20s PGM / RUN BY / DATE",program,agency,dateCreation));
            outNav.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //查询IONOSPHERIC表中所有数据
        NavDB = sqLiteManager.getWritableDatabase();
        try {
            Cursor cursor = NavDB.query("IONOSPHERIC", null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    double GPSA0 = cursor.getDouble(cursor.getColumnIndex("GPSA0"));
                    double GPSA1 = cursor.getDouble(cursor.getColumnIndex("GPSA1"));
                    double GPSA2 = cursor.getDouble(cursor.getColumnIndex("GPSA2"));
                    double GPSA3 = cursor.getDouble(cursor.getColumnIndex("GPSA3"));
                    double GPSB0 = cursor.getDouble(cursor.getColumnIndex("GPSB0"));
                    double GPSB1 = cursor.getDouble(cursor.getColumnIndex("GPSB1"));
                    double GPSB2 = cursor.getDouble(cursor.getColumnIndex("GPSB2"));
                    double GPSB3 = cursor.getDouble(cursor.getColumnIndex("GPSB3"));

                    outNav.write(String.format("GPSA  %12.4E %12.4E %12.4E %12.4E       IONOSPHERIC CORR", GPSA0, GPSA1, GPSA2, GPSA3));
                    outNav.write('\n');
                    outNav.write(String.format("GPSB  %12.4E %12.4E %12.4E %12.4E       IONOSPHERIC CORR", GPSB0, GPSB1, GPSB2, GPSB3));
                    outNav.write('\n');

                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //查询UTC表中所有数据
        try {
            Cursor cursor1 = NavDB.query("UTC", null, null, null, null, null, null);

            if (cursor1.moveToFirst()) {
                do {
                    double a0UTC = cursor1.getDouble(cursor1.getColumnIndex("a0UTC"));
                    double a1UTC = cursor1.getDouble(cursor1.getColumnIndex("a1UTC"));

                    double tot = cursor1.getDouble(cursor1.getColumnIndex("tot"));
                    double wnt = cursor1.getDouble(cursor1.getColumnIndex("wnt"));

                    if (tot > 0 && wnt > 0) {
                        outNav.write(String.format("GPUT %19.12E%19.12E%9.0f%9.0f   TIME SYSTEM CORR", a0UTC, a1UTC, tot, wnt));
                        outNav.write('\n');
                    }

                } while (cursor1.moveToNext());
            }
            cursor1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //查询周跳表中的数据
        try {
            Cursor cursor2 = NavDB.query("LEAPSECOND", null, null, null, null, null, null);

            if (cursor2.moveToFirst()) {
                do {
                    double tls = cursor2.getDouble(cursor2.getColumnIndex("tls"));
                    double wnlsf = cursor2.getDouble(cursor2.getColumnIndex("wnlsf"));

                    double dn = cursor2.getDouble(cursor2.getColumnIndex("dn"));
                    double tlsf = cursor2.getDouble(cursor2.getColumnIndex("tlsf"));
                    outNav.write(String.format("    %6.0f%6.0f%6.0f%6.0f                                        LEAP SECONDS", tls, tlsf, wnlsf, dn));
                    outNav.write('\n');
                } while (cursor2.moveToNext());
            }
            cursor2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //结束标识符
        try {
            outNav.write("                                                                   END OF HEADER");
            outNav.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("DefaultLocale")
    public void writeBody(SQLiteManager sqLiteManager)
    {
        SQLiteDatabase NavDB=sqLiteManager.getWritableDatabase();
        Log.i(TAG, "writeBody");
        StringBuilder stringBuilder = new StringBuilder();

        NavDB = sqLiteManager.getWritableDatabase();

        //查询ephgpsBook表中所有数据
        Cursor cursor = NavDB.query("ephgpsBook", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                //遍历  cursor 取出数据并打印为stringbuilder
                int svid = cursor.getInt(cursor.getColumnIndex("Svid"));
                @SuppressLint("DefaultLocale") String prn=String.format("%02d", svid);
                if(ver==Constants.VER_3_03)prn="G"+prn;

                double Toc = cursor.getDouble(cursor.getColumnIndex("Toc"));

                double af0 = cursor.getDouble(cursor.getColumnIndex("af0"));

                double af1 = cursor.getDouble(cursor.getColumnIndex("af1"));

                double af2 = cursor.getDouble(cursor.getColumnIndex("af2"));

                double iode = cursor.getDouble(cursor.getColumnIndex("iode"));

                double Crs = cursor.getDouble(cursor.getColumnIndex("Crs"));

                double delta_n = cursor.getDouble(cursor.getColumnIndex("delta_n"));

                double M0 = cursor.getDouble(cursor.getColumnIndex("M0"));

                double Cuc = cursor.getDouble(cursor.getColumnIndex("Cuc"));

                double es = cursor.getDouble(cursor.getColumnIndex("es"));

                double Cus = cursor.getDouble(cursor.getColumnIndex("Cus"));

                double sqrtA = cursor.getDouble(cursor.getColumnIndex("sqrtA"));

                double Toe = cursor.getInt(cursor.getColumnIndex("Toe"));

                double Cic = cursor.getDouble(cursor.getColumnIndex("Cic"));

                double Omega_0 = cursor.getDouble(cursor.getColumnIndex("Omega_0"));

                double Cis = cursor.getDouble(cursor.getColumnIndex("Cis"));

                double i0 = cursor.getDouble(cursor.getColumnIndex("i0"));

                double Crc = cursor.getDouble(cursor.getColumnIndex("Crc"));

                double w = cursor.getDouble(cursor.getColumnIndex("w"));

                double Omega_dot = cursor.getDouble(cursor.getColumnIndex("Omega_dot"));


                double i_dot = cursor.getDouble(cursor.getColumnIndex("i_dot"));

                double L2code = cursor.getDouble(cursor.getColumnIndex("L2code"));


                double gpsweek = cursor.getDouble(cursor.getColumnIndex("gpsweek"));

                double L2Flag = cursor.getInt(cursor.getColumnIndex("L2Flag"));

                double svAccur = cursor.getInt(cursor.getColumnIndex("svAccur"));

                double svHealth = cursor.getInt(cursor.getColumnIndex("svHealth"));

                double iodc = cursor.getDouble(cursor.getColumnIndex("iodc"));

                double TGD = cursor.getDouble(cursor.getColumnIndex("TGD"));

                //计算从1980.1.6到此刻的纳秒,计算GPS时间
                //1024*2  是2048周    获取到的gpsweek 是总周数除以1024的余数。
                //没有考虑到数据库中一个卫星数据不全的问题
                double nanos = (gpsweek + 1024 * 2) * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK + Toc * 1e9;

                GpsTime gpsTime = new GpsTime(nanos);

                int year = gpsTime.getYear();
                int simplyyear=gpsTime.getYearSimplify();
                int month = gpsTime.getMonth();
                int day = gpsTime.getDay();
                int hour = gpsTime.getHour();
                int minute = gpsTime.getMinute();
                double second = gpsTime.getSecond();

                @SuppressLint("DefaultLocale") String time = String.format("%d %2d %2d %2d %2d %5.1f", simplyyear, month, day, hour, minute, second);

                if(ver==Constants.VER_3_03) time = String.format("%d %02d %02d %02d %02d %02.0f", year, month, day, hour, minute, second);


                stringBuilder.append(prn).append(" ");
                stringBuilder.append(time);
                @SuppressLint("DefaultLocale") String epoch_clock = String.format("%19.12E%19.12E%19.12E", af0, af1, af2);
                epoch_clock = epoch_clock.replace("E", "D");
                stringBuilder.append(epoch_clock);
                //按行
                stringBuilder.append('\n');

                //广播轨道1参数
                @SuppressLint("DefaultLocale") String broadcastorbit_1 = String.format("    %19.12E%19.12E%19.12E%19.12E", iode, Crs, delta_n, M0);
                broadcastorbit_1 = broadcastorbit_1.replace("E", "D");
                stringBuilder.append(broadcastorbit_1);
                stringBuilder.append('\n');

                //广播轨道2参数
                @SuppressLint("DefaultLocale") String broadcastorbit_2 = String.format("    %19.12E%19.12E%19.12E%19.12E", Cuc, es, Cus, sqrtA);
                broadcastorbit_2 = broadcastorbit_2.replace("E", "D");
                stringBuilder.append(broadcastorbit_2);
                stringBuilder.append('\n');

                //广播轨道3参数
                @SuppressLint("DefaultLocale") String broadcastorbit_3 = String.format("    %19.12E%19.12E%19.12E%19.12E", Toe, Cic, Omega_0, Cis);
                broadcastorbit_3 = broadcastorbit_3.replace("E", "D");
                stringBuilder.append(broadcastorbit_3);
                stringBuilder.append('\n');

                //广播轨道4参数
                @SuppressLint("DefaultLocale") String broadcastorbit_4 = String.format("    %19.12E%19.12E%19.12E%19.12E", i0, Crc, w, Omega_dot);
                broadcastorbit_4 = broadcastorbit_4.replace("E", "D");
                stringBuilder.append(broadcastorbit_4);
                stringBuilder.append('\n');

                //广播轨道5参数
                @SuppressLint("DefaultLocale") String broadcastorbit_5 = String.format("    %19.12E%19.12E%19.12E%19.12E", i_dot, L2code, gpsweek, L2Flag);
                broadcastorbit_5 = broadcastorbit_5.replace("E", "D");
                stringBuilder.append(broadcastorbit_5);
                stringBuilder.append('\n');

                //广播轨道6参数
                @SuppressLint("DefaultLocale") String broadcastorbit_6 = String.format("    %19.12E%19.12E%19.12E%19.12E", svAccur, svHealth, TGD, iodc);
                broadcastorbit_6 = broadcastorbit_6.replace("E", "D");
                stringBuilder.append(broadcastorbit_6);
                stringBuilder.append('\n');
                //广播轨道7参数
                stringBuilder.append('\n');
                stringBuilder.append('\n');

            } while (cursor.moveToNext());


        }
        cursor.close();

        try {
            outNav.write(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
