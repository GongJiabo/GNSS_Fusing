package com.whu.gnss.gnsslogger;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.sax.TextElementListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapWrapper;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.model.MyLocationStyle;
import com.whu.gnss.gnsslogger.mapview.MAWebViewWrapper;
import com.whu.gnss.gnsslogger.mapview.MyWebView;



/**
 * AMapV2地图中介绍使用active deactive进行定位
 */
public class GetLoctionMode extends Activity implements LocationSource,
        AMapLocationListener,OnCheckedChangeListener {
    private AMap aMap;
    private AMapWrapper aMapWrapper;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private RadioGroup mGPSModeGroup;

    private TextView mLocationErrText;

    private MyLocationStyle myLocationStyle;

    private Button btnBasicMap;
    private Button btnSatelliteMap;
    private Button btnNightMap;

    private TextView txtViewLat;
    private TextView txtViewLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏

        // 设置定位key，正常建议在manifest中共设置
        AMapLocationClient.setApiKey("a2cc81969a0cb2f0bf70c440d09f648c");

        setContentView(R.layout.locationmodesource_outlocation_activity);
        MyWebView myWebView = findViewById(R.id.map);
        MAWebViewWrapper webViewWrapper = new MAWebViewWrapper(myWebView);
        aMapWrapper = new AMapWrapper(this, webViewWrapper);
        aMapWrapper.onCreate();

        init();
        aMapWrapper.getMapAsyn(new AMap.OnMapReadyListener() {
            @Override
            public void onMapReady(AMap map) {
                aMap = map;
            }
        });



        btnBasicMap = (Button)findViewById(R.id.basicmap);
        btnSatelliteMap = (Button)findViewById(R.id.rsmap);
        btnNightMap= (Button)findViewById(R.id.nightmap);

        txtViewLat = (TextView)findViewById(R.id.txtLatitude);
        txtViewLon = (TextView)findViewById(R.id.txtLogtitude);

        btnBasicMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
            }
        });
        btnSatelliteMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);// 矢量地图模式
            }
        });
        btnNightMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);// 矢量地图模式
            }
        });
    }

    /**
     * 初始化
     */
    private void init() {

        mGPSModeGroup = (RadioGroup) findViewById(R.id.gps_radio_group);
        mGPSModeGroup.setOnCheckedChangeListener(this);
        mLocationErrText = (TextView)findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMapLocation() {
        if (myLocationStyle == null) {
            // 如果要设置定位的默认状态，可以在此处进行设置
            myLocationStyle = new MyLocationStyle();
            aMap.setMyLocationStyle(myLocationStyle);

            aMap.setLocationSource(this);// 设置定位监听
            aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false

        }

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        if (aMap == null) {
            return;
        }

        Location location = aMap.getMyLocation();
        MyLocationStyle locationStyle = aMap.getMyLocationStyle();

        Log.e("mapcore","location " + (location == null ? "null" : location.getLatitude()));
        Log.e("mapcore","locationStyle " + (locationStyle == null ? "null" : locationStyle.getMyLocationType()));

        setUpMapLocation();

        if(mLocationOption == null || mlocationClient == null) {
            return;
        }
        switch (checkedId) {
            case R.id.gps_locate_button:
                // 设置定位的类型为定位模式
                myLocationStyle = new MyLocationStyle();
                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
                aMap.setMyLocationStyle(myLocationStyle);

                mLocationOption.setOnceLocation(true);
                mlocationClient.setLocationOption(mLocationOption);
                mlocationClient.startLocation();

                break;
            case R.id.gps_follow_button:
                // 设置定位的类型为 跟随模式
                myLocationStyle = new MyLocationStyle();
                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);
                aMap.setMyLocationStyle(myLocationStyle);

                mLocationOption.setOnceLocation(false);
                mlocationClient.setLocationOption(mLocationOption);
                mlocationClient.startLocation();
                break;
            case R.id.gps_rotate_button:
                // 设置定位的类型为根据地图面向方向旋转
                myLocationStyle = new MyLocationStyle();
                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
                aMap.setMyLocationStyle(myLocationStyle);

                mLocationOption.setOnceLocation(false);
                mlocationClient.setLocationOption(mLocationOption);
                mlocationClient.startLocation();
                break;
            default:
                break;
        }

    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        aMapWrapper.onResume();
        if(aMap != null) {
            aMap.setMyLocationEnabled(true);
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        aMapWrapper.onPause();
        deactivate();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        aMapWrapper.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        aMapWrapper.onDestroy();
        if(null != mlocationClient){
            mlocationClient.onDestroy();
        }
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                mLocationErrText.setVisibility(View.GONE);
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                txtViewLat.setText("Latitude: " + String.valueOf(amapLocation.getLatitude()));
                txtViewLon.setText("Longitude: " + String.valueOf(amapLocation.getLongitude()));
                Log.e("mapcore","onLocationChanged " + amapLocation);
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
                mLocationErrText.setVisibility(View.VISIBLE);
                mLocationErrText.setText(errText);
            }
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }



}