package hu.zenith.gamebox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends ComponentActivity {
    private static final String HOME = "https://window-wiggle-arcade.lovable.app/";
    private WebView web;
    private LinearLayout error;
    private Insets safe = Insets.NONE;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 28) getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        int background = getColor(R.color.console_background);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(background);
        web = new WebView(this);
        web.setBackgroundColor(background);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        error = new LinearLayout(this);
        error.setOrientation(LinearLayout.VERTICAL);
        error.setGravity(android.view.Gravity.CENTER);
        error.setBackgroundColor(background);
        TextView message = new TextView(this);
        message.setText("A konzol megnyitásához internetkapcsolat szükséges.");
        message.setTextColor(Color.WHITE);
        message.setGravity(android.view.Gravity.CENTER);
        Button retry = new Button(this);
        retry.setText("Újrapróbálás");
        retry.setOnClickListener(v -> { error.setVisibility(View.GONE); web.loadUrl(HOME); });
        error.addView(message);
        error.addView(retry);
        error.setVisibility(View.GONE);
        root.addView(error, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            error.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            applySafeArea();
            return insets;
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { applySafeArea(); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && "window-wiggle-arcade.lovable.app".equals(uri.getHost()) && (uri.getPort() == -1 || uri.getPort() == 443)) return false;
                if (("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && request.isForMainFrame()) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (android.content.ActivityNotFoundException ignored) { }
                }
                return true;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError failure) {
                if (request.isForMainFrame()) error.setVisibility(View.VISIBLE);
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame()) error.setVisibility(View.VISIBLE);
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (web.canGoBack()) web.goBack(); else finish();
            }
        });
        web.loadUrl(HOME);
        ViewCompat.requestApplyInsets(root);
    }
    private void applySafeArea() {
        if (web == null) return;
        // Convert physical inset pixels to CSS pixels; never pad the WebView itself.
        web.evaluateJavascript("(()=>{const d=window.devicePixelRatio||1,s=document.documentElement.style;" +
            "s.setProperty('--native-safe-top','" + safe.top + "'/d+'px');" +
            "s.setProperty('--native-safe-right','" + safe.right + "'/d+'px');" +
            "s.setProperty('--native-safe-bottom','" + safe.bottom + "'/d+'px');" +
            "s.setProperty('--native-safe-left','" + safe.left + "'/d+'px');})()", null);
    }
    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
