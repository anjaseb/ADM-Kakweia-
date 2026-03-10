package ao.kakweia.adm;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String URL = "https://kakweia-comercial.vercel.app/kakweia-admin.html";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private RelativeLayout offlineLayout;
    private ValueCallback<Uri[]> fileUploadCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView       = findViewById(R.id.webView);
        progressBar   = findViewById(R.id.progressBar);
        swipeRefresh  = findViewById(R.id.swipeRefresh);
        offlineLayout = findViewById(R.id.offlineLayout);
        Button btnRetry = findViewById(R.id.btnRetry);

        swipeRefresh.setColorSchemeColors(0xFF0055D4, 0xFFD4A017);
        swipeRefresh.setOnRefreshListener(() -> {
            if (isOnline()) { webView.reload(); }
            else { swipeRefresh.setRefreshing(false); showOffline(); }
        });

        btnRetry.setOnClickListener(v -> {
            if (isOnline()) {
                offlineLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.reload();
            }
        });

        // WebView settings
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) showOffline();
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (!url.contains("kakweia-comercial.vercel.app")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progressBar.setProgress(p);
                if (p == 100) progressBar.setVisibility(View.GONE);
            }
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                fileUploadCallback = cb;
                try { startActivityForResult(params.createIntent(), 100); }
                catch (Exception e) { fileUploadCallback = null; return false; }
                return true;
            }
            @Override
            public boolean onJsConfirm(WebView v, String url, String msg, JsResult r) {
                new AlertDialog.Builder(MainActivity.this)
                    .setMessage(msg)
                    .setPositiveButton("Sim", (d, w) -> r.confirm())
                    .setNegativeButton("Não", (d, w) -> r.cancel()).show();
                return true;
            }
        });

        if (isOnline()) { webView.loadUrl(URL); }
        else { showOffline(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && fileUploadCallback != null) {
            fileUploadCallback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(res, data));
            fileUploadCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack(); return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(this)
                .setMessage("Sair da ADM Kakweia?")
                .setPositiveButton("Sim", (d, w) -> finish())
                .setNegativeButton("Não", null).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void showOffline() {
        webView.setVisibility(View.GONE);
        offlineLayout.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);
    }

    @Override protected void onResume()  { super.onResume();  webView.onResume(); }
    @Override protected void onPause()   { super.onPause();   webView.onPause(); }
    @Override protected void onDestroy() { webView.destroy(); super.onDestroy(); }
}
