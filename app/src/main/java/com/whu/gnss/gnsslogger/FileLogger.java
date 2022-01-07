package com.whu.gnss.gnsslogger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import android.os.SystemClock;


import com.whu.gnss.gnsslogger.constellations.GnssConstellation;
import com.whu.gnss.gnsslogger.constellations.GpsTime;
import com.whu.gnss.gnsslogger.constellations.satellites.EpochMeasurement;
import com.whu.gnss.gnsslogger.constellations.satellites.GalileoSatellite;
import com.whu.gnss.gnsslogger.constellations.satellites.GpsSatellite;
import com.whu.gnss.gnsslogger.constellations.satellites.QzssSatellite;
import com.whu.gnss.gnsslogger.nav.GpsNavigationConv;
import com.whu.gnss.gnsslogger.ntrip.GNSSEphemericsNtrip;
import com.whu.gnss.gnsslogger.ntrip.RTCM3Client;
import com.whu.gnss.gnsslogger.ntrip.RTCM3ClientListener;
import com.whu.gnss.gnsslogger.rinexFileLogger.Rinex;
import com.whu.gnss.gnsslogger.rinexFileLogger.RinexHeader;
import com.whu.gnss.gnsslogger.rinexFileLogger.RinexNav;
import com.whu.gnss.gnsslogger.adjust.SPP_Result;
import com.whu.gnss.gnsslogger.adjust.WeightedLeastSquares;
import com.whu.gnss.gnsslogger.coordinates.Coordinates;
import com.whu.gnss.gnsslogger.LoggerFragment.UIFragmentComponent;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;

//
public class FileLogger implements GnssListener
{

    private static final String TAG = "FileLogger";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final String VERSION_TAG = "Version: ";

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;

    private final Object mFileLock = new Object();          // .o
    private final Object mFileSubLock = new Object();       // .kml
    private final Object mFileAccAzLock = new Object();     // .csv
    private final Object mFileNmeaLock = new Object();      // .nmea
    private final Object mFileNavLock = new Object();       // .nav
    private final Object mFileRawLock = new Object();       // Gong added: .txt

    private BufferedWriter mFileWriter;         // .o
    private FileWriter mObsFileWriter;
    private BufferedWriter mFileNavWriter;      // .nav
    private FileWriter mNavFileWriter;

    private BufferedWriter mFileSubWriter;      // .kml
    private BufferedWriter mFileAccAzWriter;    // .csv
    private BufferedWriter mFileNmeaWriter;     // .nmea
    private BufferedWriter mFileRawWriter;      // Gong added: .txt

    private File mFile;
    private File mFileNav;
    private File mFileSub;
    private File mFileAccAzi;
    private File mFileNmea;
    private File mFileRaw; // Gong added

    private UIFragmentComponent mUiComponent;
    //private LoggerFragment mloggerFragment;

    private boolean notenoughsat = false;
    private boolean firstOBSforAcc = true;

    private ArrayList<Integer> UsedInFixList = new ArrayList<Integer>();
    private ArrayList<String> utckml = new ArrayList<String>();
    private ArrayList<String> gpstkml = new ArrayList<String>();
    private ArrayList<Double> longitudekml = new ArrayList<Double>();
    private ArrayList<Double> latitudekml = new ArrayList<Double>();
    private ArrayList<Double> altitudekml = new ArrayList<Double>();

    private int gnsstimeclock_a;
    private int gnsstimeclock_b;
    private int gnsstimeclock_c;
    private double gnsstimeclock_d;
    private int gnsstimeclock_e;
    private int gnsstimeclock_f;

    private boolean RINEX_NAV_ION_OK = false;

    private double[] CURRENT_SMOOTHER_RATE = new double[300];
    private double[] LAST_DELTARANGE = new double[300];
    private double[] LAST_SMOOTHED_PSEUDORANGE = new double[300];
    private double SMOOTHER_RATE = 0.01;
    private boolean initialize = false;

    private double constFullBiasNanos = 0.0;
    private double approximateX = 0.0;
    private double approximateY = 0.0;
    private double approximateZ = 0.0;

    // 区间变量
    private int localintervaltime = 1;

    // Gong added:
    private LocationManager mLocationManager;
    private Location mLocation;

    //观测值数据类
    private GnssConstellation sumConstellation;
    //导航电文类
    private GpsNavigationConv gpsNavigationConv;
    //rinex观测值文件的获取
    private Rinex rinex;
    private RinexNav rinexNav;
    //主要用来获取setting的信息，或者没有加载setting之后，获取之前初始化的信息，在constants里面有定义
    private SharedPreferences sharedPreferences;
    private SharedPreferences sharedPreferences_spp;
    //GPS时间
    private GpsTime gpsTime;
    // 接收机位置的初始化
    private boolean poseinitialized = false;
    //参与运算的广播星历系统
    private GNSSEphemericsNtrip mGNSSEphemericsNtrip;
    // 计算接收机位置
    private Coordinates pose;
    //平差
    private WeightedLeastSquares mWeightedLeastSquares;
    //平差结果输出
    private SPP_Result mSPP_result;


    public synchronized UIFragmentComponent getUiComponent()
    {
        return mUiComponent;
    }

    public synchronized void setUiComponent(UIFragmentComponent value)
    {
        mUiComponent = value;
    }

    public FileLogger(Context context)
    {
        //这个是获取头文件信息(context为主活动的上下文)
        sharedPreferences = context.getSharedPreferences(Constants.RINEX_SETTING, 0);
        //这个是获取SPP设置的信息
        sharedPreferences_spp = context.getSharedPreferences(Constants.SPP_SETTING, 0);

        //初始化观测值数据观测文件
        sumConstellation = new GnssConstellation(0, 0, 0, 0, 0);
        //初始化导航电文类
        gpsNavigationConv = new GpsNavigationConv(context);

        this.mContext = context;
        if (initialize == false)
        {
            Arrays.fill(LAST_DELTARANGE, 0.0);
            Arrays.fill(CURRENT_SMOOTHER_RATE, 1.0);
            Arrays.fill(LAST_SMOOTHED_PSEUDORANGE, 0.0);
            initialize = true;
            Log.d("FileLogger", "Initialize complete");
        }
    }

    /**
     * Start a new file logging process.
     */
    public void startNewLog()
    {
        // Gong added:
        if (SettingsFragment.FILE_NAME == "AndroidOBS")
        {
            Calendar myCal = Calendar.getInstance(Locale.CHINA);
            DateFormat myFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
            String myTime = myFormat.format(myCal.getTime());
            SettingsFragment.FILE_NAME = myTime;
        }

        // 初始化解算时用到的观测数据类
        sumConstellation = new GnssConstellation(sharedPreferences_spp.getInt(Constants.KEY_GPS_SYSTEM, Constants.DEF_GPS_SYSTEM), sharedPreferences_spp.getInt(Constants.KEY_GAL_SYSTEM, Constants.DEF_GAL_SYSTEM), sharedPreferences_spp.getInt(Constants.KEY_GLO_SYSTEM, Constants.DEF_GLO_SYSTEM), sharedPreferences_spp.getInt(Constants.KEY_BDS_SYSTEM, Constants.DEF_BDS_SYSTEM), sharedPreferences_spp.getInt(Constants.KEY_QZSS_SYSTEM, Constants.DEF_QZSS_SYSTEM));

        // rinex的o文件
        synchronized (mFileLock)
        {
            File baseDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state))
            {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            {
                logError("Cannot write to external storage.");
                return;
            } else
            {
                logError("Cannot read external storage.");
                return;
            }

            Date now = new Date();
            int observation = now.getYear() - 100;
            String fileName = String.format(SettingsFragment.FILE_NAME + "." + observation + "o", SettingsFragment.FILE_PREFIX);
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;

            try
            {
                mObsFileWriter = new FileWriter(currentFile);
                currentFileWriter = new BufferedWriter(mObsFileWriter);
            } catch (IOException e)
            {
                logException("Could not open observation file: " + currentFilePath, e);
                return;
            }

            // initialize the contents of the file
            try
            {
                // Gong added: 如果gpsTime为空，使用当地时
                // TODO: 无法使用onGnssMeasurementsReceived中收到的clock作为文件头???
                if (gpsTime == null)
                {
                    Toast.makeText(mContext, "Do not receive GNSS-CLOCK from Filelooger!", Toast.LENGTH_SHORT).show();
                    gpsTime = new GpsTime(0);
                    Calendar calendars = Calendar.getInstance(Locale.CHINA);
                    gpsTime.setYear(calendars.get(Calendar.YEAR));
                    gpsTime.setMonth(calendars.get(Calendar.MONTH));
                    gpsTime.setDay(calendars.get(Calendar.DAY_OF_MONTH));
                    gpsTime.setHour(calendars.get(Calendar.HOUR_OF_DAY));
                    gpsTime.setMinute(calendars.get(Calendar.MINUTE));
                    gpsTime.setSecond(calendars.get(Calendar.SECOND));
                }

                // 开始记录rinex观测文件
                startRecordRinex();

                localintervaltime = SettingsFragment.interval;

            } catch (Exception e)
            {
                Toast.makeText(mContext, "Count not initialize observation file", Toast.LENGTH_SHORT).show();
                logException("Count not initialize file: " + currentFilePath, e);
                return;
            }

            if (mFileWriter != null)
            {
                try
                {
                    mFileWriter.close();
                } catch (IOException e)
                {
                    logException("Unable to close all file streams.", e);
                    return;
                }
            }

            mFile = currentFile;
            mFileWriter = currentFileWriter;
            Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();

            // To make sure that files do not fill up the external storage:
            // - Remove all empty files
            FileFilter filter = new FileToDeleteFilter(mFile);
            for (File existingFile : baseDirectory.listFiles(filter))
            {
                existingFile.delete();
            }

            // - Trim the number of files with data
            File[] existingFiles = baseDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0)
            {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i)
                {
                    existingFiles[i].delete();
                }
            }
        }

        // rinex的n文件
        synchronized (mFileNavLock)
        {
            if (SettingsFragment.ENABLE_RINEXNAVLOG)
            {
                File baseNavDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state))
                {
                    baseNavDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXNAV);
                    baseNavDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
                {
                    logError("Cannot write to external storage.");
                    return;
                } else
                {
                    logError("Cannot read external storage.");
                    return;
                }

                Date now = new Date();
                int observation = now.getYear() - 100;
                String fileNameNav = String.format(SettingsFragment.FILE_NAME + "." + observation + "n", SettingsFragment.FILE_PREFIXNAV);
                File currentFileNav = new File(baseNavDirectory, fileNameNav);
                String currentFileNavPath = currentFileNav.getAbsolutePath();
                BufferedWriter currentFileNavWriter;
                try
                {
                    mNavFileWriter = new FileWriter(currentFileNav);
                    currentFileNavWriter = new BufferedWriter(mNavFileWriter);
                } catch (IOException e)
                {
                    logException("Could not open NAVIGATION file: " + currentFileNav, e);
                    return;
                }
                try
                {
                    // 开始记录rinex星历文件
                    startRecordRinexNav();

                } catch (Exception e)
                {
                    Toast.makeText(mContext, "Count not initialize observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize file: " + currentFileNavPath, e);
                    return;
                }

                if (mFileNavWriter != null)
                {
                    try
                    {
                        mFileNavWriter.close();
                    } catch (IOException e)
                    {
                        logException("Unable to close Nav file streams.", e);
                        return;
                    }
                }
                mFileNav = currentFileNav;
                mFileNavWriter = currentFileNavWriter;
                Toast.makeText(mContext, "File opened: " + currentFileNavPath, Toast.LENGTH_SHORT).show();

                // To make sure that files do not fill up the external storage:
                // - Remove all empty files
                FileFilter filter = new FileToDeleteFilter(mFileNav);
                for (File existingFile : baseNavDirectory.listFiles(filter))
                {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseNavDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0)
                {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i)
                    {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        // .kml文件
        synchronized (mFileSubLock)
        {
            if (SettingsFragment.ENABLE_KMLLOG)
            {
                File baseSubDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state))
                {
                    baseSubDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXSUB);
                    baseSubDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
                {
                    logError("Cannot write to external storage.");
                    return;
                } else
                {
                    logError("Cannot read external storage.");
                    return;
                }
                Date currentTime = Calendar.getInstance().getTime();
                Date now = new Date();
                int observation = now.getYear() - 100;
                String fileNameSub = String.format(SettingsFragment.FILE_NAME + ".kml", SettingsFragment.FILE_PREFIXSUB);
                File currentFileSub = new File(baseSubDirectory, fileNameSub);
                String currentFileSubPath = currentFileSub.getAbsolutePath();
                BufferedWriter currentFileSubWriter;
                try
                {
                    currentFileSubWriter = new BufferedWriter(new FileWriter(currentFileSub));
                } catch (IOException e)
                {
                    logException("Could not open subobservation file: " + currentFileSubPath, e);
                    return;
                }
                // 副观测文件的标题开头
                try
                {
                    currentFileSubWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("  <Document>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("    <name>" + Build.MODEL + ".kml</name>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("    <Placemark>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("      <Snippet maxLines=\"0\"> </Snippet>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("      <description> </description>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("      <name>Line 1</name>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("      <Style>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("        <LineStyle>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("          <color>80ffffff</color>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("        </LineStyle>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("      </Style>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("    <LineString>");
                    currentFileSubWriter.newLine();
                    currentFileSubWriter.write("      <coordinates>");
                    currentFileSubWriter.newLine();
                } catch (IOException e)
                {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize subobservation file: " + currentFileSubPath, e);
                    return;
                }

                if (mFileSubWriter != null)
                {
                    try
                    {
                        mFileSubWriter.close();
                    } catch (IOException e)
                    {
                        logException("Unable to close sub observation file streams.", e);
                        return;
                    }
                }
                mFileSub = currentFileSub;
                mFileSubWriter = currentFileSubWriter;
                Toast.makeText(mContext, "File opened: " + currentFileSubPath, Toast.LENGTH_SHORT).show();

                // To make sure that files do not fill up the external storage:
                // - Remove all empty files
                FileFilter filter = new FileToDeleteFilter(mFileSub);
                for (File existingFile : baseSubDirectory.listFiles(filter))
                {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseSubDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0)
                {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i)
                    {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        // .csv传感器文件
        synchronized (mFileAccAzLock)
        {
            if (SettingsFragment.ENABLE_SENSORSLOG)
            {
                File baseAccAziDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state))
                {
                    baseAccAziDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXACCAZI);
                    baseAccAziDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
                {
                    logError("Cannot write to external storage.");
                    return;
                } else
                {
                    logError("Cannot read external storage.");
                    return;
                }

                // csv文件开头File类中提供的getAbsolutePath可以通过绝对路径获取文件的位置和名称
                Date now = new Date();
                String fileNameAccAzi = String.format(SettingsFragment.FILE_NAME + ".csv", SettingsFragment.FILE_PREFIXACCAZI);
                File currentFileAccAzi = new File(baseAccAziDirectory, fileNameAccAzi);
                String currentFileAccAziPath = currentFileAccAzi.getAbsolutePath();
                BufferedWriter currentFileAccAziWriter;
                try
                {
                    currentFileAccAziWriter = new BufferedWriter(new FileWriter(currentFileAccAzi));
                } catch (IOException e)
                {
                    logException("Could not open subobservation file: " + currentFileAccAziPath, e);
                    return;
                }

                try
                {
                    if (SettingsFragment.useDeviceSensor)
                    {
                        currentFileAccAziWriter.write("TimeStamp, Year, Month, Day, Hour, Minute, Second, AccX, AccY, AccZ, " +
                                "GyroX, GyroY, GyroZ, GravX, GravY, GravZ, MagX, MagY, MayZ, RotX, RotY, RotZ, RotS, PitchX, RollY, AzimuthZ, Pressure");
                        currentFileAccAziWriter.newLine();
                    } else
                    {
                        currentFileAccAziWriter.write("Year, Month, Day, Hour, Minute, Second, Azimuth, Pitch, Roll, Altitude");
                        currentFileAccAziWriter.newLine();
                    }

                } catch (IOException e)
                {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize subobservation file: " + currentFileAccAziPath, e);
                    return;
                }

                // 关闭csv文件
                if (mFileAccAzWriter != null)
                {
                    try
                    {
                        mFileAccAzWriter.close();
                    } catch (IOException e)
                    {
                        logException("Unable to close sub observation file streams.", e);
                        return;
                    }
                }

                mFileAccAzi = currentFileAccAzi;
                mFileAccAzWriter = currentFileAccAziWriter;
                Toast.makeText(mContext, "File opened: " + currentFileAccAziPath, Toast.LENGTH_SHORT).show();

                // To make sure that files do not fill up the external storage:
                // - Remove all empty files
                FileFilter filter = new FileToDeleteFilter(mFileAccAzi);
                for (File existingFile : baseAccAziDirectory.listFiles(filter))
                {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseAccAziDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0)
                {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i)
                    {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        //NMEA文件
        synchronized (mFileNmeaLock)
        {
            if (SettingsFragment.ENABLE_NMEALOG)
            {
                File baseNmeaDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state))
                {
                    baseNmeaDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXNMEA);
                    baseNmeaDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
                {
                    logError("Cannot write to external storage.");
                    return;
                } else
                {
                    logError("Cannot read external storage.");
                    return;
                }

                Date now = new Date();
                String fileNameNmea = String.format(SettingsFragment.FILE_NAME + ".nmea", SettingsFragment.FILE_PREFIXNMEA);
                File currentFileNmea = new File(baseNmeaDirectory, fileNameNmea);
                String currentFileNmeaPath = currentFileNmea.getAbsolutePath();
                BufferedWriter currentFileNmeaWriter;
                try
                {
                    currentFileNmeaWriter = new BufferedWriter(new FileWriter(currentFileNmea));
                } catch (IOException e)
                {
                    logException("Could not open NMEA file: " + currentFileNmea, e);
                    return;
                }

                // NMEA文件的标题开头
                try
                {
                    currentFileNmeaWriter.write("NMEA");
                    currentFileNmeaWriter.newLine();
                } catch (IOException e)
                {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize NMEA file: " + currentFileNmeaPath, e);
                    return;
                }

                if (mFileNmeaWriter != null)
                {
                    try
                    {
                        mFileNmeaWriter.close();
                    } catch (IOException e)
                    {
                        logException("Unable to close NMEA file streams.", e);
                        return;
                    }
                }
                mFileNmea = currentFileNmea;
                mFileNmeaWriter = currentFileNmeaWriter;
                Toast.makeText(mContext, "File opened: " + currentFileNmeaPath, Toast.LENGTH_SHORT).show();

                // To make sure that files do not fill up the external storage:
                // - Remove all empty files
                FileFilter filter = new FileToDeleteFilter(mFileNmea);
                for (File existingFile : baseNmeaDirectory.listFiles(filter))
                {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseNmeaDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0)
                {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i)
                    {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        // Gong added: GNSS原始观测文件txt(兼容Google的GNSSlogger)
        synchronized (mFileRawLock)
        {
            if (SettingsFragment.ENABLE_RAWDATALOG)
            {
                File baseRawDataDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state))
                {
                    baseRawDataDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXRAW);
                    baseRawDataDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
                {
                    logError("Cannot write to external storage.");
                    return;
                } else
                {
                    logError("Cannot read external storage.");
                    return;
                }
                // csv文件开头File类中提供的getAbsolutePath可以通过绝对路径获取文件的位置和名称
                Date now = new Date();
                String fileNameRaw = String.format(SettingsFragment.FILE_NAME + ".txt", SettingsFragment.FILE_PREFIXRAW);
                File currentFileRaw = new File(baseRawDataDirectory, fileNameRaw);
                String currentFileRawPath = currentFileRaw.getAbsolutePath();
                BufferedWriter currentFileRawWriter;
                try
                {
                    currentFileRawWriter = new BufferedWriter(new FileWriter(currentFileRaw));
                } catch (IOException e)
                {
                    logException("Could not open subobservation file: " + currentFileRawPath, e);
                    return;
                }

                // 副观测文件的标题开头
                try
                {
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.write("Header Description:");
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.write(VERSION_TAG);
                    String manufacturer = Build.MANUFACTURER;
                    String model = Build.MODEL;
                    String fileVersion =
                            mContext.getString(R.string.app_version)
                                    + " Platform: "
                                    + Build.VERSION.RELEASE
                                    + " "
                                    + "Manufacturer: "
                                    + manufacturer
                                    + " "
                                    + "Model: "
                                    + model;
                    currentFileRawWriter.write(fileVersion);
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.write(
                            "Raw,ElapsedRealtimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,"
                                    + "BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,"
                                    + "HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,"
                                    + "ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,"
                                    + "PseudorangeRateUncertaintyMetersPerSecond,"
                                    + "AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,"
                                    + "AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,"
                                    + "CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,"
                                    + "ConstellationType,AgcDb");
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.write(
                            "Fix,Provider,Latitude,Longitude,Altitude,Speed,Accuracy,(UTC)TimeInMs");
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.write("Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)");
                    currentFileRawWriter.newLine();
                    currentFileRawWriter.write(COMMENT_START);
                    currentFileRawWriter.newLine();
                } catch (IOException e)
                {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize RAWDATA.txt file: " + currentFileRawPath, e);
                    return;
                }
                // 关闭csv文件
                if (mFileRawWriter != null)
                {
                    try
                    {
                        mFileRawWriter.close();
                    } catch (IOException e)
                    {
                        logException("Unable to close RAWDATA.txt file streams.", e);
                        return;
                    }
                }

                mFileRaw = currentFileRaw;
                mFileRawWriter = currentFileRawWriter;
                Toast.makeText(mContext, "File opened: " + currentFileRawPath, Toast.LENGTH_SHORT).show();

                // To make sure that files do not fill up the external storage:
                // - Remove all empty files
                FileFilter filter = new FileToDeleteFilter(mFileRaw);
                for (File existingFile : baseRawDataDirectory.listFiles(filter))
                {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseRawDataDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0)
                {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i)
                    {
                        existingFiles[i].delete();
                    }
                }
            }
        }
    }


    //
    public void endLog()
    {
        // 重新设置名字
        SettingsFragment.FILE_NAME = "AndroidOBS";

        // finish data
        // RINEX OBS
        if (SettingsFragment.ENABLE_RINEXOBSLOG && mFileWriter != null)
        {
            try
            {
                // 停止记录 生成文件
                stopRecordRinex();
                //
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile));
                mContext.sendBroadcast(mediaScanIntent);
                mFileWriter.close();
                mFileWriter = null;
                mObsFileWriter = null;
            } catch (IOException e)
            {
                logException("Unable to close all file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        // RINEX NAV
        if (SettingsFragment.ENABLE_RINEXNAVLOG && mFileNavWriter != null)
        {
            try
            {
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFileNav));
                mContext.sendBroadcast(mediaScanIntentSub);

                // 结束时写入文件
                stopRecordRinexNav();

                mFileNavWriter.close();
                mFileNavWriter = null;
                mNavFileWriter = null;

                RINEX_NAV_ION_OK = false;
            } catch (IOException e)
            {
                logException("Unable to close NAV file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        // KML
        if (SettingsFragment.ENABLE_KMLLOG && mFileSubWriter != null)
        {
            try
            {
                mFileSubWriter.write("    </coordinates>\n  </LineString>\n</Placemark>\n");
                // </coordinates></LineString></Placemark><Folder></Document></kml>
                mFileSubWriter.newLine();
                for (int i = 0; i < utckml.size(); i++)
                {
                    mFileSubWriter.write(" <Placemark>\n");
                    mFileSubWriter.write("<name>" + utckml.get(i) + "\"</name>\"");
                    mFileSubWriter.newLine();
                    mFileSubWriter.write("    <Snippet maxLines=\"0\"> </Snippet>");
                    mFileSubWriter.newLine();
                    mFileSubWriter.write("    <description>" + i + ", UTC " + utckml.get(i));
                    mFileSubWriter.newLine();
                    mFileSubWriter.write(longitudekml.get(i) + "," + latitudekml.get(i) + "," + altitudekml.get(i));
                    mFileSubWriter.newLine();
                    mFileSubWriter.write("    </description>");
                    //mFileSubWriter.write("<TimeStamp><when>"+arrayList1.get(i)+"</when></TimeStamp>");
                    //mFileSubWriter.newLine();
                    mFileSubWriter.write("<Style>\n<BalloonStyle><text></text></BalloonStyle>\n" +
                            "    <LabelStyle><scale>0</scale></LabelStyle>\n" +
                            "    <IconStyle>\n" +
                            "      <scale>0.6</scale>\n" +
                            "      <color>bfed8548</color>\n" +
                            "      <Icon><href>http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png</href></Icon>\n" +
                            "    </IconStyle>\n" +
                            "  </Style>\n" +
                            "  <Point>");
                    mFileSubWriter.newLine();
                    mFileSubWriter.write("<coordinates>" + longitudekml.get(i) + "," + latitudekml.get(i) + "," + altitudekml.get(i) + "</coordinates>");
                    mFileSubWriter.write("</Point>");
                    mFileSubWriter.newLine();
                    mFileSubWriter.write("</Placemark>");
                }
                mFileSubWriter.newLine();
                mFileSubWriter.write("\n  </Document>\n  </kml>");
            } catch (IOException e)
            {
                Toast.makeText(mContext, "ERROR_WRITINGFOTTER_FILE", Toast.LENGTH_SHORT).show();
                logException(ERROR_WRITING_FILE, e);
            }

            // 关闭文件
            try
            {
                mFileSubWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFileSub));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileSubWriter = null;
            } catch (IOException e)
            {
                logException("Unable to close subobservation file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }

        }

        // NMEA Logs
        if (SettingsFragment.ENABLE_NMEALOG && mFileNmeaWriter != null)
        {
            try
            {
                mFileNmeaWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFileNmea));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileNmeaWriter = null;
            } catch (IOException e)
            {
                logException("Unable to close NMEA file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        // SENSORS Data
        if (SettingsFragment.ENABLE_SENSORSLOG && mFileAccAzWriter != null)
        {
            try
            {
                mFileAccAzWriter.close();
                Intent mediaScanIntentSensor = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFileAccAzi));
                mContext.sendBroadcast(mediaScanIntentSensor);
                mFileAccAzWriter = null;
            } catch (IOException e)
            {
                logException("Unable to close SENSORS file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        // RAW Data
        if (SettingsFragment.ENABLE_RAWDATALOG && mFileRawWriter != null)
        {
            try
            {
                mFileRawWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFileRaw));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileRawWriter = null;
            } catch (IOException e)
            {
                logException("Unable to close RAWDATA file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }
        //Log.i("progress","dismiss");
        //mUiComponent.ShowProgressWindow(false);
    } // 各文件保存

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }

    @Override
    public void onLocationChanged(Location location)
    {
        // 使用GPS或得到的经纬度
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER))
        {
            // KML
            synchronized (mFileSubLock)
            {
                if (mFileSubWriter == null)
                {
                    return;
                } else
                {
                    try
                    {
                        longitudekml.add(location.getLongitude());
                        latitudekml.add(location.getLatitude());
                        altitudekml.add(location.getAltitude());
                        //
                        Calendar myCal = Calendar.getInstance();
                        DateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String utcTime = myFormat.format(myCal.getTime());
                        utckml.add(utcTime);
                        //
                        String gnsstime = String.format("%d,%d,%d,%d,%d,%13.7f", gnsstimeclock_f, gnsstimeclock_e, gnsstimeclock_a, gnsstimeclock_b, gnsstimeclock_c, gnsstimeclock_d);
                        gpstkml.add(gnsstime);
                        //}catch (IOException e){
                    } catch (Exception e)
                    {
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }

        // 初始化位置坐标
        if (location != null && !poseinitialized)
        {
            mLocation = location;
            try
            {
                //用于计算的接收机近似位置(直接经纬度转换得到的)
                pose = Coordinates.globalGeodInstance(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude());

                //坐标转换
                double[] xyz = CoodinateConv.WGS84LLAtoXYZ(location.getLatitude(), location.getLongitude(), location.getAltitude());

                approximateX = xyz[0];
                approximateY = xyz[1];
                approximateZ = xyz[2];
                Log.d(TAG, "approximateXYZ: " + approximateX + ',' + approximateY + ',' + approximateZ);
            } catch (Exception e)
            {
                Log.d(TAG, "避免出现Location不为空值，但纬度经度为空值的情况");
            }

            //接收机位置是否初始化(每次都重新计算)
            //poseinitialized=true;
        }
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras)
    {

    }


    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event)
    {
        // 检查采样间隔(间隔几个历元)
         if (localintervaltime < SettingsFragment.interval) {
             localintervaltime++;
             Log.i("interval", String.valueOf(localintervaltime) + "," + String.valueOf(SettingsFragment.interval));
             return;
         } else {
             localintervaltime = 1;
         }

         //
        GnssClock clock = event.getClock();

        gpsTime = new GpsTime(clock);

        sumConstellation.updateMeasurements(event);

        EpochMeasurement epochMeasurement = sumConstellation.getEpochMeasurement();

        // 检测到GNSSClock
        SettingsFragment.GNSSClockSync_FILE = true;

        // 如果表明正在记录文件，则需要执行更新文件
        if (SettingsFragment.EnableLogging)
        {
            // 记录文件
            try
            {
                // 写入rinex文件
                rinex.writeBody(epochMeasurement);

                // 写入原始log文件
                writeRawGnssMeasurementToFile(clock, event);

                // TODO: 计算卫星位置/平差
                // ....

            } catch (IOException e)
            {
                logException(ERROR_WRITING_FILE, e);
            }
        }

        // 如果设置了观测时长(历元数量)
         if(SettingsFragment.enableTimer){
             SettingsFragment.timer = SettingsFragment.timer - 1;
             getUiComponent().RefreshTimer();
         }

    }  // 计算传播时间，o文件头下部

    @Override
    public void onGnssMeasurementsStatusChanged(int status)
    {

    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage)
    {
        synchronized (mFileNavLock)
        {
            if (SettingsFragment.ENABLE_RINEXNAVLOG && SettingsFragment.EnableLogging)
            {
                if (mFileNavWriter == null)
                {
                    return;
                }
                //TODO: 写入导航文件
                //卫星号
                int svid = navigationMessage.getSvid();
                //原始数据
                byte[] rawData = navigationMessage.getData();
                //
                int messageId = navigationMessage.getMessageId();
                //
                int submessageId = navigationMessage.getSubmessageId();
                int type = navigationMessage.getType();

                gpsNavigationConv.onGpsNavMessageReported(svid, type, submessageId, rawData);

            }
        }
    }


    public void onRawSensorListener(String listener, float timeStamp, float rawAcc[], float rawGyro[], float rawGrav[], float rawMag[],
                                    float rawRot[], float rawOrient[], float rawPre)
    {
        synchronized (mFileAccAzLock)
        {
            if (mFileAccAzWriter == null || !SettingsFragment.ENABLE_SENSORSLOG)
            {
                return;
            } else
            {
                if (listener == "")
                {
                    try
                    {
                        // Log.e("Write","Writing Sensors Data");
                        Calendar myCal = Calendar.getInstance();
                        DateFormat myFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss.SSS");
                        String myTime = myFormat.format(myCal.getTime());
                        //csv文件内容 行人位置模型 altitude是气压传感器
                        String SensorStream =
                                String.format("%f, %s, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f\n",
                                        timeStamp, myTime,
                                        rawAcc[0], rawAcc[1], rawAcc[2], rawGyro[0], rawGyro[1], rawGyro[2],
                                        rawGrav[0], rawGrav[1], rawGrav[2], rawMag[0], rawMag[1], rawMag[2],
                                        rawRot[0], rawRot[1], rawRot[2], rawRot[3],
                                        rawOrient[1], rawOrient[2], rawOrient[0], rawPre);

                        mFileAccAzWriter.write(SensorStream);

//                        String day= String.format("%6d,%6d,%6d,%13.7f,\t",gnsstimeclock_a,gnsstimeclock_b,gnsstimeclock_c,gnsstimeclock_d,myName);
//                        mFileAccAzWriter.write(day);
//                        String time= String.format("%13.7f",myName);
//                        mFileAccAzWriter.write(myName);
//                        mFileAccAzWriter.newLine();

                    } catch (IOException e)
                    {
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }
    }

    public void onSensorListener(String listener, float azimuth, float pitch, float roll, float altitude)
    {
        synchronized (mFileAccAzLock)
        {
            if (mFileAccAzWriter == null || !SettingsFragment.ENABLE_SENSORSLOG)
            {
                return;
            } else
            {
                if (listener == "")
                {
                    try
                    {
                        Calendar myCal = Calendar.getInstance();
                        DateFormat myFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss.SSSSSS");
                        String myTime = myFormat.format(myCal.getTime());
                        //
                        String SensorStream =
                                String.format("%s, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f\n",
                                        myTime, azimuth, pitch, roll, altitude);
                        mFileAccAzWriter.write(SensorStream);
                    } catch (IOException e)
                    {
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status)
    {
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus)
    {
        try
        {
            writeUseInFixArray(gnssStatus);
        } catch (IOException e)
        {
            Toast.makeText(mContext, "FATAL_ERROR_FOR_WRITING_ARRAY", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNmeaReceived(long timestamp, String s)
    {
        synchronized (mFileNmeaLock)
        {
            if (mFileNmeaWriter == null)
            {
                return;
            } else
            {
                try
                {
                    String NmeaStream = String.format(Locale.US, "NMEA,%s,%d", s.trim(), timestamp);
                    mFileNmeaWriter.write(NmeaStream);
                    mFileNmeaWriter.newLine();
                } catch (IOException e)
                {
                    Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                    logException(ERROR_WRITING_FILE, e);
                }
            }
        }
    }

    @Override
    public void onListenerRegistration(String listener, boolean result)
    {

    }

    private void writeUseInFixArray(GnssStatus gnssStatus) throws IOException
    {
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++)
        {
            if (gnssStatus.usedInFix(i) && gnssStatus.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS)
            {
                if (UsedInFixList.indexOf(gnssStatus.getSvid(i)) == -1)
                {
                    UsedInFixList.add(gnssStatus.getSvid(i));
                }
            } else if (gnssStatus.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS)
            {
                int index = UsedInFixList.indexOf(gnssStatus.getSvid(i));
                if (index != -1)
                {
                    UsedInFixList.remove(index);
                }
            }
        }
    }

    private boolean ReadUseInFixArray(int Svid) throws IOException
    {
        return UsedInFixList.indexOf(Svid) != -1;
    }

    private void writeRawGnssMeasurementToFile(GnssClock clock, GnssMeasurementsEvent event) throws IOException
    {
        try
        {
            for (GnssMeasurement measurement : event.getMeasurements())
            {
                String clockStream =
                        String.format(
                                "Raw,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                SystemClock.elapsedRealtime(),
                                clock.getTimeNanos(),
                                clock.hasLeapSecond() ? clock.getLeapSecond() : "",
                                clock.hasTimeUncertaintyNanos() ? clock.getTimeUncertaintyNanos() : "",
                                clock.getFullBiasNanos(),
                                clock.hasBiasNanos() ? clock.getBiasNanos() : "",
                                clock.hasBiasUncertaintyNanos() ? clock.getBiasUncertaintyNanos() : "",
                                clock.hasDriftNanosPerSecond() ? clock.getDriftNanosPerSecond() : "",
                                clock.hasDriftUncertaintyNanosPerSecond()
                                        ? clock.getDriftUncertaintyNanosPerSecond()
                                        : "",
                                clock.getHardwareClockDiscontinuityCount() + ",");
                mFileRawWriter.write(clockStream);

                String measurementStream =
                        String.format(
                                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                measurement.getSvid(),
                                measurement.getTimeOffsetNanos(),
                                measurement.getState(),
                                measurement.getReceivedSvTimeNanos(),
                                measurement.getReceivedSvTimeUncertaintyNanos(),
                                measurement.getCn0DbHz(),
                                measurement.getPseudorangeRateMetersPerSecond(),
                                measurement.getPseudorangeRateUncertaintyMetersPerSecond(),
                                measurement.getAccumulatedDeltaRangeState(),
                                measurement.getAccumulatedDeltaRangeMeters(),
                                measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
                                measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "",
                                measurement.hasCarrierCycles() ? measurement.getCarrierCycles() : "",
                                measurement.hasCarrierPhase() ? measurement.getCarrierPhase() : "",
                                measurement.hasCarrierPhaseUncertainty()
                                        ? measurement.getCarrierPhaseUncertainty()
                                        : "",
                                measurement.getMultipathIndicator(),
                                measurement.hasSnrInDb() ? measurement.getSnrInDb() : "",
                                measurement.getConstellationType(),
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                        && measurement.hasAutomaticGainControlLevelDb()
                                        ? measurement.getAutomaticGainControlLevelDb()
                                        : "");
                mFileRawWriter.write(measurementStream);
                mFileRawWriter.newLine();
            }
        } catch (IOException e)
        {
            logException(ERROR_WRITING_FILE, e);
        }
    }

    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurementsEvent event) throws IOException
    {

    }

    private void logException(String errorMessage, Exception e)
    {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage)
    {
        Log.e(GnssContainer.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link FileToDeleteFilter#mRetainedFiles}.
     */
    private static class FileToDeleteFilter implements FileFilter
    {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles)
        {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname)
        {
            if (pathname == null || !pathname.exists())
            {
                return false;
            }
            if (mRetainedFiles.contains(pathname))
            {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }


    // 从GPS周秒到GPS时的转换
    public static class ReturnValue
    {
        public int Y;
        public int M;
        public int D;
        public int h;
        public int m;
        public double s;
    }

    public static class GPSWStoGPST
    {
        public ReturnValue method(double GPSW, double GPSWS)
        {
            ReturnValue value = new ReturnValue();
            // MJD和MD的计算
            double MD = (int) (GPSWS / 86400);
            double MJD = 44244 + GPSW * 7 + MD;
            // 简化儒略日至年月日
            double JD = MJD + 2400000.5;
            double N = JD + 0.5;
            int Z = (int) N;
            double F = N - Z;
            double A;
            if (Z >= 2299161)
            {
                int X = (int) ((Z - 1867216.25) / 36524.25);
                A = Z + 1 + X - X / 4;
            } else
            {
                A = Z;
            }
            double B = A + 1524;
            int C = (int) ((B - 122.1) / 365.25);
            int K = (int) (365.25 * C);
            int E = (int) ((B - K) / 30.6001);
            double D = B - K - (int) (30.6001 * E) + F;
            int M;
            int Y;
            if (E < 13.5)
            {
                M = E - 1;
            } else
            {
                M = E - 13;
            }
            if (M > 2.5)
            {
                Y = C - 4716;
            } else
            {
                Y = C - 4715;
            }
            value.Y = Y;
            value.M = M;
            value.D = (int) D;

            //GPS周秒至GPS时分秒
            double DS = GPSWS - MD * 86400;
            int h = (int) (DS / 3600);
            double hm = DS - h * 3600;
            int m = (int) (hm / 60);
            double s = hm - m * 60;

            value.h = h;
            value.m = m;
            value.s = s;

            return value;

        }

    }

    @Override
    public void onTTFFReceived(long l)
    {
    }

    // Gong added:
    private void startRecordRinex()
    {
        // 初始化rinex类
        rinex = new Rinex(mContext, sharedPreferences.getInt(Constants.KEY_RINEX_VER, Constants.DEF_RINEX_VER));

        // 设置rinex文件的写文件指针
        rinex.setFileWriter(mObsFileWriter);

        rinex.writeHeader(new RinexHeader(
                sharedPreferences.getString(Constants.KEY_MARK_NAME, Constants.DEF_MARK_NAME),
                sharedPreferences.getString(Constants.KEY_MARK_TYPE, Constants.DEF_MARK_TYPE),
                sharedPreferences.getString(Constants.KEY_OBSERVER_NAME, Constants.DEF_OBSERVER_NAME),
                sharedPreferences.getString(Constants.KEY_OBSERVER_AGENCY_NAME, Constants.DEF_OBSERVER_AGENCY_NAME),
                sharedPreferences.getString(Constants.KEY_RECEIVER_NUMBER, Constants.DEF_RECEIVER_NUMBER),
                sharedPreferences.getString(Constants.KEY_RECEIVER_TYPE, Constants.DEF_RECEIVER_TYPE),
                sharedPreferences.getString(Constants.KEY_RECEIVER_VERSION, Constants.DEF_RECEIVER_VERSION),
                sharedPreferences.getString(Constants.KEY_ANTENNA_NUMBER, Constants.DEF_ANTENNA_NUMBER),
                sharedPreferences.getString(Constants.KEY_ANTENNA_TYPE, Constants.DEF_ANTENNA_TYPE),
                Double.parseDouble(sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_EAST, Constants.DEF_ANTENNA_ECCENTRICITY_EAST)),
                Double.parseDouble(sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_NORTH, Constants.DEF_ANTENNA_ECCENTRICITY_NORTH)),
                Double.parseDouble(sharedPreferences.getString(Constants.KEY_ANTENNA_HEIGHT, Constants.DEF_ANTENNA_HEIGHT)),
                String.valueOf(approximateX), String.valueOf(approximateY), String.valueOf(approximateZ), gpsTime
        ));
    }

    private void stopRecordRinex()
    {
        rinex.closeFile();
    }

    private void startRecordRinexNav()
    {
        rinexNav = new RinexNav(mContext, sharedPreferences.getInt(Constants.KEY_RINEX_VER, Constants.DEF_RINEX_VER));
        rinexNav.setFileWriter(mNavFileWriter);
        //需要更新数据库后再写表头
    }

    private void stopRecordRinexNav()
    {
        rinexNav.writeHeader(gpsNavigationConv.sqliteManager);
        rinexNav.writeBody(gpsNavigationConv.sqliteManager);
        rinexNav.closeFile();
    }

}
