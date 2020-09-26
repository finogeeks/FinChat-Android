package com.finogeeks.finochatapp

//import com.tencent.matrix.Matrix
//import com.tencent.matrix.iocanary.IOCanaryPlugin
//import com.tencent.matrix.iocanary.config.IOConfig
//import com.tencent.matrix.resource.ResourcePlugin
//import com.tencent.matrix.resource.config.ResourceConfig
//import com.tencent.matrix.resource.config.ResourceConfig.DumpMode
//import com.tencent.matrix.trace.TracePlugin
//import com.tencent.matrix.trace.config.TraceConfig
//import com.tencent.sqlitelint.SQLiteLint
//import com.tencent.sqlitelint.SQLiteLintPlugin
//import com.tencent.sqlitelint.config.SQLiteLintConfig

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import androidx.multidex.MultiDexApplication
import com.finogeeks.utility.utils.isTrue
//import com.github.moduth.blockcanary.BlockCanary
import com.tencent.bugly.crashreport.CrashReport


class FinoChatApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        val processName = processName()
        if (processName?.endsWith(":finAuthWebView").isTrue
                || processName?.contains(":FinApp").isTrue) {
            return
        }

        if (!BuildConfig.DEBUG) {
            CrashReport.initCrashReport(applicationContext, "c13f0687d7", BuildConfig.DEBUG)
            CrashReport.setAppVersion(applicationContext, BuildConfig.VERSION_NAME)
            CrashReport.setAppChannel(this@FinoChatApplication, BuildConfig.FLAVOR)
        } else {
//            tencentMatrix()
//            BlockCanary.install(this, AppBlockCanaryContext()).start()
        }

        FinoChatSDKInitializer(this).init()

//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
//                    .detectAll()
//                    .build())
//
//            StrictMode.setVmPolicy(VmPolicy.Builder()
//                    .detectAll()
//                    .build())
//        }
    }

    private fun processName(): String? {
        return try {
            (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                    ?.runningAppProcesses
                    ?.firstOrNull { it.pid == Process.myPid() }
                    ?.processName
        } catch (ignore: Exception) {
            null
        }
    }

//    private fun initSQLiteLintConfig(): SQLiteLintConfig? {
//        return try {
//            /**
//             * HOOK模式下，SQLiteLint会自己去获取所有已执行的sql语句及其耗时(by hooking sqlite3_profile)
//             * @see 而另一个模式：SQLiteLint.SqlExecutionCallbackMode.CUSTOM_NOTIFY , 则需要调用 {@link SQLiteLint.notifySqlExecution
//             * @see TestSQLiteLintActivity.doTest
//             */
//            SQLiteLintConfig(SQLiteLint.SqlExecutionCallbackMode.HOOK)
//        } catch (t: Throwable) {
//            SQLiteLintConfig(SQLiteLint.SqlExecutionCallbackMode.HOOK)
//        }
//    }

//    private fun tencentMatrix() {
//        val dynamicConfig = DynamicConfigImplDemo()
//        val matrixEnable = dynamicConfig.isMatrixEnable
//        val fpsEnable = dynamicConfig.isFPSEnable
//        val traceEnable = dynamicConfig.isTraceEnable
//
//        sContext = this
//
//        val builder: Matrix.Builder = Matrix.Builder(this)
//        builder.patchListener(TestPluginListener(this))
//
//        val traceConfig = TraceConfig.Builder()
//                .dynamicConfig(dynamicConfig)
//                .enableFPS(fpsEnable)
//                .enableEvilMethodTrace(traceEnable)
//                .enableAnrTrace(traceEnable)
//                .enableStartup(traceEnable)
////                .splashActivities("sample.tencent.matrix.SplashActivity;")
//                .isDebug(true)
//                .isDevEnv(false)
//                .build()
//
//        val tracePlugin = TracePlugin(traceConfig)
//        builder.plugin(tracePlugin)
//
//        if (matrixEnable) { //resource
//            val intent = Intent()
//            val mode = DumpMode.AUTO_DUMP
//            intent.setClassName(this.packageName, "com.tencent.mm.ui.matrix.ManualDumpActivity")
//            val resourceConfig = ResourceConfig.Builder()
//                    .dynamicConfig(dynamicConfig)
//                    .setAutoDumpHprofMode(mode)
//                    .setDetectDebuger(true) //matrix test code
////                    .setNotificationContentIntent(intent)
//                    .build()
//            builder.plugin(ResourcePlugin(resourceConfig))
//            ResourcePlugin.activityLeakFixer(this)
//
//            //io
//            val ioCanaryPlugin = IOCanaryPlugin(IOConfig.Builder()
//                    .dynamicConfig(dynamicConfig)
//                    .build())
//            builder.plugin(ioCanaryPlugin)
//
//            val sqlLiteConfig: SQLiteLintConfig = try {
//                SQLiteLintConfig(SQLiteLint.SqlExecutionCallbackMode.CUSTOM_NOTIFY)
//            } catch (t: Throwable) {
//                SQLiteLintConfig(SQLiteLint.SqlExecutionCallbackMode.CUSTOM_NOTIFY)
//            }
//            builder.plugin(SQLiteLintPlugin(sqlLiteConfig))
//        }
//
//        Matrix.init(builder.build())
//        tracePlugin.start()
//    }
}