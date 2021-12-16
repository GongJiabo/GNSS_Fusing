package com.whu.gnss.gnsslogger;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import com.afollestad.materialdialogs.MaterialDialog;


public class RinexSettingsActivity extends AppCompatActivity {

    private static final String TAG = RinexSettingsActivity.class.getSimpleName();

    private RadioButton radioBtnRINEX211;
    private RadioButton radioBtnRINEX303;

    private TextView textViewMarkName;
    private TextView textViewMarkType;
    private TextView textViewObserverName;
    private TextView textViewObserverAgencyName;
    private TextView textViewReceiverNumber;
    private TextView textViewReceiverType;
    private TextView textViewReceiverVersion;
    private TextView textViewAntennaNumber;
    private TextView textViewAntennaType;
    private TextView textViewAntennaEccentricityEast;
    private TextView textViewAntennaEccentricityNorth;
    private TextView textViewAntennaHeight;

    private TextView textViewBtnMarkName;
    private TextView textViewBtnMarkType;
    private TextView textViewBtnObserverName;
    private TextView textViewBtnObserverAgencyName;
    private TextView textViewBtnReceiverNumber;
    private TextView textViewBtnReceiverType;
    private TextView textViewBtnReceiverVersion;
    private TextView textViewBtnAntennaNumber;
    private TextView textViewBtnAntennaType;
    private TextView textViewBtnAntennaEccentricityEast;
    private TextView textViewBtnAntennaEccentricityNorth;
    private TextView textViewBtnAntennaHeight;

    private TextView textViewBtnRestore;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rinex_settings);

        radioBtnRINEX211 = findViewById(R.id.RINEXMODE211);
        radioBtnRINEX303 = findViewById(R.id.RINEXMODE303);

        textViewMarkName = findViewById(R.id.setting_markName);
        textViewMarkType = findViewById(R.id.setting_markType);
        textViewObserverName = findViewById(R.id.setting_observerName);
        textViewObserverAgencyName = findViewById(R.id.setting_observerAgencyName);
        textViewReceiverNumber = findViewById(R.id.setting_receiverNumber);
        textViewReceiverType = findViewById(R.id.setting_receiverType);
        textViewReceiverVersion = findViewById(R.id.setting_receiverVersion);
        textViewAntennaNumber = findViewById(R.id.setting_antennaNumber);
        textViewAntennaType = findViewById(R.id.setting_antennaType);
        textViewAntennaEccentricityEast = findViewById(R.id.setting_antennaEccentricityEast);
        textViewAntennaEccentricityNorth = findViewById(R.id.setting_antennaEccentricityNorth);
        textViewAntennaHeight = findViewById(R.id.setting_antennaHeight);
        textViewBtnMarkName = findViewById(R.id.setting_btnMarkName);
        textViewBtnMarkType = findViewById(R.id.setting_btnMarkType);
        textViewBtnObserverName = findViewById(R.id.setting_btnObserverName);
        textViewBtnObserverAgencyName = findViewById(R.id.setting_btnObserverAgencyName);
        textViewBtnReceiverNumber = findViewById(R.id.setting_btnReceiverNumber);
        textViewBtnReceiverType = findViewById(R.id.setting_btnReceiverType);
        textViewBtnReceiverVersion = findViewById(R.id.setting_btnReceiverVersion);
        textViewBtnAntennaNumber = findViewById(R.id.setting_btnAntennaNumber);
        textViewBtnAntennaType = findViewById(R.id.setting_btnAntennaType);
        textViewBtnAntennaEccentricityEast = findViewById(R.id.setting_btnAntennaEccentricityEast);
        textViewBtnAntennaEccentricityNorth = findViewById(R.id.setting_btnAntennaEccentricityNorth);
        textViewBtnAntennaHeight = findViewById(R.id.setting_btnAntennaHeight);
        textViewBtnRestore = findViewById(R.id.setting_btnRestore);

        sharedPreferences = getSharedPreferences(Constants.RINEX_SETTING, 0);

        reloadSettingText();

        radioBtnRINEX211.setChecked(false);
        radioBtnRINEX303.setChecked(true);
        radioBtnRINEX211.setOnClickListener(v -> {
            radioBtnRINEX211.setChecked(true);
            radioBtnRINEX303.setChecked(false);
        });
        radioBtnRINEX303.setOnClickListener(v -> {
            radioBtnRINEX211.setChecked(false);
            radioBtnRINEX303.setChecked(true);
        });

        textViewBtnMarkName.setOnClickListener(v -> {
            Log.i(TAG, "Click -> MarkName");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.mark_name)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.mark_name),
                            sharedPreferences.getString(Constants.KEY_MARK_NAME, Constants.DEF_MARK_NAME),
                            (dialog, input) -> {
                                Log.i(TAG, "Marker input = " + input);
                                sharedPreferences.edit().putString(Constants.KEY_MARK_NAME, input.toString()).apply();
                                textViewMarkName.setText(input.toString());
                            }).show();
        });

        textViewBtnMarkType.setOnClickListener(v -> {
            Log.i(TAG, "Click -> MarkType");
            String[] type = {
                    "Geodetic",
                    "Non Geodetic",
                    "Non Physical",
                    "Space borne",
                    "Air borne",
                    "Water Craft",
                    "Ground Craft",
                    "Fixed Buoy",
                    "Floating Buoy",
                    "Floating Ice",
                    "Glacier",
                    "Ballistic",
                    "Animal",
                    "Human"
            };
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.mark_type)
                    .items(type)
                    .itemsCallback((dialog, view, which, text) -> {
                        sharedPreferences.edit().putString(Constants.KEY_MARK_TYPE, text.toString()).apply();
                        textViewMarkType.setText(text.toString());
                    })
                    .show();
        });


        textViewBtnObserverName.setOnClickListener(v -> {
            Log.i(TAG, "Click -> ObserverName");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.observer_name)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.observer_name),
                            sharedPreferences.getString(Constants.KEY_OBSERVER_NAME, Constants.DEF_OBSERVER_NAME),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_OBSERVER_NAME, input.toString()).apply();
                                textViewObserverName.setText(input.toString());
                            }).show();
        });

        textViewBtnObserverAgencyName.setOnClickListener(v -> {
            Log.i(TAG, "Click -> ObserverAgencyName");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.observer_agency_name)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.observer_agency_name),
                            sharedPreferences.getString(Constants.KEY_OBSERVER_AGENCY_NAME, Constants.DEF_OBSERVER_AGENCY_NAME),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_OBSERVER_AGENCY_NAME, input.toString()).apply();
                                textViewObserverAgencyName.setText(input.toString());
                            }).show();
        });

        textViewBtnReceiverNumber.setOnClickListener(v -> {
            Log.i(TAG, "Click -> ReceiverNumber");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.receiver_number)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.receiver_number),
                            sharedPreferences.getString(Constants.KEY_RECEIVER_NUMBER, Constants.DEF_RECEIVER_NUMBER),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_RECEIVER_NUMBER, input.toString()).apply();
                                textViewReceiverNumber.setText(input.toString());
                            }).show();
        });

        textViewBtnReceiverType.setOnClickListener(v -> {
            Log.i(TAG, "Click -> ReceiverType");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.receiver_type)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.receiver_type),
                            sharedPreferences.getString(Constants.KEY_RECEIVER_TYPE, Constants.DEF_RECEIVER_TYPE),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_RECEIVER_TYPE, input.toString()).apply();
                                textViewReceiverType.setText(input.toString());
                            }).show();
        });

        textViewBtnReceiverVersion.setOnClickListener(v -> {
            Log.i(TAG, "Click -> ReceiverVersion");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.receiver_version)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.receiver_version),
                            sharedPreferences.getString(Constants.KEY_RECEIVER_VERSION, Constants.DEF_RECEIVER_VERSION),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_RECEIVER_VERSION, input.toString()).apply();
                                textViewReceiverVersion.setText(input.toString());
                            }).show();
        });

        textViewBtnAntennaNumber.setOnClickListener(v -> {
            Log.i(TAG, "Click -> AntennaNumber");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.antenna_number)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.antenna_number),
                            sharedPreferences.getString(Constants.KEY_ANTENNA_NUMBER, Constants.DEF_ANTENNA_NUMBER),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_ANTENNA_NUMBER, input.toString()).apply();
                                textViewAntennaNumber.setText(input.toString());
                            }).show();
        });

        textViewBtnAntennaType.setOnClickListener(v -> {
            Log.i(TAG, "Click -> AntennaType");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.antenna_type)
                    .inputRangeRes(0, 20, R.color.colorDanger)
                    .input(getString(R.string.antenna_type),
                            sharedPreferences.getString(Constants.KEY_ANTENNA_TYPE, Constants.DEF_ANTENNA_TYPE),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_ANTENNA_TYPE, input.toString()).apply();
                                textViewAntennaType.setText(input.toString());
                            }).show();
        });

        textViewBtnAntennaEccentricityEast.setOnClickListener(v -> {
            Log.i(TAG, "Click -> AntennaEccentricityEast");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.antenna_eccentricity_east)
                    .inputRangeRes(0, 6, R.color.colorDanger)
                    .input(getString(R.string.antenna_eccentricity_east),
                            sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_EAST, Constants.DEF_ANTENNA_ECCENTRICITY_EAST),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_ANTENNA_ECCENTRICITY_EAST, input.toString()).apply();
                                textViewAntennaEccentricityEast.setText(input.toString());
                            }).show();
        });

        textViewBtnAntennaEccentricityNorth.setOnClickListener(v -> {
            Log.i(TAG, "Click -> AntennaEccentricityNorth");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.antenna_eccentricity_north)
                    .inputRangeRes(0, 6, R.color.colorDanger)
                    .input(getString(R.string.antenna_eccentricity_north),
                            sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_NORTH, Constants.DEF_ANTENNA_ECCENTRICITY_NORTH),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_ANTENNA_ECCENTRICITY_NORTH, input.toString()).apply();
                                textViewAntennaEccentricityNorth.setText(input.toString());
                            }).show();
        });

        textViewBtnAntennaHeight.setOnClickListener(v -> {
            Log.i(TAG, "Click -> AntennaHeight");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.antenna_height)
                    .inputRangeRes(0, 6, R.color.colorDanger)
                    .input(getString(R.string.antenna_height),
                            sharedPreferences.getString(Constants.KEY_ANTENNA_HEIGHT, Constants.DEF_ANTENNA_HEIGHT),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_ANTENNA_HEIGHT, input.toString()).apply();
                                textViewAntennaHeight.setText(input.toString());
                            }).show();
        });

        textViewBtnRestore.setOnClickListener(v -> {
            Log.i(TAG, "Click -> Restore");

            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.restore_defaults)
                    .content(R.string.please_confirm)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive((dialog, which) -> {
                        restoreSettingText();
                        reloadSettingText();
                    })
                    .show();
        });

    }

    private void reloadSettingText() {
        textViewMarkName.setText(sharedPreferences.getString(Constants.KEY_MARK_NAME, Constants.DEF_MARK_NAME));
        textViewMarkType.setText(sharedPreferences.getString(Constants.KEY_MARK_TYPE, Constants.DEF_MARK_TYPE));
        textViewObserverName.setText(sharedPreferences.getString(Constants.KEY_OBSERVER_NAME, Constants.DEF_OBSERVER_NAME));
        textViewObserverAgencyName.setText(sharedPreferences.getString(Constants.KEY_OBSERVER_AGENCY_NAME, Constants.DEF_OBSERVER_AGENCY_NAME));
        textViewReceiverNumber.setText(sharedPreferences.getString(Constants.KEY_RECEIVER_NUMBER, Constants.DEF_RECEIVER_NUMBER));
        textViewReceiverType.setText(sharedPreferences.getString(Constants.KEY_RECEIVER_TYPE, Constants.DEF_RECEIVER_TYPE));
        textViewReceiverVersion.setText(sharedPreferences.getString(Constants.KEY_RECEIVER_VERSION, Constants.DEF_RECEIVER_VERSION));
        textViewAntennaNumber.setText(sharedPreferences.getString(Constants.KEY_ANTENNA_NUMBER, Constants.DEF_ANTENNA_NUMBER));
        textViewAntennaType.setText(sharedPreferences.getString(Constants.KEY_ANTENNA_TYPE, Constants.DEF_ANTENNA_TYPE));
        textViewAntennaEccentricityEast.setText(sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_EAST, Constants.DEF_ANTENNA_ECCENTRICITY_EAST));
        textViewAntennaEccentricityNorth.setText(sharedPreferences.getString(Constants.KEY_ANTENNA_ECCENTRICITY_NORTH, Constants.DEF_ANTENNA_ECCENTRICITY_NORTH));
        textViewAntennaHeight.setText(sharedPreferences.getString(Constants.KEY_ANTENNA_HEIGHT, Constants.DEF_ANTENNA_HEIGHT));
    }

    private void restoreSettingText() {
        sharedPreferences.edit().putString(Constants.KEY_MARK_NAME, Constants.DEF_MARK_NAME).apply();
        sharedPreferences.edit().putString(Constants.KEY_MARK_TYPE, Constants.DEF_MARK_TYPE).apply();
        sharedPreferences.edit().putString(Constants.KEY_OBSERVER_NAME, Constants.DEF_OBSERVER_NAME).apply();
        sharedPreferences.edit().putString(Constants.KEY_OBSERVER_AGENCY_NAME, Constants.DEF_OBSERVER_AGENCY_NAME).apply();
        sharedPreferences.edit().putString(Constants.KEY_RECEIVER_NUMBER, Constants.DEF_RECEIVER_NUMBER).apply();
        sharedPreferences.edit().putString(Constants.KEY_RECEIVER_TYPE, Constants.DEF_RECEIVER_TYPE).apply();
        sharedPreferences.edit().putString(Constants.KEY_RECEIVER_VERSION, Constants.DEF_RECEIVER_VERSION).apply();
        sharedPreferences.edit().putString(Constants.KEY_ANTENNA_NUMBER, Constants.DEF_ANTENNA_NUMBER).apply();
        sharedPreferences.edit().putString(Constants.KEY_ANTENNA_TYPE, Constants.DEF_ANTENNA_TYPE).apply();
        sharedPreferences.edit().putString(Constants.KEY_ANTENNA_ECCENTRICITY_EAST, Constants.DEF_ANTENNA_ECCENTRICITY_EAST).apply();
        sharedPreferences.edit().putString(Constants.KEY_ANTENNA_ECCENTRICITY_NORTH, Constants.DEF_ANTENNA_ECCENTRICITY_NORTH).apply();
        sharedPreferences.edit().putString(Constants.KEY_ANTENNA_HEIGHT, Constants.DEF_ANTENNA_HEIGHT).apply();
    }
}
