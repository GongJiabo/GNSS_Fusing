package com.whu.gnss.gnsslogger.adjust;

import android.content.Context;
import android.util.Log;

import com.whu.gnss.gnsslogger.constellations.Time;
import com.whu.gnss.gnsslogger.R;
import com.whu.gnss.gnsslogger.coordinates.Coordinates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SPP_Result {

    private static final String TAG = SPP_Result.class.getSimpleName();


    private FileWriter out = null;

    private Context context;

    public SPP_Result(Context context) {
        this.context = context;
        createFile();
    }

    private void createFile() {
        Date date = new Date();

        String dateString = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(date);
        String type = "p"; //spp_result file
        int year = Integer.parseInt(new SimpleDateFormat("yy", Locale.US).format(date));
        String yearString;
        if (year - 10 < 0)
            yearString = "0" + year;
        else
            yearString = "" + year;
        String fileName = "SP" + dateString + 1 + "." + yearString + type;

        try {
            File rootFile = new File(context.getFilesDir().getAbsolutePath(), context.getString(R.string.app_name) + "_Rinex");
            if (!rootFile.exists()) rootFile.mkdirs();

            File file = new File(rootFile, fileName);
            out = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "CreateFile, File name = " + fileName);
    }

    public void closeFile() {
        Log.i(TAG, "CloseFile");
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //接收机近似位置---这里是第一次初始化的接收机近似位置
    public void writeHeader(Coordinates Rxpose) {
        try {
            out.write("第一列-GPS周");
            out.write("\n");
            out.write("第二列-GPS周内秒");
            out.write("\n");
            out.write("第三列-X/第四列-Y/第五列-Z");
            out.write("\n");
            out.write("接收机近似位置：" + Rxpose.getX() + " " + Rxpose.getY() + " " + Rxpose.getZ());
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeInfor(int spp_model, int GPS, int GAL, int GLO, int BDS, int QZSS) {
        try {
            if (spp_model == 0)
                out.write("SPP_MODEL" + "伪距单点定位");
            if (spp_model == 1)
                out.write("SPP_MODEL" + "伪距差分定位");
            if (spp_model == 2)
                out.write("SPP_MODEL" + "伪距单点/差分定位");
            out.write("\n");
            out.write("参与运算的卫星系统：");
            out.write("\n");
            if(GPS==1)
                out.write("GPS");
            if(GAL==1)
                out.write("GAL");
            if(GLO==1)
                out.write("GLO");
            if(BDS==1)
                out.write("BDS");
            if(QZSS==1)
                out.write("QZSS");
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void writeBody(Coordinates Rxpose_result, Time time)
    {
        try {
            out.write(time.getGpsWeek()+" "+time.getGpsWeekSec()+" "+Rxpose_result.getX()+" "+Rxpose_result.getY()+" "+Rxpose_result.getZ());
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
