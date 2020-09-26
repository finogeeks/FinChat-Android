package com.finogeeks.finochatapp.modules.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochat.modules.common.WebViewActivity
import com.finogeeks.finochat.router.StaticUrls
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochat.sdk.SimpleCallBack
import com.finogeeks.finochat.utils.Log
import com.finogeeks.finochat.repository.finoOptions
import com.finogeeks.finochatapp.R
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.activity_phone_register.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class PhoneRegisterActivity : BaseActivity() {

    private val eyesClose by lazy { ContextCompat.getDrawable(this, R.drawable.sdk_login_ic_eyeoff) }
    private val eyesOpen by lazy { ContextCompat.getDrawable(this, R.drawable.sdk_login_ic_eye) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_register)
        initToolBar(toolbar)

        onDestroyDisposer += RxView.clicks(registerBtn)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe { confirm() }

        onDestroyDisposer += RxView.clicks(tv_user_protocol)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe { WebViewActivity.start(this, URL_USER_PROTOCOL, showOption = false) }

        // 密码可见
        password_visible.setOnClickListener {
            password_input.apply {
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

        username_input.setOnFocusChangeListener { _, hasFocus ->
            val name = username_input.text.toString()
            if (!hasFocus) {
                // setOnFocusChangeListener覆盖了父类的实例，所以通过反射调用父类方法
                try {
                    val cls = username_input.javaClass
                    val method = cls.getDeclaredMethod("setClearIconVisible", Boolean::class.java)
                    method.isAccessible = true
                    method.invoke(username_input, false)
                } catch (ignored: Exception) {
                }

                when {
                    name.isEmpty() -> longToast("请输入用户名")
                    name.length < MIN_USERNAME_LENGTH -> longToast("用户名不能少于${MIN_USERNAME_LENGTH}位")
                    name.length > MAX_USERNAME_LENGTH -> longToast("用户名不能超过${MAX_USERNAME_LENGTH}位") // Should never happened.
                    !name.first().isLowerCase() -> longToast("用户名首位须为小写字母")// 检查用户名第一位是否为小写字母
                    !REG_REGEX.toRegex().matches(name) -> longToast("含有大写字或异常字符，请重新输入")
                }
            }
        }
    }

    /**
     * Click on the confirm button.
     */
    private fun confirm() {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE)
        val code = intent.getStringExtra(EXTRA_CODE)

        val name = username_input.text!!.trim().toString()
        val pw = password_input.text!!.trim().toString()

        when {
            name.isEmpty() -> toast("请输入用户名")
            name.length < MIN_USERNAME_LENGTH -> toast("用户名不能少于${MIN_USERNAME_LENGTH}位")
            name.length > MAX_USERNAME_LENGTH -> toast("用户名不能超过${MAX_USERNAME_LENGTH}位") // Should never happened.
            !name.first().isLowerCase() -> toast("用户名首位须为小写字母")// 检查用户名第一位是否为小写字母
            !REG_REGEX.toRegex().matches(name) -> toast("含有大写字或异常字符，请重新输入")

            pw.isBlank() -> toast("请输入密码")
            pw.length < MIN_PASSWORD_LENGTH -> toast("密码长度需为${MIN_PASSWORD_LENGTH}到${MAX_PASSWORD_LENGTH}位")
            pw.length > MAX_PASSWORD_LENGTH -> toast("密码长度需为${MIN_PASSWORD_LENGTH}到${MAX_PASSWORD_LENGTH}位")

            !check_box.isChecked -> longToast("请阅读并同意《FinChat用户使用协议》")

            else -> {
                FinoChatClient.getInstance().accountManager()
                        .finAccountRegister(this, phoneNumber, name, code, pw, object : SimpleCallBack<Void>() {
                            override fun onSuccess(result: Void?) {
                                toast("注册成功")
                                val i = Intent()
                                        .putExtra(PhoneVerifyActivity.EXTRA_USERNAME, name)
                                        .putExtra(PhoneVerifyActivity.EXTRA_PASSWORD, pw)
                                setResult(Activity.RESULT_OK, i)
                                this@PhoneRegisterActivity.finish()
                            }

                            override fun onError(code: Int, error: String) {
                                toast(error)
                                Log.e(LOG_TAG, "confirm: $error")
                            }
                        })
            }
        }
    }

    companion object {
        private val URL_USER_PROTOCOL
            get() = finoOptions.apiURL + StaticUrls.termsofService

        private const val LOG_TAG = "PhoneRegisterActivity"

        const val EXTRA_PHONE = "PHONE"
        const val EXTRA_CODE = "CODE"

        // 匹配注册用户名的正则表达式
        private const val REG_REGEX = "^[a-z]+[a-z0-9_]{4,21}"

        const val MIN_USERNAME_LENGTH = 5
        const val MAX_USERNAME_LENGTH = 16
        const val MIN_PASSWORD_LENGTH = 6
        const val MAX_PASSWORD_LENGTH = 16

        fun startForResult(activity: Activity, phone: String, code: String, reqCode: Int) {
            Intent(activity, PhoneRegisterActivity::class.java)
                    .putExtra(EXTRA_PHONE, phone)
                    .putExtra(EXTRA_CODE, code)
                    .run { activity.startActivityForResult(this, reqCode) }
        }
    }
}