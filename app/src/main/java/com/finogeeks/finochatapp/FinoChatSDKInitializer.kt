package com.finogeeks.finochatapp

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.finogeeks.finochat.repository.login.LoginStorage
import com.finogeeks.finochat.sdk.*
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochatapp.modules.AuthActivity
import com.finogeeks.finochatapp.modules.login.SplashActivity
import com.finogeeks.finochatapp.modules.server.view.ServerSettingActivity.Companion.APP_SERVER_URL_SHARED_PREFS
import com.finogeeks.utility.utils.isTrue
import org.jetbrains.anko.toast

/**
 * 用于初始化FinoChat SDK的类
 */
class FinoChatSDKInitializer(private val application: Application) {

    @SuppressLint("CheckResult")
    fun init() {
        val option = FinoChatOption().apply {
            appKey = BuildConfig.KEY
            appType = BuildConfig.AppType

            val preferences = application.getSharedPreferences(APP_SERVER_URL_SHARED_PREFS, Context.MODE_PRIVATE)
            var apiUrl = preferences.getString("apiUrl", "")

            when {
                apiUrl?.isNotEmpty().isTrue -> apiURL = apiUrl
                else -> {
                    //如果没有apiurl 从现在登录用户信息里取
                    val configs = LoginStorage(application).credentialsList
                    if (configs?.size ?: 0 > 0) {
                        apiUrl = configs[0].homeserverUri.toString()
                    }
                    //如果还没有 使用默认地址
                    if (apiUrl.isNullOrEmpty()) {
                        apiUrl = BuildConfig.API
                    }
                    apiURL = apiUrl
                    preferences.edit().putString("apiUrl", apiUrl).apply()
                }
            }

            appId = "3"
            apiPrefix = "/api/v1"
            appletApiURL = "https://finchat-mop-private.finogeeks.club"
            appletApiPrefix = "/api/v1/mop/"
            appletAppKey = "22LyZEib0gLTQdU3MUauAQpfYyACROIyPtZXijuos2QA"
            appletAppSecret = "154a4a31a57e75cb"
            notification.notificationIcon = R.drawable.ico_finchat_logo
            isAppDebug = BuildConfig.DEBUG
            logLevel = android.util.Log.VERBOSE
            sdkVersion = BuildConfig.VERSION_NAME

            shareParams = FinoChatOption.ShareParams().apply {
                wechatAppId = BuildConfig.wechatAppId
                qqAppId = BuildConfig.qqAppId
                weiBoAppKey = BuildConfig.weiBoAppKey
                miniProgramId = "gh_8a6a81029506"
                appletAvatar = R.drawable.cs_ic_applets_preview
            }

//            pushConfig.enableMiPush("2882303761518316527", "5331831620527")
//            pushConfig.enableVivoPush()
//            pushConfig.enableHuaweiPush()
//            pushConfig.enableOppoPush("awd", "afdfa")
        }

        FinoChatClient.getInstance().initFinoChatSession(application, option, object : FinoCallBack<Void> {
            override fun onSuccess(result: Void?) {
                Log.i(LOG_TAG, "initFinoChatSession success")
            }

            override fun onProgress(progress: Int, status: String?) {}

            override fun onError(code: Int, message: String?) {
                Log.e(LOG_TAG, "initFinoChatSession onError code : $code, message : $message")
                if (code == FinoError.NO_HISTORY_TOKEN_FOUND) {
                    // 未登录
                    SplashActivity.gotoLogin()
                } else {
                    Handler(Looper.getMainLooper())
                            .post { application.toast("code:$code,message:$message") }
                }
            }
        })


        val menuItems = listOf(
                IPluginManager.MenuItem(IPluginManager.MenuItem.FILE_SPACE_DEFAULT_ID, application.getString(R.string.files_space), R.drawable.sdk_me_ic_myspace))

        FinoChatClient.getInstance()
                .pluginManager()
                .registerMineMenu(menuItems)
                { _, menuItem, map ->
                    if (menuItem.id == 102) {
                        (map["fragment"] as? androidx.fragment.app.Fragment)?.apply {
                            val intent = Intent(context, AuthActivity::class.java)
                                    .putExtra("TYPE", "LOGIN")
                            startActivityForResult(intent, 0x256)
                        }
                    }
                }
    }

    companion object {
        private const val LOG_TAG = "FinoChatSDKInitializer"
    }
}