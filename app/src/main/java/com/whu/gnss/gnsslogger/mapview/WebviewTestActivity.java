package com.whu.gnss.gnsslogger.mapview;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * AMapV2地图中介绍如何显示一个基本地图
 */
public class WebviewTestActivity extends Activity{
    MyWebView webView = null;
    private LinearLayout mainLayout ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainLayout = new LinearLayout(this);
        webView = new MyWebView(this);
        mainLayout.addView(webView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(mainLayout);
        webView.loadUrl("file:///android_asset/show_chrome_version.html");

    }
}
