package com.finogeeks.finochatapp.modules.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.finogeeks.finochat.sdk.FinoChatClient;
import com.finogeeks.finochatapp.R;
import com.finogeeks.finochatapp.modules.home.HomeActivity;

public class SplashActivity extends Activity {

    private static SplashActivity instance;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;

        if (!FinoChatClient.getInstance().accountManager().isLogin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        } else if (FinoChatClient.getInstance().isSessionInitSuccess()) {
            navigate2Home();
            return;
        }

        new Handler().postDelayed(SplashActivity::navigate2Home, 2000);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(null);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public static void close() {
        if (instance != null) {
            instance.finish();
        }
    }

    public static void gotoLogin() {
        if (instance != null) {
            instance.startActivity(new Intent(instance, LoginActivity.class));
            instance.finish();
        }
    }

    public static void navigate2Home() {
        if (instance != null) {
            instance.startActivity(new Intent(instance, HomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            instance.finish();
        }
    }
}