package com.finogeeks.finochatapp.modules.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.finogeeks.finochat.components.utils.asyncIO
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.sdk.FinoCallBack
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.rest.bindApi
import com.finogeeks.finochatapp.rest.model.BindParam
import com.finogeeks.utility.views.LoadingDialog
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import kotlinx.android.synthetic.main.activity_bind.*
import org.jetbrains.anko.toast
import org.matrix.androidsdk.rest.model.login.Credentials
import java.util.concurrent.TimeUnit

class BindActivity : BaseActivity() {

    private val loadingDialog: LoadingDialog by lazy(LazyThreadSafetyMode.NONE) { LoadingDialog(this, "登录中") }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bind)
        initToolBar(toolbar)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                button_login.isEnabled = !login_username.text.isNullOrEmpty() && !login_password.text.isNullOrEmpty()
            }
        }

        login_username.addTextChangedListener(watcher)
        login_password.addTextChangedListener(watcher)

        RxView.clicks(button_login)
                .compose(bindToLifecycle())
                .bindToLifecycle(this)
                .throttleFirst(3, TimeUnit.SECONDS)
                .subscribe {
                    val username = login_username.text.toString()
                    val password = login_password.text.toString()
                    doLogin(username, password)
                }
    }

    private fun doLogin(username: String, password: String) {
        loadingDialog.show()
        FinoChatClient.getInstance()
                .accountManager()
                .loginGetCredential(username, password, null, null, object : FinoCallBack<Credentials> {
                    @SuppressLint("CheckResult")
                    override fun onSuccess(result: Credentials?) {
                        if (result != null) {
                            val param = BindParam("bind", intent.getStringExtra(KEY_CLOAK_TOKEN))
                            bindApi.bind(param, result.authorization)
                                    .bindUntilEvent(this@BindActivity, ActivityEvent.DESTROY)
                                    .asyncIO()
                                    .subscribe({
                                        loadingDialog.dismiss()
                                        val intent = Intent()
                                        intent.putExtra(USERNAME, username)
                                        intent.putExtra(PASSWORD, password)
                                        setResult(Activity.RESULT_OK, intent)
                                        finish()
                                    }, {
                                        loadingDialog.dismiss()
                                        Log.e("BindActivity", it, "loginGetCredential - bind")
                                        toast("账号绑定失败")
                                    })
                        } else {
                            loadingDialog.dismiss()
                            Log.e("BindActivity", "loginGetCredential, credential from server is null.")
                            toast("账号登录获取数据错误")
                        }
                    }

                    override fun onProgress(status: Int, error: String?) {
                    }

                    override fun onError(code: Int, error: String?) {
                        loadingDialog.dismiss()
                        Log.e("BindActivity", "loginGetCredential, error: $error")
                        toast("账号登录失败")
                    }
                })
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        super.onDestroy()
    }

    companion object {
        const val KEY_CLOAK_TOKEN = "keyCloakToken"
        const val USERNAME = "username"
        const val PASSWORD = "password"

        fun startForResult(activity: Activity, keyCloakToken: String, requestCode: Int) {
            val intent = Intent(activity, BindActivity::class.java)
            intent.putExtra(KEY_CLOAK_TOKEN, keyCloakToken)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
