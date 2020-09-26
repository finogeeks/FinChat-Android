package com.finogeeks.finochatapp.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.finogeeks.finochat.sdk.FinoChatClient;
import com.finogeeks.finochat.utils.Log;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private static final String TAG = "WXEntryActivity";

    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = WXAPIFactory.createWXAPI(this, FinoChatClient.getInstance().getOptions().getShareParams().wechatAppId, false);

        try {
            api.handleIntent(getIntent(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {
    }

    @Override
    public void onResp(BaseResp resp) {
        Log.d("WXEntryActivity", "onResp errCode : " + resp.errCode + " & errStr : " + resp.errStr);
        FinoChatClient.getInstance().getThirdPartySdkManager().onWXResp(resp);
        finish();
    }
}