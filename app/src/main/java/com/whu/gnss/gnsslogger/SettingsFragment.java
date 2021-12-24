package com.whu.gnss.gnsslogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.widget.Button;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.location.GnssMeasurementsEvent.Callback.STATUS_LOCATION_DISABLED;
import static android.location.GnssMeasurementsEvent.Callback.STATUS_NOT_SUPPORTED;
import static android.location.GnssMeasurementsEvent.Callback.STATUS_READY;

import com.whu.gnss.gnsslogger.rinexFileLogger.Rinex;

import org.w3c.dom.Text;

/**
 * The UI fragment showing a set of configurable settings for the client to request GPS data.
 */
public class SettingsFragment extends Fragment {

    // is support GNSS RawMeasurement
    private TextView mRawDataIsOk;
    private TextView mLocationIsOk;

    // Button
    private Button BtnRineXSettings;
    private Button BtnResSettings;

    // Label under Sensors' TextView("Unavailable/available" on left&bottom side)
    private TextView mAccSpecView;
    private TextView mGyroSpecView;
    private TextView mMagSpecView;
    private TextView mPressSpecView;

    // Label offside Sensors' TextView("Unavailable/available" on right side)
    private TextView mAccAvView;
    private TextView mGyroAvView;
    private TextView mMagAvView;
    private TextView mPressAvView;


    public static final String TAG = ":SettingsFragment";
    public static String SAVE_LOCATION = "GNSS_FUSING";
    public static String FILE_PREFIX = "/" + SAVE_LOCATION + "/RINEXOBS";
    public static String FILE_PREFIXSUB = "/" + SAVE_LOCATION + "/KML";
    public static String FILE_PREFIXACCAZI = "/" + SAVE_LOCATION + "/CSV";
    public static String FILE_PREFIXNMEA = "/" + SAVE_LOCATION + "/NMEA";
    public static String FILE_PREFIXNAV = "/" + SAVE_LOCATION + "/RINEXNAV";
    public static String FILE_PREFIXRAW = "/" + SAVE_LOCATION +"/RAWDATA";
    public static String FILE_NAME = "AndroidOBS";

    public static boolean usePseudorangeSmoother = false;
    public static boolean usePseudorangeRate = false;
    public static boolean useDeviceSensor = false;

    public static boolean GNSSClockSync = false;

    public static boolean SMOOTHER_RATE_RESET_FLAG_FILE = false;
    public static boolean SMOOTHER_RATE_RESET_FLAG_UI = false;

    public static boolean SendMode = false;
    public static int GNSSMeasurementReadyMode = 10;


    private SensorContainer mSensorContainer;
    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private GnssContainer mGpsContainer;
    private GnssContainer mGnssContainer;

    private TextView EditSaveLocation;
    private TextView FTPDirectory;

    public static boolean FIRST_CHECK = false;
    public static String FTP_SERVER_DIRECTORY = "";

    public static boolean PermissionOK = false;
    public static boolean registerGNSS = false;
    public static boolean FileLoggingOK = false;

    // 是否正在记录
    public static boolean EnableLogging = false;

    // 记录checkbox
    public static boolean ENABLE_RINEXOBSLOG = true;
    public static boolean ENABLE_RESLOG      = false;
    public static boolean ENABLE_RINEXNAVLOG = false;
    public static boolean ENABLE_KMLLOG      = false;
    public static boolean ENABLE_NMEALOG     = false;
    public static boolean ENABLE_SENSORSLOG  = false;
    public static boolean ENABLE_RAWDATALOG  = false;


    // 观测时间
    public static int timer = 0;
    public static boolean enableTimer = false;
    public static int interval = 1;

    private final SettingsFragment.UIFragmentSettingComponent mUiSettingComponent = new SettingsFragment.UIFragmentSettingComponent();

    public void setSensorContainer(SensorContainer value){
        mSensorContainer = value;
    }

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    public void setGpsContainer(GnssContainer value) {
        mGpsContainer = value;
    }

    public void setGnssContainer(GnssContainer value){
        mGnssContainer = value;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false /* attachToRoot */);
    }

    public void onViewCreated(View view, Bundle savedInstanceState){

        // can device support
        mRawDataIsOk = (TextView) view.findViewById(R.id.rawDataIsOk);
        mLocationIsOk = (TextView) view.findViewById(R.id.locationIsOk);

        // button for RinexObs/ResPos setting
        BtnRineXSettings = (Button) view.findViewById(R.id.buttonRinexSetting);
        BtnResSettings = (Button) view.findViewById(R.id.buttonResSetting);

        BtnRineXSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Log.i(TAG, "Click -> Setting (RinexSettings)");
                startActivity(
                        new Intent(getActivity(), RinexSettingsActivity.class));
            }
            });
        BtnResSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Log.i(TAG, "Click -> Setting (ResSettings)");
                startActivity(
                        new Intent(getActivity(), CalSettingsActivity.class));
            }
        });

        // Label under Sensors' TextView("Unavailable/available" on left&bottom side)
        mAccSpecView = (TextView) view.findViewById(R.id.accSpecView);
        mGyroSpecView = (TextView) view.findViewById(R.id.gyroSpecView);
        mMagSpecView = (TextView) view.findViewById(R.id.magSpecView);
        mPressSpecView = (TextView) view.findViewById(R.id.pressSpecView);

        // Label offside Sensors' TextView("Unavailable/available" on right side)
        mAccAvView = (TextView) view.findViewById(R.id.accAvView);
        mGyroAvView = (TextView) view.findViewById(R.id.gyroAvView);
        mMagAvView = (TextView) view.findViewById(R.id.magAvView);
        mPressAvView = (TextView) view.findViewById(R.id.pressAvView);

        // files' name
        EditSaveLocation = (TextView) view.findViewById(R.id.editSaveLocation);
        EditSaveLocation.setText("(Current Time)");
        EditSaveLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString() == ""){
                    EditSaveLocation.setText("(Current Time)");
                }else{
                    FILE_NAME = s.toString();
                }
            }
        });

        // output files(get year eg: 21.o/n)
        Date now = new Date();
        int observation = now.getYear() - 100;

        final TextView FileExtension = (TextView) view.findViewById(R.id.textViewRinexTxt);
        FileExtension.setText("$log/RINEX/\"prefix\"." + observation + "o");

        final TextView FileExtensionNav = (TextView) view.findViewById(R.id.textViewNaviTxt);
        FileExtensionNav.setText("$log/RINEX/\"prefix\"." + observation + "n");

        final TextView FileExtensionPos = (TextView) view.findViewById(R.id.textViewResTxt);
        FileExtensionPos.setText("$log/RES/\"prefix\"." + observation + "pos");

        final Switch registerSensor = (Switch) view.findViewById(R.id.register_sensor);
        registerSensor.setChecked(false);
        registerSensor.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mSensorContainer.registerSensor();
                            useDeviceSensor = true;
                        } else {
                            mSensorContainer.unregisterSensor();
                            useDeviceSensor = false;
                            // ???
                            Logger2Fragment.deviceAzimuth = 0;
                        }
                    }
                });

        // Get outfile types
        final  CheckBox resCheckBox = (CheckBox)  view.findViewById(R.id.outputRes);
        resCheckBox.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                ENABLE_RESLOG = resCheckBox.isChecked();
            }
        });

        final CheckBox naviCheckBox = (CheckBox) view.findViewById(R.id.outputNAVI);
        naviCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ENABLE_RINEXNAVLOG = naviCheckBox.isChecked();
            }
        });

        final CheckBox kmlCheckBox = (CheckBox) view.findViewById(R.id.outputKML);
        kmlCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ENABLE_KMLLOG = kmlCheckBox.isChecked();
            }
        });

        final CheckBox nmeaCheckBox = (CheckBox) view.findViewById(R.id.outputNmea);
        nmeaCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ENABLE_NMEALOG = nmeaCheckBox.isChecked();
            }
        });

        final CheckBox sensorCheckBox = (CheckBox) view.findViewById(R.id.outputSensors);
        sensorCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ENABLE_SENSORSLOG = sensorCheckBox.isChecked();
            }
        });

        final CheckBox rawDataCheckBox = (CheckBox) view.findViewById(R.id.outputRaw);
        rawDataCheckBox.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                ENABLE_RAWDATALOG = rawDataCheckBox.isChecked();
            }
        });


        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            Log.d("mUILogger","Pointer OK");
            currentUiLogger.setUISettingComponent(mUiSettingComponent);
        }

        GnssContainer currentGnssContainer = mGnssContainer;
        if(currentGnssContainer != null){
            currentGnssContainer.setUISettingComponent(mUiSettingComponent);
        }

        // can device support(seem no sense?)
        if(FIRST_CHECK == false) {
            CheckGNSSMeasurementsReady(GNSSMeasurementReadyMode, FIRST_CHECK);
            FIRST_CHECK = true;
        }else{
            CheckGNSSMeasurementsReady(GNSSMeasurementReadyMode, FIRST_CHECK);
        }
    }

    private void CheckGNSSMeasurementsReady(int status, boolean FIRST_CHECK){
        if(status == STATUS_NOT_SUPPORTED){
            if(FIRST_CHECK == false) {
                new AlertDialog.Builder(getContext())
                        .setTitle("DEVICE NOT SUPPORTED")
                        .setMessage("This device is not suppored please check supported device list\nhttps://developer.android.com/guide/topics/sensors/gnss.html")
                        .setPositiveButton("OK", null)
                        .show();
            }
            MainActivity.getInstance().finishAndRemoveTask();
            mRawDataIsOk.setText("Unavailable");
        }
        if(status == STATUS_LOCATION_DISABLED){
            if(FIRST_CHECK == false) {
                new AlertDialog.Builder(getContext())
                        .setTitle("LOCATION DISABLED")
                        .setMessage("Location is disabled. \nplease turn on your GPS Setting")
                        .setPositiveButton("OK", null)
                        .show();
            }
            mLocationIsOk.setText("Unavailable");
        }
        if(status == STATUS_READY){
            Log.d("GNSSStatus","GNSSMeasurements Status Ready");
            Toast.makeText(getContext(),"GNSS Measurements Ready",Toast.LENGTH_SHORT).show();
        }
    }


    //UI界面设置
    public class UIFragmentSettingComponent {

        //private static final int MAX_LENGTH = 12000;
        //private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);
        public synchronized void SettingFTPDirectory(final String DirectoryName){
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                        //FTPDirectory.setText(DirectoryName);
                        }
                    });
        }

        public synchronized void SettingTextFragment(final String FileName) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(EditSaveLocation.getText().toString().indexOf("Current Time") != -1){
                                FILE_NAME = FileName;
                            }
                        }
                    });
        }

        /*public synchronized void Lockout(final boolean status) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            EditSaveLocation.setEnabled(status);

                        }
                    });
        }*/

        public synchronized void SettingFragmentSensorSpec(final String SensorSpec[]) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mAccSpecView.setText(SensorSpec[0]);
                            mGyroSpecView.setText(SensorSpec[1]);
                            mMagSpecView.setText(SensorSpec[2]);
                            mPressSpecView.setText(SensorSpec[3]);
                        }
                    });
        }

        public synchronized void SettingFragmentSensorAvairable(final String SensorAvairable[]) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mAccAvView.setText(SensorAvairable[0]);
                            mGyroAvView.setText(SensorAvairable[1]);
                            mMagAvView.setText(SensorAvairable[2]);
                            mPressAvView.setText(SensorAvairable[3]);
                        }
                    });
        }


        /*public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }*/


    }
}