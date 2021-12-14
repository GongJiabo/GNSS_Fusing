package com.whu.gnss.gnsslogger;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Arrays;

public class CalSettingsActivity extends AppCompatActivity
{

    private static final String TAG = "CalPosActivity";
    //按钮
    private TextView textViewBtnSppModel;
    private TextView textViewBtnGnssSystem;
    private TextView textViewBtnNtripHost;
    private TextView textViewBtnNtripPort;
    private TextView textViewBtnNtripUserName;
    private TextView textViewBtnNtripPassWord;

    private TextView textViewBtnRestore;

    //文字框
    private TextView textViewSppModel;
    private TextView textViewGnssSystem;
    private TextView textViewNtripHost;
    private TextView textViewNtripPort;
    private TextView textViewNtripUserName;
    private TextView textViewNtripPassWord;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculate_pos);

        textViewBtnRestore = findViewById(R.id.sppsetting_btnRestore);

        textViewBtnSppModel = findViewById(R.id.sppsetting_btnSppModel);

        textViewBtnGnssSystem = findViewById(R.id.sppsetting_btnGnssSystem);

        textViewBtnNtripHost = findViewById(R.id.sppsetting_btnNtripHost);

        textViewBtnNtripPort = findViewById(R.id.sppsetting_btnNtripPort);

        textViewBtnNtripUserName = findViewById(R.id.sppsetting_btnNtripUserName);

        textViewBtnNtripPassWord = findViewById(R.id.sppsetting_btnNtripPassWord);


        textViewSppModel = findViewById(R.id.sppsetting_SppModel);

        textViewGnssSystem = findViewById(R.id.sppsetting_GnssSystem);

        textViewNtripHost = findViewById(R.id.sppsetting_NtripHost);

        textViewNtripPort = findViewById(R.id.sppsetting_NtripPort);

        textViewNtripUserName = findViewById(R.id.sppsetting_NtripUserName);

        textViewNtripPassWord = findViewById(R.id.sppsetting_NtripPassWord);

        sharedPreferences = getSharedPreferences(Constants.SPP_SETTING, 0);

        reloadSettingText();


        textViewBtnSppModel.setOnClickListener(v -> {
            Log.i(TAG, "Click -> 定位模式");
            String[] model = {
                    "伪距单点定位",
                    "伪距差分定位",
                    "伪距单点/差分定位",
                    "GNSS/PDR融合定位"
            };
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.sppmodle)
                    .items(model)
                    .itemsCallback(new MaterialDialog.ListCallback()
                    {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence)
                        {
                            sharedPreferences.edit().putInt(Constants.KEY_SPP_MODEL, i).apply();
                            textViewSppModel.setText(model[i]);
                        }
                    })
                    .show();
        });

        textViewBtnGnssSystem.setOnClickListener(v -> {
            Log.i(TAG, "Click -> 卫星系统");

            //当每一次进行选择时，先重置。
            restoreSettingText();

            String[] system = {
                    "GPS",
                    "GLO",
                    "GAL",
                    "BDS",
                    "QZSS"
            };
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.system_gnss)        // 标题
                    .items(system)              // 列表数据
                    // itemsCallbackMultiChoice 方法中的第一个参数代表预选项的值，没有预选项这个值就设置为 null，有预选项就传入一组预选项的索引值即可。
                    .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                            //如果使用 alwaysCallMultiChoiceCallback() 方法，在这边返回 false 将不允许新选择的单选按钮被选中。
                            return true;
                        }

                    })
                    // 如果没有使用 positiveText() 设置正面操作按钮，则当用户按下正面操作按钮时，
                    // 对话框将自动调用多项选择回调方法，该对话框也将自行关闭，除非关闭自动关闭。
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            Log.d(TAG, ":::" + Arrays.toString(dialog.getSelectedIndices()));
                            Integer[] index = dialog.getSelectedIndices();
                            textViewGnssSystem.setText("");
                            for (int i = 0; i < index.length; i++) {
                                switch (index[i]) {
                                    case 0:
                                        sharedPreferences.edit().putInt(Constants.KEY_GPS_SYSTEM, 1).apply();
                                        textViewGnssSystem.append("GPS");
                                        break;
                                    case 1:
                                        sharedPreferences.edit().putInt(Constants.KEY_GLO_SYSTEM, 1).apply();
                                        textViewGnssSystem.append("/GLO");
                                        break;
                                    case 2:
                                        sharedPreferences.edit().putInt(Constants.KEY_GAL_SYSTEM, 1).apply();
                                        textViewGnssSystem.append("/GAL");
                                        break;
                                    case 3:
                                        sharedPreferences.edit().putInt(Constants.KEY_BDS_SYSTEM, 1).apply();
                                        textViewGnssSystem.append("/BDS");
                                        break;
                                    case 4:
                                        sharedPreferences.edit().putInt(Constants.KEY_QZSS_SYSTEM, 1).apply();
                                        textViewGnssSystem.append("/QZSS");
                                        break;
                                }
                            }
                        }
                    })
                    .positiveText("确认")
                    // 如果调用 alwaysCallMultiChoiceCallback() 该方法，则每次用户选择/取消项目时都会调用多项选择回调方法。
                    .alwaysCallMultiChoiceCallback()
                    .show();// 显示对话框

        });

        textViewBtnNtripHost.setOnClickListener(v -> {
            Log.i(TAG, "Click -> host");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.host)
                    .inputRangeRes(0, 30, R.color.colorDanger)
                    .input(getString(R.string.host),
                            sharedPreferences.getString(Constants.KEY_NTRIP_HOST, Constants.DEF_NTRIP_HOST),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_NTRIP_HOST, input.toString()).apply();
                                textViewNtripHost.setText(input.toString());
                            }).show();
        });
        textViewBtnNtripPort.setOnClickListener(v -> {
            Log.i(TAG, "Click -> port");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.port)
                    .inputRangeRes(0, 30, R.color.colorDanger)
                    .input(getString(R.string.port),
                            sharedPreferences.getString(Constants.KEY_NTRIP_PORT, Constants.DEF_NTRIP_PORT),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_NTRIP_PORT, input.toString()).apply();
                                textViewNtripPort.setText(input.toString());
                            }).show();
        });
        textViewBtnNtripUserName.setOnClickListener(v -> {
            Log.i(TAG, "Click -> username");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.username)
                    .inputRangeRes(0, 30, R.color.colorDanger)
                    .input(getString(R.string.username),
                            sharedPreferences.getString(Constants.KEY_NTRIP_USERNAME, Constants.DEF_NTRIP_USERNAME),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_NTRIP_USERNAME, input.toString()).apply();
                                textViewNtripUserName.setText(input.toString());
                            }).show();
        });

        textViewBtnNtripPassWord.setOnClickListener(v -> {
            Log.i(TAG, "Click -> password");
            new MaterialDialog.Builder(v.getContext())
                    .title(R.string.password)
                    .inputRangeRes(0, 30, R.color.colorDanger)
                    .input(getString(R.string.password),
                            sharedPreferences.getString(Constants.KEY_NTRIP_PASSWORD, Constants.DEF_NTRIP_PASSWARD),
                            (dialog, input) -> {
                                sharedPreferences.edit().putString(Constants.KEY_NTRIP_PASSWORD, input.toString()).apply();
                                textViewNtripPassWord.setText(input.toString());
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
        int model = sharedPreferences.getInt(Constants.KEY_SPP_MODEL, Constants.DEF_SPP_MODEL);
        switch (model) {
            case 0:
                textViewSppModel.setText("伪距单点定位");
                break;
            case 1:
                textViewSppModel.setText("伪距差分定位");
                break;
            case 2:
                textViewSppModel.setText("伪距单点/差分定位");
        }
        //默认状态下未进行卫星系统的选择
        textViewGnssSystem.setText("");
        int gps = sharedPreferences.getInt(Constants.KEY_GPS_SYSTEM, Constants.DEF_GPS_SYSTEM);
        if (gps == 1) textViewGnssSystem.append("GPS");
        int gal = sharedPreferences.getInt(Constants.KEY_GAL_SYSTEM, Constants.DEF_GAL_SYSTEM);

        if (gal == 1) textViewGnssSystem.append("/GAL");
        int glo = sharedPreferences.getInt(Constants.KEY_GLO_SYSTEM, Constants.DEF_GLO_SYSTEM);
        if (glo == 1) textViewGnssSystem.append("/GLO");
        int bds = sharedPreferences.getInt(Constants.KEY_BDS_SYSTEM, Constants.DEF_BDS_SYSTEM);
        if (bds == 1) textViewGnssSystem.append("/BDS");
        int qzss = sharedPreferences.getInt(Constants.KEY_QZSS_SYSTEM, Constants.DEF_QZSS_SYSTEM);
        if (qzss == 1) textViewGnssSystem.append("/QZSS");

        textViewNtripHost.setText(sharedPreferences.getString(Constants.KEY_NTRIP_HOST, Constants.DEF_NTRIP_HOST));
        textViewNtripPort.setText(sharedPreferences.getString(Constants.KEY_NTRIP_PORT, Constants.DEF_NTRIP_PORT));
        textViewNtripUserName.setText(sharedPreferences.getString(Constants.KEY_NTRIP_USERNAME, Constants.DEF_NTRIP_USERNAME));
        textViewNtripPassWord.setText(sharedPreferences.getString(Constants.KEY_NTRIP_PASSWORD, Constants.DEF_NTRIP_PASSWARD));

    }

    //重置sharedPreferences
    private void restoreSettingText() {
        sharedPreferences.edit().putInt(Constants.KEY_GPS_SYSTEM, Constants.DEF_GPS_SYSTEM).apply();
        sharedPreferences.edit().putInt(Constants.KEY_GAL_SYSTEM, Constants.DEF_GAL_SYSTEM).apply();
        sharedPreferences.edit().putInt(Constants.KEY_GLO_SYSTEM, Constants.DEF_GLO_SYSTEM).apply();
        sharedPreferences.edit().putInt(Constants.KEY_BDS_SYSTEM, Constants.DEF_BDS_SYSTEM).apply();
        sharedPreferences.edit().putInt(Constants.KEY_QZSS_SYSTEM, Constants.DEF_QZSS_SYSTEM).apply();

        sharedPreferences.edit().putInt(Constants.KEY_SPP_MODEL, Constants.DEF_SPP_MODEL).apply();
        sharedPreferences.edit().putString(Constants.KEY_NTRIP_HOST, Constants.DEF_NTRIP_HOST).apply();
        sharedPreferences.edit().putString(Constants.KEY_NTRIP_PORT, Constants.DEF_NTRIP_PORT).apply();
        sharedPreferences.edit().putString(Constants.KEY_NTRIP_USERNAME, Constants.DEF_NTRIP_USERNAME).apply();
        sharedPreferences.edit().putString(Constants.KEY_NTRIP_PASSWORD, Constants.DEF_NTRIP_PASSWARD).apply();
    }

}
