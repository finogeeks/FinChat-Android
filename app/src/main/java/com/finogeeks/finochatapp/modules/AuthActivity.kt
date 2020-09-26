package com.finogeeks.finochatapp.modules

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.finogeeks.auth.AuthCallback
import com.finogeeks.auth.AuthService
import com.finogeeks.auth.model.TokenResponse
import com.finogeeks.finochat.components.utils.asyncIO
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.services.ServiceFactory
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochat.repository.currentSession
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.rest.bindApi
import com.finogeeks.finochatapp.rest.model.BindParam
import com.finogeeks.finochatapp.rest.model.BindResult
import com.google.gson.Gson
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import kotlinx.android.synthetic.main.activity_keycloak_auth.*
import org.jetbrains.anko.toast
import retrofit2.HttpException

class AuthActivity : BaseActivity() {
    private var mAuthService: AuthService? = null
    private val mAuthCallback = object : AuthCallback {
        override fun onSuccess(p0: TokenResponse?) {
            if (p0 != null && !p0.accessToken.isNullOrBlank()) {
                val id = currentSession!!.credentials.payload?.get("keycloakId") as String?
                bind(id.isNullOrBlank(), p0.accessToken!!)
            }
        }

        override fun onError(error: String?, recoverable: Boolean) {
            toast("onError: $error")
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        override fun onLoading(loadingMessage: String?) {
        }
    }

    /**
     * Add KeyCloakId to the payload of HomeServer's Credential.
     */
    private fun saveKeyCloakId(id: String?) {
        if (id == null) return
        val homeConfig = currentSession!!.homeServerConfig
        homeConfig.credentials.payload["keycloakId"] = id
        ServiceFactory.getInstance().sessionManager.loginStorage.replaceCredentials(homeConfig)
    }

    private fun removeKeyCloakId() {
        val homeConfig = currentSession!!.homeServerConfig
        homeConfig.credentials.payload.remove("keycloakId")
        ServiceFactory.getInstance().sessionManager.loginStorage.replaceCredentials(homeConfig)
    }

    @SuppressLint("CheckResult")
    private fun bind(bind: Boolean, keyCloakToken: String, cover: Boolean = false) {
        val session = ServiceFactory.getInstance().sessionManager.currentSession
        val jwt = session.credentials.authorization

        if (bind) {
            val coverOrBind = if (cover) "cover" else "bind"
            val param = BindParam(coverOrBind, keyCloakToken)
            bindApi.bind(param, jwt)
        } else {
            bindApi.unbind(jwt)
        }.bindUntilEvent(this, ActivityEvent.DESTROY)
                .asyncIO()
                .subscribe({
                    if (bind) { // Success
                        saveKeyCloakId(it.id)
                        toast("绑定成功")
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else { // Error
                        removeKeyCloakId()
                        toast("解除绑定成功")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }, {
                    if (it is HttpException) {
                        val code = it.code()
                        val intent = Intent().putExtra("ERROR_CODE", code)
                        when (code) {
                            400 -> {
                                val jsonString = it.response()?.errorBody()?.string() ?: ""
                                val result = Gson().fromJson(jsonString, BindResult::class.java)
                                Log.e("AuthActivity", "bindType: $bind", jsonString)
                                toast(result.error.toString())
                                setResult(Activity.RESULT_CANCELED, intent)
                                finish()
                            }

                            409 -> {
                                Log.e("AuthActivity", "bindType: $bind", it.message())
                                intent.putExtra("ERR", it.response()?.errorBody()?.string() ?: "")
                                intent.putExtra("KEY_CLOAK_TOKEN", keyCloakToken)
                                setResult(Activity.RESULT_CANCELED, intent)
                                finish()
                            }

                            404 -> {
                                toast("绑定功能异常：404")
                                Log.e("AuthActivity", "bindType: $bind", it.message())
                                setResult(Activity.RESULT_CANCELED, intent)
                                finish()
                            }

                            else -> {
                                toast("未知异常：$code")
                                Log.e("AuthActivity", "bindType: $bind", it.message())
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        }
                    } else {
                        toast("访问接口异常")
                        Log.e("AuthActivity", "bindType: $bind", it)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keycloak_auth)
        initToolBar(toolbar, null)

        mAuthService = AuthService.Builder
                .builder(this, mAuthCallback, R.raw.auth_config)
                .isMultiProcess(false)
                .build()

        val id = currentSession!!.credentials.payload?.get("keycloakId") as String?
        toAuth.setOnClickListener { mAuthService?.beginAuth(REQUEST_CSRC_LOGIN) }
        toAuth.text = if (id.isNullOrBlank()) "开始绑定操作" else "开始解除绑定操作"
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthService?.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CSRC_LOGIN) {
            when (resultCode) {
                Activity.RESULT_OK -> mAuthService?.getResult(data)
                Activity.RESULT_CANCELED -> {
                    toast("取消授权登录")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CSRC_LOGIN = 0x256
    }
}

