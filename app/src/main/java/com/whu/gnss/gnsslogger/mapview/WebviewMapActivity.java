package com.whu.gnss.gnsslogger.mapview;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapWrapper;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

public class WebviewMapActivity extends Activity implements View.OnClickListener {
    MyWebView webView = null;

    private Button initButton;
    private Button addMapButton;

    private MAWebViewWrapper webViewWrapper;
    private AMapWrapper aMapWrapper;

    private LinearLayout mainLayout ;

    private LatLng latlng = new LatLng(39.761, 116.434);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.debugMode(true);

        mainLayout = new LinearLayout(this);

        webView = new MyWebView(this);
        setContentView(mainLayout);

        LinearLayout linearLayout = new LinearLayout(this);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        addMapButton = new Button(this);
        addMapButton.setText("添加WebView");

        initButton = new Button(this);
        initButton.setText("创建地图");

        linearLayout.addView(addMapButton);
        linearLayout.addView(initButton);

        addContentView(linearLayout,layoutParams);

        webViewWrapper = new MAWebViewWrapper(webView);
        aMapWrapper = new AMapWrapper(this, webViewWrapper);


        // 如果需要长按手势，需要将手势传递给SDK，不需要长按手势不用处理
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return aMapWrapper.onTouchEvent(motionEvent);
            }
        });

        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 初始化地图文件，在线拉取和预加载JS


                if (isAdded && !isCreated) {
                    final long time = System.currentTimeMillis();
                    aMapWrapper.onCreate();
                    aMapWrapper.getMapAsyn(new AMap.OnMapReadyListener() {
                        @Override
                        public void onMapReady(AMap map) {
                            android.util.Log.e("mapcore", "console onMapLoaded cost " + (System.currentTimeMillis() - time));
                            Marker marker = map.addMarker(new MarkerOptions().position(latlng).title("test"));
                            marker.showInfoWindow();
                        }
                    });
//					isCreated = true;
                }

            }
        });

        addMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isAdded) {
                    mainLayout.addView(webView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
                    isAdded = true;
                }
            }
        });
    }

    private boolean isCreated = false;
    private boolean isAdded = false;


    @Override
    public void onClick(View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aMapWrapper.onDestroy();
    }
}
