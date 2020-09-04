package com.finogeeks.finochatapp.modules.server.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.finogeeks.finochat.modules.base.BaseActivity
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.modules.server.adapter.ServerAdapter
import com.finogeeks.finochatapp.modules.server.model.LocalConfig
import com.finogeeks.finochatapp.modules.server.model.ServerConfig
import com.finogeeks.finochatapp.modules.server.model.ServerData
import com.finogeeks.finochatapp.modules.server.view.ServerSettingActivity.Companion.APP_LOCAL_URLS_LIST_SHARED_PREFS
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_server_selecting.*
import okhttp3.*
import org.matrix.androidsdk.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

class ServerSelectingActivity : BaseActivity() {

    // OkHttp Call, cancel when onDestroy().
    private var mCall: Call? = null

    // Address list from server.
    private var mServerData: ServerData? = null

    private val mCustomData = ArrayList<ServerConfig>()

    private val mServerListPrefs by lazy { getSharedPreferences(APP_LOCAL_URLS_LIST_SHARED_PREFS, Context.MODE_PRIVATE) }

    private lateinit var mAdapter: ServerAdapter

    private val mClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    private val spListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        val value = sp.getString(key, "")
        if (!value.isNullOrBlank()) {
            mCustomData.clear()
            mServerListPrefs.all.map { LocalConfig(it.key, it.value.toString()) }.toCollection(mCustomData)
            mAdapter.combineData(mServerData?.data, mCustomData)
        }
    }

    private val mCallback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(LOG_TAG, "mCallback", e)
        }

        override fun onResponse(call: Call, response: Response) {
            when (response.code) {
                200 -> {
                    val string = response.body?.string()
                    if (string.isNullOrBlank()) return

                    try {
                        mServerData = Gson().fromJson(string, ServerData::class.java)

                        val result = mServerData?.data
                        if (!result.isNullOrEmpty()) {
                            runOnUiThread { mAdapter.insertDate(result) }
                        }
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "mCallback_onResponse_Exception: --> $string <--", e)
                    }
                }

                else -> Log.e(LOG_TAG, response.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_selecting)
        initToolBar(toolbar)

        init()
        initLocal()
        initRemote()
    }

    private fun init() {
        mServerListPrefs.registerOnSharedPreferenceChangeListener(spListener)

        mAdapter = ServerAdapter(this)
        serverList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ServerSelectingActivity)
        serverList.adapter = mAdapter
    }

    /**
     * Load from local
     */
    private fun initLocal() {
        mServerListPrefs.all
                .map { LocalConfig(it.key, it.value as String) }
                .toCollection(mCustomData)
        mAdapter.insertDate(mCustomData)
    }

    /**
     * Load from server.
     */
    private fun initRemote() {
        // Cancel last one, start new one.
        mCall?.cancel()

        val url = "https://api.finogeeks.club/api/v1/platform/server"
        val request = Request.Builder().url(url).build()
        mCall = mClient.newCall(request)
        mCall?.enqueue(mCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCall?.cancel()
        mServerListPrefs.unregisterOnSharedPreferenceChangeListener(spListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.server_adding_toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_toolbar_menu_item_create_chat -> {
                Intent(this, ServerSettingActivity::class.java)
                        .run { startActivityForResult(this, REQ_SERVER_ADDING_ACTIVITY) }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val LOG_TAG = "ServerSelectingActivity"
        const val REQ_SERVER_ADDING_ACTIVITY = 0x100
    }
}