package com.finogeeks.finochatapp.modules.server.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.webkit.URLUtil
import com.finogeeks.finochat.components.text.isUrl
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.rest.RetrofitUtil
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochat.sdk.IChatUIManager
import com.finogeeks.finochat.sdk.IChatUIManager.SCAN_RESULT
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochat.repository.finoOptions
import com.finogeeks.finochatapp.R
import com.google.gson.JsonParser
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import kotlinx.android.synthetic.main.activity_server_setting.*
import okhttp3.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.matrix.androidsdk.util.SSLUtils
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

class ServerSettingActivity : BaseActivity() {

    private val mSharedPrefs by lazy { getSharedPreferences(APP_LOCAL_URLS_LIST_SHARED_PREFS, Context.MODE_PRIVATE) }
    private val mServerUrl by lazy { getSharedPreferences(APP_SERVER_URL_SHARED_PREFS, Context.MODE_PRIVATE) }

    private val prevName by lazy { intent.getStringExtra(PREV_SERVER_NAME) }
    private val prevUrl by lazy { intent.getStringExtra(PREV_SERVER_URL) }

    // OkHttp Call, cancel when onDestroy().
    private var mCall: Call? = null

    private val mCallback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            runOnUiThread {
                if (!call.isCanceled()) {
                    longToast("URL地址有误或网络异常")
                    Log.e(LOG_TAG, "Check server URL error: ", e)
                }
            }
        }

        override fun onResponse(call: Call, response: Response) {
            var availableServer = false
            try {
                val element = JsonParser().parse(response.body?.string() ?: "")
                availableServer = element.asJsonObject.has("flows")
            } catch (ignored: Exception) {
            }

            val availableCode = response.code == 200

            runOnUiThread {
                if (availableCode) {
                    if (availableServer) {
                        val serverName = etServerName.text.toString()
                        val serverAddress = etServerUrl.text.toString()

//                        FinoChatClient.getInstance().options.apiURL = serverAddress
//                        RetrofitUtil.reset()
                        val keySet = mSharedPrefs.all.keys

                        when {
                            prevName != null -> {
                                if (serverName in keySet && prevName != serverName) {
                                    toast("名称已存在，请修改")
                                } else {
                                    val serverMap = mSharedPrefs.all
                                    for (i in serverMap) {
                                        if (prevName != i.key && serverAddress == (i.value as String)) {
                                            toast("本地已存在相同地址")
                                            return@runOnUiThread
                                        }
                                    }

                                    replaceServerConfig(prevName, serverName, serverAddress)
                                }
                            }

                            serverName in keySet -> {
                                alert("本地已存在相同名称配置，直接覆盖?") {
                                    negativeButton("取消") { it.dismiss() }
                                    positiveButton("确认") {
                                        it.dismiss()
                                        saveServerConfig(serverName, serverAddress)
                                    }
                                }.show()
                            }

                            else -> {
                                saveServerConfig(serverName, serverAddress)
                            }
                        }
                    } else {
                        longToast("URL地址有误")
                    }
                } else {
                    longToast("URL地址有误或网络异常")
                    Log.e(LOG_TAG, response.toString())
                }
            }
        }
    }

    private fun saveServerConfig(serverName: String, serverAddress: String) {
        urlToUse(serverAddress)

        if (mSharedPrefs.edit().putString(serverName, serverAddress).commit()) {
            val intent = Intent().putExtra("ServerAddress", serverAddress)
            setResult(Activity.RESULT_OK, intent)
            toast("已保存服务器地址")
            finish()
        }
    }

    private fun replaceServerConfig(preName: String, newName: String, newAddress: String) {
        if (!TextUtils.equals(preName, newName)) {
            mSharedPrefs.edit().remove(preName).apply()
        }
        saveServerConfig(newName, newAddress)
    }

    private fun urlToUse(serverAddress: String) {
        if (prevName != null) return
        mServerUrl.edit().putString("apiUrl", serverAddress).commit()
        FinoChatClient.getInstance().options.apiURL = serverAddress
        RetrofitUtil.reset()
    }

    private val mTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            isButtonEnable()
        }
    }

    private val mClient by lazy {
        val builder = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)

        val sslCertName = finoOptions.settings.sslCertName
        if (!TextUtils.isEmpty(sslCertName)) {
            builder.hostnameVerifier(HostnameVerifier { _, _ -> return@HostnameVerifier true })
            SSLUtils.setSSL(application.applicationContext, builder, sslCertName)
        }
        builder.build()
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_setting)
        val title = if (prevName == null) "新增服务器" else "编辑服务器"
        initToolBar(toolbar, title)

        RxView.clicks(confirm)
                .bindToLifecycle(this)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe({
                    when {
                        etServerUrl.text!!.isUrl() -> verifyServer()
                        etServerUrl.text!!.isEmpty() -> finish()
                        else -> toast("URL不合法")
                    }
                }) {
                    Log.e(LOG_TAG, "Click confirm : " + it.localizedMessage)
                }

        etServerName.addTextChangedListener(mTextWatcher)
        etServerUrl.addTextChangedListener(mTextWatcher)

        etServerName.setText(prevName ?: return)
        etServerName.setSelection(prevName.length)

        etServerUrl.setText(prevUrl ?: return)
        etServerUrl.setClearIconVisible(false)
    }

    private fun isButtonEnable() {
        confirm.isEnabled = !(etServerName.text!!.isBlank() || etServerUrl.text!!.isBlank())
    }

    private fun verifyServer() {
        // Cancel last one, start new one.
        mCall?.cancel()

        var rawUrl = etServerUrl.text.toString()

        // Trim last "/".
        if (rawUrl.endsWith("/")) {
            rawUrl = rawUrl.substringBeforeLast("/")
        }

        // Local verify.
        if (prevName == null) {
            val serverMap = mSharedPrefs.all
            for (i in serverMap.values) {
                if (rawUrl == (i as String)) {
                    toast("本地已存在相同地址")
                    return
                }
            }
        }

        if (!URLUtil.isValidUrl(rawUrl) || !Patterns.WEB_URL.matcher(rawUrl).matches()) {
            toast("URL不合法")
            return
        }

        val url = "$rawUrl/_matrix/client/r0/login"
        try {
            val request = Request.Builder().url(url).build()
            mCall = mClient.newCall(request)
            mCall?.enqueue(mCallback)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("URL不合法")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.fc_menu_server_setting_scan, menu)
        val menuItem = menu.findItem(R.id.itemScan)
        menuItem?.actionView?.let { v ->
            RxView.clicks(v)
                    .bindToLifecycle(this)
                    .throttleFirst(1, TimeUnit.SECONDS)
                    .subscribe({ onOptionsItemSelected(menuItem) },
                            { Log.e(LOG_TAG, it.localizedMessage) })
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.itemScan) {
            FinoChatClient.getInstance()
                    .chatUIManager()
                    .scanQrCode(this, false)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IChatUIManager.REQUEST_CODE_SCAN_QR_CODE) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                val result = data.getStringExtra(SCAN_RESULT)
                if (result != null && result.isUrl()) {
                    etServerUrl.apply {
                        setText(result)
                        setSelection(text!!.length)
                    }
                } else {
                    toast("地址不合法")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCall?.cancel()
    }

    companion object {
        private const val LOG_TAG = "ServerSettingActivity"
        const val APP_SERVER_URL_SHARED_PREFS = "APP_SERVER_URL_SHARED_PREFS"
        const val APP_LOCAL_URLS_LIST_SHARED_PREFS = "APP_LOCAL_URLS_LIST_SHARED_PREFS"

        const val PREV_SERVER_NAME = "PREV_SERVER_NAME"
        const val PREV_SERVER_URL = "PREV_SERVER_URL"

    }
}