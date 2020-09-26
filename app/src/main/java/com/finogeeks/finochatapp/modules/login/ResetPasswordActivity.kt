package com.finogeeks.finochatapp.modules.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MenuItem
import android.view.MotionEvent
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochat.sdk.SimpleCallBack
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.utils.isShouldHideInput
import com.finogeeks.utility.utils.hideSoftInput
import kotlinx.android.synthetic.main.activity_forget_password.*
import org.jetbrains.anko.toast

class ResetPasswordActivity : BaseActivity() {

    private val eyesClose by lazy { ContextCompat.getDrawable(this, R.drawable.sdk_login_ic_eyeoff) }
    private val eyesOpen by lazy { ContextCompat.getDrawable(this, R.drawable.sdk_login_ic_eye) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 确认注册按钮
        button_confirm.setOnClickListener {
            confirm()
        }

        password_visible.setOnClickListener {
            password_input.apply {
                if (inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                    transformationMethod = PasswordTransformationMethod.getInstance()
                    setSelection(text.length)
                    password_visible.setImageDrawable(eyesClose)
                } else {
                    inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    transformationMethod = HideReturnsTransformationMethod.getInstance()
                    setSelection(text.length)
                    password_visible.setImageDrawable(eyesOpen)
                }
            }
        }
    }

    /**
     * Click on the confirm button.
     */
    private fun confirm() {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE)
        val verificationCode = intent.getStringExtra(EXTRA_CODE)
        val pw = password_input.text.toString()

        when {
            pw.isEmpty() -> toast("请输入密码")
            pw.length < MIN_PASSWORD_LENGTH -> toast("密码少于${MIN_PASSWORD_LENGTH}位")
            pw.length > MAX_PASSWORD_LENGTH -> toast("密码超过${MAX_PASSWORD_LENGTH}位")
            else -> FinoChatClient.getInstance()
                    .accountManager()
                    .finResetPassword(phoneNumber, verificationCode, pw, object : SimpleCallBack<Void?>() {
                        override fun onSuccess(result: Void?) {
                            toast("密码修改成功，请登录")
                            val i = Intent()
                                    .putExtra(PhoneVerifyActivity.EXTRA_USERNAME, phoneNumber)
                                    .putExtra(PhoneVerifyActivity.EXTRA_PASSWORD, pw)
                            setResult(Activity.RESULT_OK, i)
                            this@ResetPasswordActivity.finish()
                        }

                        override fun onError(code: Int, error: String) {
                            toast(error)
                        }
                    })
        }
    }

    /**
     * Init BackPressed.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 获得当前得到焦点的View，一般情况下就是EditText
        if (ev.action == MotionEvent.ACTION_DOWN && currentFocus.isShouldHideInput(ev)) {
            hideSoftInput()
        }
        return super.dispatchTouchEvent(ev)
    }

    companion object {
        const val EXTRA_PHONE = "PHONE"
        const val EXTRA_CODE = "CODE"

        const val MIN_PASSWORD_LENGTH = 6
        const val MAX_PASSWORD_LENGTH = 16

        fun startForResult(activity: Activity, phone: String, code: String, requestCode: Int) {
            Intent(activity, ResetPasswordActivity::class.java)
                    .putExtra(EXTRA_PHONE, phone)
                    .putExtra(EXTRA_CODE, code)
                    .run { activity.startActivityForResult(this, requestCode) }
        }
    }
}
