package com.whu.gnss.gnsslogger.mapview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.amap.api.maps.IAMapJsCallback;
import com.amap.api.maps.IAMapWebView;

public class MAWebViewWrapper implements IAMapWebView {

    private final WebView webView;
    private WebViewClient mapWebViewClient;

    public MAWebViewWrapper(final WebView webView) {
        this.webView = webView;
        if (this.webView != null) {
            this.webView.setWebViewClient(
                    new WebViewClient() {
                        @TargetApi(Build.VERSION_CODES.N)
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            if (mapWebViewClient != null) {
                                boolean flag = mapWebViewClient.shouldOverrideUrlLoading(view, request);
                                if (flag) {
                                    return true;
                                }

                            }
                            return super.shouldOverrideUrlLoading(view, request);
                        }

                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                            if (mapWebViewClient != null) {
                                WebResourceResponse flag = mapWebViewClient.shouldInterceptRequest(view, request);
                                if (flag != null) {
                                    return flag;
                                }

                            }
                            return super.shouldInterceptRequest(view, request);
                        }

                        @Override
                        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                            if (mapWebViewClient != null) {
                                WebResourceResponse flag = mapWebViewClient.shouldInterceptRequest(view, url);
                                if (flag != null) {
                                    return flag;
                                }

                            }
                            return super.shouldInterceptRequest(view, url);
                        }
                    }
            );
        }
    }

    @Override
    public void evaluateJavascript(String jsCallSig, ValueCallback<String> callback) {
        if (this.webView != null) {
            this.webView.evaluateJavascript(jsCallSig, callback);
        }
    }

    @Override
    public void loadUrl(String toString) {
        if (this.webView != null) {
            this.webView.loadUrl(toString);
        }
    }

    @Override
    public void addAMapJavascriptInterface(IAMapJsCallback object, String javascriptInterfaceName) {
        if (this.webView != null) {
            this.webView.addJavascriptInterface(object, javascriptInterfaceName);
        }
    }

    @Override
    public void setWebViewClient(WebViewClient webViewClient) {
        this.mapWebViewClient = webViewClient;
    }

    @Override
    public int getWidth() {
        if (this.webView != null) {
            return this.webView.getWidth();
        }
        return 0;
    }

    @Override
    public int getHeight() {
        if (this.webView != null) {
            this.webView.getHeight();
        }
        return 0;
    }


    @Override
    public void addView(View v, ViewGroup.LayoutParams params) {
        if (webView != null && v != null) {
            webView.addView(v, params);
        }
    }
}
