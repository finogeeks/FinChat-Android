package com.finogeeks.finochatapp.modules.login


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.finogeeks.auth.AuthCallback
import com.finogeeks.auth.AuthService
import com.finogeeks.auth.model.TokenResponse
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.sdk.FinoCallBack
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.modules.server.view.ServerSelectingActivity
import com.finogeeks.finochatapp.utils.isShouldHideInput
import com.finogeeks.utility.utils.hideSoftInput
import com.finogeeks.utility.views.LoadingDialog
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit


class LoginActivity : BaseActivity() {

    private var mAuthService: AuthService? = null
    private val mAuthCallback = object : AuthCallback {
        override fun onSuccess(p0: TokenResponse?) {
            tokenLogin(p0?.accessToken ?: return)
        }

        override fun onError(error: String?, recoverable: Boolean) {
            toast("onError: $error")
        }

        override fun onLoading(loadingMessage: String?) {
        }
    }

    private val eyesClose by lazy { ContextCompat.getDrawable(this, R.drawable.sdk_login_ic_eyeoff) }
    private val eyesOpen by lazy { ContextCompat.getDrawable(this, R.drawable.sdk_login_ic_eye) }

    private val loadingDialog: LoadingDialog by lazy(LazyThreadSafetyMode.NONE) { LoadingDialog(this, "登录中") }

    private val preferences by lazy { getSharedPreferences("pref_persist", Context.MODE_PRIVATE) }

    @SuppressLint("CheckResult")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        forget_password.setOnClickListener { PhoneVerifyActivity.start(this, PhoneVerifyActivity.MODE_RESET_PASSWORD, REQUEST_CODE) }
        phone_register.setOnClickListener { PhoneVerifyActivity.start(this, PhoneVerifyActivity.MODE_REGISTER, REQUEST_CODE) }
        iv_setting.setOnClickListener { startActivity(Intent(this, ServerSelectingActivity::class.java)) }

        mAuthService = AuthService.Builder
                .builder(this, mAuthCallback, R.raw.auth_config)
                .isMultiProcess(false)
                .build()

        RxView.clicks(button_login)
                .compose(bindToLifecycle())
                .throttleFirst(3, TimeUnit.SECONDS)
                .subscribe {
                    val username = login_username.text.toString()
                    val password = login_password.text.toString()
                    doLogin(username, password)
                }

        RxView.clicks(csrc_login)
                .compose(bindToLifecycle())
                .throttleFirst(3, TimeUnit.SECONDS)
                .subscribe { mAuthService?.beginAuth(REQUEST_CSRC_LOGIN) }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                button_login.isEnabled = login_username.text!!.isNotEmpty() && login_password.text!!.isNotEmpty()
            }
        }

        login_username.setText(preferences.getString("username", ""))
        login_username.setSelection(login_username.text!!.length)
        login_username.addTextChangedListener(watcher)
        login_password.addTextChangedListener(watcher)

        // 密码可见
        password_visible.setOnClickListener {
            login_password.apply {
                when (inputType) {
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> {
                        inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                        transformationMethod = PasswordTransformationMethod.getInstance()
                        setSelection(text!!.length)
                        password_visible.setImageDrawable(eyesClose)
                    }

                    else -> {
                        inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        transformationMethod = HideReturnsTransformationMethod.getInstance()
                        setSelection(text!!.length)
                        password_visible.setImageDrawable(eyesOpen)
                    }
                }
            }
        }
    }

    private fun doLogin(username: String, password: String) {
        // 下面为账号密码登录
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this@LoginActivity, "用户名或密码为空", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show()
        preferences.edit().putString("username", username).apply()

        FinoChatClient.getInstance().accountManager().login(username, password, object : FinoCallBack<Map<String, Any>?> {
            override fun onSuccess(result: Map<String, Any>?) {
                runOnUiThread { loadingDialog.dismiss() }
            }

            override fun onProgress(progress: Int, status: String) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    startActivity(Intent(this@LoginActivity, SplashActivity::class.java))
                    finish()
                }
            }

            override fun onError(code: Int, message: String) {
                Log.d("LoginActivity", message)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    loadingDialog.dismiss()
                }
            }
        })

//        FinoChatClient.getInstance().accountManager().loginWithToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ6TEZYQko4T1pXWWRlQmRWanVuckR3SmNDTTNpUU5wTyIsInRva2VuVHlwZSI6ImFjY2VzcyIsImFwcFR5cGUiOiJTVEFGRiIsImV4cCI6MTU1NDEwMDA1OCwiaWF0IjoxNTU0MDk5NDU4fQ.uMjLkuVeTIdVEwKlMPE6J7y_XhJb3ORFOrQTFRCtT3w"
//                , object : FinoCallBack<Map<String, Any>?> {
//            override fun onSuccess(result: Map<String, Any>?) {
//            }
//
//            override fun onProgress(progress: Int, status: String) {
//                startActivity(Intent(this@LoginActivity, SplashActivity::class.java))
//                finish()
//            }
//
//            override fun onError(code: Int, message: String) {
//                Log.d("LoginActivity", message)
//                Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
//            }
//        })
    }

    override fun onResume() {
        super.onResume()
        val apiUrl = FinoChatClient.getInstance().options.apiURL
        val isCloud = (apiUrl == "https://api.finogeeks.com") || (apiUrl == "https://api.finogeeks.club")
                || apiUrl == "https://fin.fdep.cn" || apiUrl == "https://chat.finogeeks.com"
        forget_password.visibility = if (isCloud) View.VISIBLE else View.GONE
        phone_register.visibility = if (isCloud) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthService?.destroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // 获得当前得到焦点的View，一般情况下就是EditText（特殊情况就是轨迹求或者实体案件会移动焦点）
            if (currentFocus.isShouldHideInput(ev)) {
                this.hideSoftInput()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE -> {
                    if (data != null) {
                        val username = data.getStringExtra(PhoneVerifyActivity.EXTRA_USERNAME)
                        val password = data.getStringExtra(PhoneVerifyActivity.EXTRA_PASSWORD)
                        if (username.isNullOrBlank() || password.isNullOrBlank()) {
                            toast("用户名或密码为空")
                        } else {
                            doLogin(username, password)
                        }
                    } else {
                        Log.e("LoginActivity", "REQUEST_CODE: data is null.")
                    }
                }

                REQUEST_CSRC_LOGIN -> {
                    mAuthService?.getResult(data)
                }

                REQUEST_CSRC_BIND -> {
                    if (data != null) {
                        val username = data.getStringExtra(BindActivity.USERNAME)
                        val password = data.getStringExtra(BindActivity.PASSWORD)
                        if (username.isNullOrBlank() || password.isNullOrBlank()) {
                            toast("用户名或密码为空")
                        } else {
                            doLogin(username, password)
                        }
                    } else {
                        Log.e("LoginActivity", "REQUEST_CSRC_BIND: data is null.")
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (resultCode == REQUEST_CSRC_LOGIN) {
                toast("授权登录操作取消")
            }
        }
    }

    private fun tokenLogin(token: String) {
        FinoChatClient.getInstance().accountManager().loginWithToken(token, object : FinoCallBack<Map<String, Any>?> {
            override fun onSuccess(result: Map<String, Any>?) {
                runOnUiThread { loadingDialog.dismiss() }
            }

            override fun onProgress(progress: Int, status: String) {
                runOnUiThread {
                    loadingDialog.dismiss()
                    startActivity(Intent(this@LoginActivity, SplashActivity::class.java))
                    finish()
                }
            }

            override fun onError(code: Int, message: String) {
                Log.d("LoginActivity", message)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    BindActivity.startForResult(this@LoginActivity, token, REQUEST_CSRC_BIND)
                    loadingDialog.dismiss()
                }
            }
        })
    }

    override fun slideBackDisable() = true

    companion object {
        private const val REQUEST_CODE = 0x100
        private const val REQUEST_CSRC_LOGIN = 0x256
        private const val REQUEST_CSRC_BIND = 0x257
    }
}
