package com.whu.gnss.gnsslogger;

import android.content.Context;
import android.content.Intent;
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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.SystemClock;
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

//aiueo
public class FileLogger implements GnssListener {

    private static final String TAG = "FileLogger";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "Version: ";
    private static final String FILE_VERSION = "1.4.0.0, Platform: N";
    private static final double GPS_L1_FREQ = 154.0 * 10.23e6;  //1575.42MHz
    private static final double GPS_L5_FREQ=115.0 * 10.23e6; //1176.45MHz
    private static final double SPEED_OF_LIGHT = 299792458.0; //[m/s]
    private static final double GPS_L1_WAVELENGTH = SPEED_OF_LIGHT/GPS_L1_FREQ;
    private static final double GPS_L5_WAVELENGTH = SPEED_OF_LIGHT/GPS_L5_FREQ;

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;

    private final Object mFileLock = new Object();
    private  final Object mFileSubLock = new Object();
    private final Object mFileAccAzLock = new Object();
    private final Object mFileNmeaLock = new Object();
    private final Object mFileNavLock = new Object();
    private final Object mFileRawLock = new Object();

    private BufferedWriter mFileWriter;         // .o
    private BufferedWriter mFileSubWriter;      // .kml
    private BufferedWriter mFileAccAzWriter;    // .csv
    private BufferedWriter mFileNmeaWriter;     // .nmea
    private BufferedWriter mFileNavWriter;      // .nav
    private BufferedWriter mFileRawWriter;      // Gong added: .txt

    private File mFile;
    private File mFileSub;
    private File mFileAccAzi;
    private File mFileNmea;
    private File mFileNav;
    private File mFileRaw; // Gong added

    private boolean firsttime;
    private UIFragmentComponent mUiComponent;
    //private LoggerFragment mloggerFragment;

    private boolean notenoughsat = false;
    private boolean firstOBSforAcc = true;

    private ArrayList<Integer> UsedInFixList = new ArrayList<Integer>() ;
    private ArrayList<String> utckml= new ArrayList<>();
    private ArrayList<Double>longitudekml=new ArrayList<>();
    private ArrayList<Double>latitudekml=new ArrayList<>();
    private ArrayList<Double>altitudekml=new ArrayList<>();
    private ArrayList<String> gpstkml=new ArrayList<String>();
    private boolean RINEX_NAV_ION_OK = false;
    final float TOLERANCE_MHZ = 1e8f;
    // GLONASS系统的补正信息
    private int[] GLONASSFREQ = {1,-4,5,6,1,-4,5,6,-2,-7,0,-1,-2,-7,0,-1,4,-3,3,2,4,-3,3,2};
    private int leapseconds = 18;

    private double[] CURRENT_SMOOTHER_RATE = new double[300];
    private double[] LAST_DELTARANGE = new double[300];
    private double[] LAST_SMOOTHED_PSEUDORANGE = new double[300];
    private double SMOOTHER_RATE = 0.01;
    private boolean initialize = false;

    private double constFullBiasNanos = 0.0;

    // 区间变量
    private int localintervaltime = 1;

    public synchronized UIFragmentComponent getUiComponent() {
        return mUiComponent;
    }

    public synchronized void setUiComponent(UIFragmentComponent value) {
        mUiComponent = value;
    }

    public FileLogger(Context context) {
        this.mContext = context;
        if(initialize == false){
            Arrays.fill(LAST_DELTARANGE,0.0);
            Arrays.fill(CURRENT_SMOOTHER_RATE,1.0);
            Arrays.fill(LAST_SMOOTHED_PSEUDORANGE,0.0);
            initialize = true;
            Log.d("FileLogger","Initialize complete");
        }
    }

    /**
     * Start a new file logging process.
     */
    public void startNewLog() {
        // Gong added:
        if(SettingsFragment.FILE_NAME=="AndroidOBS"){
            Calendar myCal= Calendar.getInstance();
            DateFormat myFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
            String myTime = myFormat.format(myCal.getTime());
            SettingsFragment.FILE_NAME = myTime;
        }


        // .kml文件
        synchronized (mFileSubLock){
            File baseSubDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseSubDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXSUB);
                baseSubDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
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
            try {
                currentFileSubWriter = new BufferedWriter(new FileWriter(currentFileSub));
            } catch (IOException e) {
                logException("Could not open subobservation file: " + currentFileSubPath, e);
                return;
            }
            // 副观测文件的标题开头
            try {
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
            } catch (IOException e) {
                Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                logException("Count not initialize subobservation file: " + currentFileSubPath, e);
                return;
            }

            if (mFileSubWriter != null) {
                try {
                    mFileSubWriter.close();
                } catch (IOException e) {
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
            for (File existingFile : baseSubDirectory.listFiles(filter)) {
                existingFile.delete();
            }
            // - Trim the number of files with data
            File[] existingFiles = baseSubDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }
        }

        // .csv传感器文件
        synchronized (mFileAccAzLock){
            if(SettingsFragment.ENABLE_SENSORSLOG) {
                File baseAccAziDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    baseAccAziDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXACCAZI);
                    baseAccAziDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    logError("Cannot write to external storage.");
                    return;
                } else {
                    logError("Cannot read external storage.");
                    return;
                }
                // csv文件开头File类中提供的getAbsolutePath可以通过绝对路径获取文件的位置和名称
                Date now = new Date();
                String fileNameAccAzi = String.format(SettingsFragment.FILE_NAME + ".csv", SettingsFragment.FILE_PREFIXACCAZI);
                File currentFileAccAzi = new File(baseAccAziDirectory, fileNameAccAzi);
                String currentFileAccAziPath = currentFileAccAzi.getAbsolutePath();
                BufferedWriter currentFileAccAziWriter;
                try {
                    currentFileAccAziWriter = new BufferedWriter(new FileWriter(currentFileAccAzi));
                } catch (IOException e) {
                    logException("Could not open subobservation file: " + currentFileAccAziPath, e);
                    return;
                }

                // 副观测文件的标题开头
                try {
                    if(SettingsFragment.ENABLE_SENSORSLOG) {
//                        currentFileAccAziWriter.write("Android Acc\nEast,North ");
//                        currentFileAccAziWriter.newLine();
                        currentFileAccAziWriter.write("Year, Month, Day, Hour, Minute, Second, AccX, AccY, AccZ, " +
                                "GyroX, GyroY, GyroZ, GravX, GravY, GravZ, MagX, MagY, MayZ, RotX, RotY, RotZ, RotS, Pressure");
                        currentFileAccAziWriter.newLine();
                    }else {
                         currentFileAccAziWriter.write("PseudorangeRate,PseudorangeRate (Carrier Phase),PseudorangeRate (Doppler) ");
                         currentFileAccAziWriter.newLine();
                    }

                } catch (IOException e) {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize subobservation file: " + currentFileAccAziPath, e);
                    return;
                }
                // 关闭csv文件
                if (mFileAccAzWriter != null) {
                    try {
                        mFileAccAzWriter.close();
                    } catch (IOException e) {
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
                for (File existingFile : baseAccAziDirectory.listFiles(filter)) {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseAccAziDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0) {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i) {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        // rinex的o文件
        synchronized (mFileLock) {
            File baseDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            Date now = new Date();
            int observation = now.getYear() - 100;
            String fileName = String.format(SettingsFragment.FILE_NAME + "." + observation + "o", SettingsFragment.FILE_PREFIX);
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;
            try {
                currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open observation file: " + currentFilePath, e);
                return;
            }

            // initialize the contents of the file
            try {

                //RINEX ver3.03
                if(SettingsFragment.RINEX303){

                }//RINEX ver2.11
                else {

                }
                firsttime = true;
                localintervaltime = SettingsFragment.interval;
            } catch (Exception e) {
                Toast.makeText(mContext, "Count not initialize observation file", Toast.LENGTH_SHORT).show();
                logException("Count not initialize file: " + currentFilePath, e);
                return;
            }

            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
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
            for (File existingFile : baseDirectory.listFiles(filter)) {
                existingFile.delete();
            }
            // - Trim the number of files with data
            File[] existingFiles = baseDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }

            //
        }

        //NMEA文件
        synchronized (mFileNmeaLock){
                File baseNmeaDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    baseNmeaDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXNMEA);
                    baseNmeaDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    logError("Cannot write to external storage.");
                    return;
                } else {
                    logError("Cannot read external storage.");
                    return;
                }

                Date now = new Date();
                String fileNameNmea = String.format(SettingsFragment.FILE_NAME + ".nmea", SettingsFragment.FILE_PREFIXNMEA);
                File currentFileNmea = new File(baseNmeaDirectory, fileNameNmea);
                String currentFileNmeaPath = currentFileNmea.getAbsolutePath();
                BufferedWriter currentFileNmeaWriter;
                try {
                    currentFileNmeaWriter = new BufferedWriter(new FileWriter(currentFileNmea));
                } catch (IOException e) {
                    logException("Could not open NMEA file: " + currentFileNmea, e);
                    return;
                }

                // NMEA文件的标题开头

                try {
                    currentFileNmeaWriter.write("NMEA");
                    currentFileNmeaWriter.newLine();
                } catch (IOException e) {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize NMEA file: " + currentFileNmeaPath, e);
                    return;
                }

                if (mFileNmeaWriter != null) {
                    try {
                        mFileNmeaWriter.close();
                    } catch (IOException e) {
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
                for (File existingFile : baseNmeaDirectory.listFiles(filter)) {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseNmeaDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0) {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i) {
                        existingFiles[i].delete();
                    }
                }
        }

        // Gong added: GNSS原始观测文件txt(兼容Google的GNSSlogger)
        synchronized (mFileRawLock){
            if(SettingsFragment.ENABLE_RAWDATALOG) {
                File baseRawDataDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    baseRawDataDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXRAW);
                    baseRawDataDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    logError("Cannot write to external storage.");
                    return;
                } else {
                    logError("Cannot read external storage.");
                    return;
                }
                // csv文件开头File类中提供的getAbsolutePath可以通过绝对路径获取文件的位置和名称
                Date now = new Date();
                String fileNameRaw = String.format(SettingsFragment.FILE_NAME + ".txt", SettingsFragment.FILE_PREFIXRAW);
                File currentFileRaw = new File(baseRawDataDirectory, fileNameRaw);
                String currentFileRawPath = currentFileRaw.getAbsolutePath();
                BufferedWriter currentFileRawWriter;
                try {
                    currentFileRawWriter = new BufferedWriter(new FileWriter(currentFileRaw));
                } catch (IOException e) {
                    logException("Could not open subobservation file: " + currentFileRawPath, e);
                    return;
                }

                // 副观测文件的标题开头
                try {
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
                } catch (IOException e) {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize RAWDATA.txt file: " + currentFileRawPath, e);
                    return;
                }
                // 关闭csv文件
                if (mFileRawWriter != null) {
                    try {
                        mFileRawWriter.close();
                    } catch (IOException e) {
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
                for (File existingFile : baseRawDataDirectory.listFiles(filter)) {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseRawDataDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0) {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i) {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        // N文件开头
        if(SettingsFragment.ENABLE_RINEXNAVLOG) {
            synchronized (mFileNavLock) {
                File baseNavDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    baseNavDirectory = new File(Environment.getExternalStorageDirectory(), SettingsFragment.FILE_PREFIXNAV);
                    baseNavDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    logError("Cannot write to external storage.");
                    return;
                } else {
                    logError("Cannot read external storage.");
                    return;
                }

                Date now = new Date();
                int observation = now.getYear() - 100;
                String fileNameNav = String.format(SettingsFragment.FILE_NAME + "." + observation + "n", SettingsFragment.FILE_PREFIXNAV);
                File currentFileNav = new File(baseNavDirectory, fileNameNav);
                String currentFileNavPath = currentFileNav.getAbsolutePath();
                BufferedWriter currentFileNavWriter;
                try {
                    currentFileNavWriter = new BufferedWriter(new FileWriter(currentFileNav));
                } catch (IOException e) {
                    logException("Could not open NMEA file: " + currentFileNav, e);
                    return;
                }
                try {
                    currentFileNavWriter.write("     3.03           N: GNSS NAV DATA    M: MIXED            RINEX VERSION / TYPE");
                    currentFileNavWriter.newLine();
                    currentFileNavWriter.write("                                                            PGM / RUN BY / DATE");
                    currentFileNavWriter.newLine();
                } catch (IOException e) {
                    Toast.makeText(mContext, "Count not initialize observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize file: " + currentFileNavPath, e);
                    return;
                }
/*
                try {
                    currentFileNmeaWriter.write("NMEA");
                    currentFileNmeaWriter.newLine();
                } catch (IOException e) {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize NMEA file: " + currentFileNmeaPath, e);
                    return;
                }
*/
                if (mFileNavWriter != null) {
                    try {
                        mFileNavWriter.close();
                    } catch (IOException e) {
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
                for (File existingFile : baseNavDirectory.listFiles(filter)) {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseNavDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0) {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i) {
                        existingFiles[i].delete();
                    }
                }
            }
        }

    } // 各文件编辑

    /**
     * Send the current log via email or other options selected from a pop menu shown to the user. A
     * new log is started when calling this function.
     */
    public void send() {
        //mUiComponent.ShowProgressWindow(true);
        if (mFile == null) {
            //mUiComponent.ShowProgressWindow(false);
            return;
        }
        if (mFileSub == null){
            //mUiComponent.ShowProgressWindow(false);
            return;
        }
        if(mFileAccAzi == null){
            //mUiComponent.ShowProgressWindow(false);
            return;
        }
        if(mFileNmea == null){
            //mUiComponent.ShowProgressWindow(false);
            return;
        }
        if(mFileNav == null){
            //mUiComponent.ShowProgressWindow(false);
            return;
        }
        if(mFileRaw == null){
            //mUiComponent.ShowProgressWindow(false);
            return;
        }

        try {
            mFileSubWriter.write("    </coordinates>\n  </LineString>\n</Placemark>\n");
            // </coordinates></LineString></Placemark><Folder></Document></kml>
            mFileSubWriter.newLine();
            for (int i=0; i<utckml.size(); i++){
                mFileSubWriter.write(" <Placemark>\n");
                mFileSubWriter.write("<name>" + utckml.get(i) + "\"</name>\"");
                mFileSubWriter.newLine();
                mFileSubWriter.write("    <Snippet maxLines=\"0\"> </Snippet>");
                mFileSubWriter.newLine();
                mFileSubWriter.write("    <description>" + i + ", UTC " + utckml.get(i));
                mFileSubWriter.newLine();
                mFileSubWriter.write(longitudekml.get(i)+","+latitudekml.get(i)+","+altitudekml.get(i));
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
                                "  <Point>" );
                mFileSubWriter.newLine();
                mFileSubWriter.write("<coordinates>"+longitudekml.get(i)+","+latitudekml.get(i)+","+altitudekml.get(i)+ "</coordinates>");
                mFileSubWriter.write("</Point>");
                mFileSubWriter.newLine();
                mFileSubWriter.write("</Placemark>");
            }mFileSubWriter.newLine();
            mFileSubWriter.write("\n  </Document>\n  </kml>");
        }catch (IOException e){
            Toast.makeText(mContext, "ERROR_WRITINGFOTTER_FILE", Toast.LENGTH_SHORT).show();
            logException(ERROR_WRITING_FILE, e);
        }

        // finish data
        // rinex OBS
        if (SettingsFragment.ENABLE_RINEXOBSLOG && mFileWriter != null) {
            try {
                mFileWriter.close();
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(mFile));
                mContext.sendBroadcast(mediaScanIntent);
                mFileWriter = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        // kml
        if( SettingsFragment.ENABLE_KMLLOG && mFileSubWriter != null) {
            try {
                mFileSubWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(mFileSub));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileSubWriter = null;
            } catch (IOException e) {
                logException("Unable to close subobservation file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }



        if ( SettingsFragment.ENABLE_SENSORSLOG && mFileAccAzWriter != null) {
            try {
                mFileAccAzWriter.close();
                Intent mediaScanIntentSensor = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(mFileAccAzi));
                mContext.sendBroadcast(mediaScanIntentSensor);
                mFileAccAzWriter = null;
            } catch (IOException e) {
                logException("Unable to close sensorlog file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }


        if(mFileNmeaWriter != null) {
            try {
                mFileNmeaWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(mFileNmea));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileNmeaWriter = null;
            } catch (IOException e) {
                logException("Unable to close NMEA file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        if(SettingsFragment.ENABLE_RINEXNAVLOG && mFileNavWriter != null){
            try {
                mFileNavWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(mFileNav));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileNavWriter = null;
                RINEX_NAV_ION_OK = false;
            } catch (IOException e) {
                logException("Unable to close NAV file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }

        if(SettingsFragment.ENABLE_RAWDATALOG){
            try {
                mFileRawWriter.close();
                Intent mediaScanIntentSub = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(mFileRaw));
                mContext.sendBroadcast(mediaScanIntentSub);
                mFileRawWriter = null;
            } catch (IOException e) {
                logException("Unable to close RAWDATA file streams.", e);
                //mUiComponent.ShowProgressWindow(false);
                return;
            }
        }
        //Log.i("progress","dismiss");
        //mUiComponent.ShowProgressWindow(false);
    } // 各文件保存

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            synchronized (mFileSubLock) {
                if (mFileSubWriter == null) {
                    return;
                }
                else{
                    try {
                        String locationStream =
                                String.format(

                                        Locale.US,
                                        // KML的内容
                                        "       %15.9f,%15.9f,%15.9f",
                                        location.getLongitude(),
                                        location.getLatitude(),
                                        location.getAltitude()
                                        );
                        longitudekml.add(location.getLongitude());
                        latitudekml.add(location.getLatitude());
                        altitudekml.add(location.getAltitude());
                        //
                        Calendar myCal= Calendar.getInstance();
                        DateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String utcTime = myFormat.format(myCal.getTime());
                        utckml.add(utcTime);
                        //
                        String gnsstime= String.format("%d,%d,%d,%d,%d,%13.7f",gnsstimeclock_f,gnsstimeclock_e,gnsstimeclock_a,gnsstimeclock_b,gnsstimeclock_c,gnsstimeclock_d);
                        gpstkml.add(gnsstime);
                    //}catch (IOException e){
                    }catch (Exception e){
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {

    }


    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }

            // 获取GNSS时钟数据
            GnssClock gnssClock = event.getClock();

            // 如果平滑化方式改变，则初始化系数
            if(SettingsFragment.SMOOTHER_RATE_RESET_FLAG_FILE){
                Arrays.fill(LAST_DELTARANGE,0.0);
                Arrays.fill(CURRENT_SMOOTHER_RATE,1.0);
                Arrays.fill(LAST_SMOOTHED_PSEUDORANGE,0.0);
                SettingsFragment.SMOOTHER_RATE_RESET_FLAG_FILE = false;
            }

            // 通过参数的 getMeasurements()方法获取观测值对象的集合
            // 因为接收机只有一部, 而观测卫星众多, 这就是钟对象只有一个, 而观测值对象有多个的原因
            // 只要有接收到一颗GPS卫星的信号, 就开始记录
            for (GnssMeasurement measurement : event.getMeasurements()) {
                try {
                    // 首次观测, 文件头
                    if(firsttime == true && measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS){
                        gnssClock = event.getClock();

                        double weekNumber = Math.floor(-(gnssClock.getFullBiasNanos() * 1e-9 / 604800));
                        double weekNumberNanos = weekNumber * 604800 * 1e9;
                        double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;
                        if (gnssClock.hasBiasNanos()) {
                            tRxNanos = tRxNanos - gnssClock.getBiasNanos();
                        }
                        // 从GPS周、周秒转换成年月、日期、分秒
                        GPSWStoGPST gpswStoGPST = new GPSWStoGPST();
                        ReturnValue value = gpswStoGPST.method(weekNumber, tRxNanos * 1e-9);
                        if (measurement.getTimeOffsetNanos() != 0) {
                            tRxNanos = tRxNanos - measurement.getTimeOffsetNanos();
                        }
                        double tRxSeconds = tRxNanos * 1e-9;
                        double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
                        // GPS周的重复检查
                        double prSeconds = tRxSeconds - tTxSeconds;
                        boolean iRollover = prSeconds > 604800 / 2;
                        if (iRollover) {
                            double delS = Math.round(prSeconds / 604800) * 604800;
                            double prS = prSeconds - delS;
                            double maxBiasSeconds = 10;
                            if (prS > maxBiasSeconds) {
                                Log.e("RollOver", "Rollover Error");
                                iRollover = true;
                            } else {
                                tRxSeconds = tRxSeconds - delS;
                                prSeconds = tRxSeconds - tTxSeconds;
                                iRollover = false;
                            }

                        }
                        // 代码伪距离的计算
                        double prm = prSeconds * 2.99792458e8;
                        if (iRollover == false && prm > 0 && prSeconds < 0.5) {
                            if (SettingsFragment.RINEX303) {
                                mFileWriter.write(String.format("  %4d    %2d    %2d    %2d    %2d   %10.7f     GPS         TIME OF FIRST OBS   ", value.Y, value.M, value.D, value.h, value.m, value.s));
                                mFileWriter.newLine();
                                mFileWriter.write(" 24 R01  1 R02 -4 R03  5 R04  6 R05  1 R06 -4 R07  5 R08  6 GLONASS SLOT / FRQ #");
                                mFileWriter.newLine();
                                mFileWriter.write("    R09 -2 R10 -7 R11  0 R12 -1 R13 -2 R14 -7 R15  0 R16 -1 GLONASS SLOT / FRQ #");
                                mFileWriter.newLine();
                                mFileWriter.write("    R17  4 R18 -3 R19  3 R20  2 R21  4 R22 -3 R23  3 R24  2 GLONASS SLOT / FRQ #");
                                mFileWriter.newLine();
                                mFileWriter.write("                                                            END OF HEADER       ");
                                mFileWriter.newLine();

                            } else {
                                String StartTimeOBS = String.format("%6d%6d%6d%6d%6d%13.7f     %3s         TIME OF FIRST OBS\n", value.Y, value.M, value.D, value.h, value.m, value.s, "GPS");
                                //END OF HEADER
                                String ENDOFHEADER = String.format("%73s", "END OF HEADER");
                                mFileWriter.write(StartTimeOBS + ENDOFHEADER);
                                mFileWriter.newLine();
                            }

                            // 固定FullBiasNanos
                            if (gnssClock.hasFullBiasNanos() && gnssClock.hasBiasNanos()) {
                                // 获取本地硬件时钟与GPST的偏差
                                constFullBiasNanos = gnssClock.getFullBiasNanos() + gnssClock.getBiasNanos();
                            } else {
                                constFullBiasNanos = gnssClock.getFullBiasNanos();
                            }
                            firsttime = false;

                         }
                    }
                    else{
                        break;
                    }
                } catch (IOException e) {
                    Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                    logException(ERROR_WRITING_FILE, e);
                }
            }
            try {
                // 记录原始观测txt文件
                writeRawGnssMeasurementToFile(gnssClock, event);

                // 记录rienx文件
                writeGnssMeasurementToFile(gnssClock, event);

                // Gong: Timer for what???
                if(SettingsFragment.enableTimer){
                    if(true) {
                        SettingsFragment.timer = SettingsFragment.timer - 1;
                        getUiComponent().RefreshTimer();
                    }
                }
            } catch (IOException e){
                logException(ERROR_WRITING_FILE, e);
            }

        }
        firstOBSforAcc = true;
    }  // 计算传播时间，o文件头下部

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {}

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage) {
        if(SettingsFragment.ENABLE_RINEXNAVLOG){
            synchronized (mFileNavLock){
                if(mFileNavWriter == null){
                    return;
                }
                /*try {
                    if(RINEX_NAV_ION_OK == false) {
                        StringBuilder NAV_ION = new StringBuilder();
                        GnssNavigationConv mGnssNavigationConv = new GnssNavigationConv();
                        StringBuilder ION = mGnssNavigationConv.onNavMessageReported((byte) navigationMessage.getSvid(),(byte)navigationMessage.getType(),navigationMessage.getMessageId(),navigationMessage.getSubmessageId(),navigationMessage.getData());
                        if(ION != null && ION.toString().indexOf("null") == -1) {
                            NAV_ION.append(ION);
                            Log.d("NAV",ION.toString());
                            mFileNavWriter.write(NAV_ION.toString());
                            RINEX_NAV_ION_OK = true;
                        }
                    }
                }catch (IOException e){
                    Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                    logException(ERROR_WRITING_FILE, e);
                }*/
            }
        }
    }


    public void onRawSensorListener(String listener,float rawAcc[], float rawGyro[], float rawGrav[], float rawMag[],
                                 float rawRot[], float rawPre) {
        synchronized (mFileAccAzLock) {
            if (mFileAccAzWriter == null || !SettingsFragment.ENABLE_SENSORSLOG) {
                return;
            }
            else{
                if(listener == "") {
                    try {
                        // Log.e("Write","Writing Sensors Data");
                        Calendar myCal= Calendar.getInstance();
                        DateFormat myFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss.SSSSSS");
                        String myTime = myFormat.format(myCal.getTime());
                        //csv文件内容 行人位置模型 altitude是气压传感器
                        String SensorStream =
                                String.format("%s, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f\n",
                                        myTime,
                                        rawAcc[0], rawAcc[1], rawAcc[2], rawGyro[0], rawGyro[1], rawGyro[2],
                                        rawGrav[0], rawGrav[1], rawGrav[2], rawMag[0], rawMag[1], rawMag[2],
                                        rawRot[0], rawRot[1], rawRot[2], rawRot[3], rawPre);

                        //Log.e("Sensors", SensorStream);
                        mFileAccAzWriter.write(SensorStream);

//                        String day= String.format("%6d,%6d,%6d,%13.7f,\t",gnsstimeclock_a,gnsstimeclock_b,gnsstimeclock_c,gnsstimeclock_d,myName);
//                        mFileAccAzWriter.write(day);
//                        String time= String.format("%13.7f",myName);
//                        mFileAccAzWriter.write(myName);
//                        mFileAccAzWriter.newLine();

                    } catch (IOException e) {
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }
    }

    public void onSensorListener(String listener,float azimuth,float accZ,float altitude){
        synchronized (mFileAccAzLock) {
            if (mFileAccAzWriter == null || !SettingsFragment.ENABLE_SENSORSLOG) {
                return;
            }
            else{
                if(listener == "") {
                    try {
                        //Calendar myCal= Calendar.getInstance();
                        //DateFormat myFormat = new SimpleDateFormat("MM/dd/hh:mm.ss");
                        //String myName = myFormat.format(myCal.getTime());
                        //csv文件内容 行人位置模型 altitude是气压传感器
                        String SensorStream =
                                String.format("%f,%f,%f", (float) (accZ * Math.sin(azimuth)), (float) (accZ * Math.cos(azimuth)), altitude);
                        mFileAccAzWriter.write(SensorStream);
                        //String day= String.format("%6d,%6d,%6d,%13.7f,\t",gnsstimeclock_a,gnsstimeclock_b,gnsstimeclock_c,gnsstimeclock_d,myName);
                        //mFileAccAzWriter.write(day);
                        //String time= String.format("%13.7f",myName);
                        //mFileAccAzWriter.write(myName);
                        mFileAccAzWriter.newLine();
                    } catch (IOException e) {
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
    }

    @Override
    public void  onGnssStatusChanged(GnssStatus gnssStatus) {
        try {
            writeUseInFixArray(gnssStatus);
        }catch (IOException e){
            Toast.makeText(mContext, "FATAL_ERROR_FOR_WRITING_ARRAY", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNmeaReceived(long timestamp, String s) {
        synchronized (mFileNmeaLock) {
            if (mFileNmeaWriter == null) {
                return;
            }
            else{
                try {
                    String NmeaStream = String.format(Locale.US, "NMEA,%s,%d", s.trim(), timestamp);
                    mFileNmeaWriter.write(NmeaStream);
                    mFileNmeaWriter.newLine();
                }catch (IOException e){
                    Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                    logException(ERROR_WRITING_FILE, e);
                }
            }
        }
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {

    }

    private void writeUseInFixArray(GnssStatus gnssStatus) throws IOException{
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            if (gnssStatus.usedInFix(i) && gnssStatus.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS) {
                if (UsedInFixList.indexOf(gnssStatus.getSvid(i)) == -1) {
                    UsedInFixList.add(gnssStatus.getSvid(i));
                }
            } else if(gnssStatus.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS) {
                int index = UsedInFixList.indexOf(gnssStatus.getSvid(i));
                if (index != -1) {
                    UsedInFixList.remove(index);
                }
            }
        }
    }

    private boolean ReadUseInFixArray(int Svid) throws IOException{
        return UsedInFixList.indexOf(Svid) != -1;
    }

    private void writeRawGnssMeasurementToFile(GnssClock clock, GnssMeasurementsEvent event) throws IOException {
        try {
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
        } catch(IOException e){
            logException(ERROR_WRITING_FILE, e);
        }
    }

    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurementsEvent event) throws IOException {

    }

    private void logException(String errorMessage, Exception e) {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(GnssContainer.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link FileToDeleteFilter#mRetainedFiles}.
     */
    private static class FileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }

    private double GLONASSG1WAVELENGTH(int svid){
        return SPEED_OF_LIGHT/((1602 + GLONASSFREQ[svid - 1] * 0.5625) * 10e5);
    }
    private double GLONASSG2WAVELENGTH(int svid){
        return SPEED_OF_LIGHT/((1246 + GLONASSFREQ[svid - 1] * 0.4375) * 10e5);
    }

    // 北斗B1信号
    private double BEIDOUWAVELENGTH(int svid){
        return SPEED_OF_LIGHT/(1561.098 * 10e5);
    }

    // 从GPS周秒到GPS时的转换
    public static class ReturnValue {
        public int Y;
        public int M;
        public int D;
        public int h;
        public int m;
        public double s;
    }

    public static class GPSWStoGPST {
        public ReturnValue method(double GPSW , double GPSWS) {
            ReturnValue value = new ReturnValue();
            // MJD和MD的计算
            double MD = (int)(GPSWS/86400);
            double MJD = 44244+GPSW*7+MD;
            // 简化儒略日至年月日
            double JD = MJD + 2400000.5;
            double N = JD + 0.5;
            int Z = (int)N;
            double F = N - Z;
            double A;
            if(Z >= 2299161){
                int X = (int)((Z-1867216.25)/36524.25);
                A = Z + 1 + X - X/4;
            }
            else {
                A = Z;
            }
            double B = A + 1524;
            int C = (int)((B-122.1)/365.25);
            int K = (int)(365.25*C);
            int E = (int)((B-K)/30.6001);
            double D = B-K-(int)(30.6001*E)+F;
            int M;
            int Y;
            if(E < 13.5){
                M = E - 1;
            }
            else {
                M = E - 13;
            }
            if(M > 2.5){
                Y = C - 4716;
            }
            else{
                Y = C - 4715;
            }
            value.Y = Y;
            value.M = M;
            value.D = (int)D;

            //GPS周秒至GPS时分秒
            double DS = GPSWS-MD*86400;
            int h = (int)(DS/3600);
            double hm = DS-h*3600;
            int m = (int)(hm/60);
            double s = hm - m * 60;

            value.h = h;
            value.m = m;
            value.s = s;

            return value;

            }

    }

    @Override
    public void onTTFFReceived(long l) {}
}
