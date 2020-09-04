package com.finogeeks.finochatapp.modules.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochat.sdk.SimpleCallBack
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochatapp.R
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_phone_verify.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class PhoneVerifyActivity : BaseActivity() {

    private var mDisposable: Disposable? = null

    /**
     * Default is MODE_REGISTER;
     *
     * MODE_REGISTER, MODE_RESET_PASSWORD
     */
    private val mMode by lazy { intent.getIntExtra(EXTRA_MODE, MODE_REGISTER) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_verify)

        onDestroyDisposer += RxView.clicks(nextBtn)
                .throttleFirst(3, TimeUnit.SECONDS)
                .subscribe { onNextStep() }

        onDestroyDisposer += RxView.clicks(request_captcha)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe {
                    when (mMode) {
                        MODE_REGISTER -> verifyAndSend()
                        MODE_RESET_PASSWORD -> sendCaptcha()
                    }
                }

        // TextWatcher.
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val isPhoneNumberFit = (phone.text!!.length == 11)
                request_captcha.isEnabled = isPhoneNumberFit && mDisposable?.isDisposed ?: true
                nextBtn.isEnabled = isPhoneNumberFit && (code.text!!.length == 6)
            }
        }

        phone.addTextChangedListener(textWatcher)
        code.addTextChangedListener(textWatcher)
    }

    /**
     * Do not remove the param.
     */
    fun setCancel(v: View) {
        finish()
    }

    private fun verifyAndSend() {
        val phoneNumber = phone.text!!.trim().toString()

        when {
            phoneNumber.isEmpty() -> toast("请输入手机号")
            phoneNumber.length != 11 || !phoneNumber.startsWith("1") -> toast("请输入合法手机号")
            else -> FinoChatClient.getInstance().accountManager()
                    .finIsPhoneAvailable(this, phoneNumber, object : SimpleCallBack<Int>() {
                        override fun onSuccess(result: Int?) {
                            when (result) {
                                200 -> sendCaptcha()
                                409 -> toast("该手机号已注册")
                                else -> longToast("服务器内部异常: $result")
                            }
                        }

                        override fun onError(code: Int, error: String?) {
                            Log.e(TAG, "requestCode", error ?: "Error but no message.")
                            longToast("$error")
                        }
                    })
        }
    }

    private fun sendCaptcha() {
        requestCodeEnable(false)

        FinoChatClient.getInstance()
                .accountManager()
                .getFinRegisterSms(this, phone.text.toString(), object : SimpleCallBack<Void>() {
                    override fun onSuccess(result: Void?) {
                        super.onSuccess(result)
                        code.requestFocus()
                        toast("验证码已发送，请查收")
                    }

                    override fun onError(code: Int, error: String) {
                        super.onError(code, error)
                        Log.e(TAG, error)
                        requestCodeEnable(true)
                        toast(error)
                    }
                })

        mDisposable = Flowable.interval(0, 1, TimeUnit.SECONDS)
                .onBackpressureBuffer()
                .take(CAPTCHA_COUNT + 1)
                .map { CAPTCHA_COUNT - it }
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { requestCodeEnable(false) }
                .subscribe({ request_captcha.text = "${it}秒后重新发送" },
                        { Log.e(TAG, "sendCaptcha", it) },
                        { requestCodeEnable(true) })
    }

    /**
     * 发送验证码按钮是否有效
     */
    private fun requestCodeEnable(enable: Boolean) {
        request_captcha.apply {
            if (enable) {
                mDisposable?.dispose()
                isEnabled = true
                text = "发送验证码"
            } else {
                isEnabled = false
            }
        }
    }

    /**
     * To next step if the captcha is verified successful.
     */
    private fun onNextStep() {
        val phone = phone.text.toString()
        val code = code.text.toString()
        val type = if (mMode == MODE_RESET_PASSWORD) "forgetPwd" else "register"

        FinoChatClient.getInstance()
                .accountManager()
                .finVerifyPhoneNumber(this, phone, code, type, object : SimpleCallBack<Void?>() {
                    override fun onSuccess(result: Void?) {
                        when (mMode) {
                            MODE_REGISTER -> {
                                PhoneRegisterActivity.startForResult(this@PhoneVerifyActivity, phone, code, REQ_CODE_REGISTER)
                            }

                            MODE_RESET_PASSWORD -> {
                                ResetPasswordActivity.startForResult(this@PhoneVerifyActivity, phone, code, REQ_CODE_REGISTER)
                            }
                        }
                    }

                    override fun onError(code: Int, error: String?) {
                        toast("$error")
                        Log.e(TAG, "onNextStep", "$code $error")
                    }
                })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // 注册完成，或忘记密码修改成功后，使用相同值登录进LoginActivity.
            if (requestCode == REQ_CODE_REGISTER) {
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "PhoneVerifyActivity"

        private const val CAPTCHA_COUNT = 59L
        private const val REQ_CODE_REGISTER = 0x200

        const val MODE_REGISTER = 1
        const val MODE_RESET_PASSWORD = 2

        private const val EXTRA_MODE = ""
        const val EXTRA_USERNAME = "USERNAME"
        const val EXTRA_PASSWORD = "PASSWORD"

        fun start(activity: Activity, mode: Int = MODE_REGISTER, requestCode: Int) {
            Intent(activity, PhoneVerifyActivity::class.java)
                    .putExtra(EXTRA_MODE, mode)
                    .run { activity.startActivityForResult(this, requestCode) }
        }
    }
}