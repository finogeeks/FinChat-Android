package com.finogeeks.finochatapp.modules.server.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.finogeeks.finochat.rest.RetrofitUtil
import com.finogeeks.finochat.sdk.FinoChatClient
import com.finogeeks.finochatapp.BuildConfig
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.modules.server.adapter.holder.LocalHolder
import com.finogeeks.finochatapp.modules.server.adapter.holder.ServerHolder
import com.finogeeks.finochatapp.modules.server.model.ServerConfig
import com.finogeeks.finochatapp.modules.server.view.ServerSettingActivity.Companion.APP_LOCAL_URLS_LIST_SHARED_PREFS
import com.finogeeks.finochatapp.modules.server.view.ServerSettingActivity.Companion.APP_SERVER_URL_SHARED_PREFS
import org.jetbrains.anko.toast

class ServerAdapter(private val activity: Activity) : androidx.recyclerview.widget.RecyclerView.Adapter<ServerHolder>() {

    private val mInflater = LayoutInflater.from(activity)

    private val mServerUrl by lazy { activity.getSharedPreferences(APP_SERVER_URL_SHARED_PREFS, Context.MODE_PRIVATE) }

    private val mColorSpan = ForegroundColorSpan(Color.BLACK)

    private var mDefServerUrl: String = mServerUrl.getString("apiUrl", BuildConfig.API)!!

    /**
     * 结果列表
     */
    private val mList = ArrayList<ServerConfig>()

    fun combineData(server: ArrayList<ServerConfig>?, local: ArrayList<ServerConfig>?) {
        mList.clear()
        mDefServerUrl = mServerUrl.getString("apiUrl", BuildConfig.API)!!
        server?.run { mList.addAll(this) }
        local?.run { mList.addAll(this) }
        notifyDataSetChanged()
    }

    /**
     * 服务器返回数据后插入数据
     */
    fun insertDate(data: ArrayList<ServerConfig>) {
        if (data.isEmpty()) return
        mList.addAll(0, data)
        notifyDataSetChanged()
    }

    /**
     * 修改选择项目
     */
    fun confirmConfig(v: View, toIndex: Int) {
        val newUrl = mList[toIndex].url

        mDefServerUrl = newUrl

        notifyDataSetChanged()

        FinoChatClient.getInstance().options.apiURL = newUrl
        RetrofitUtil.reset()

        // Confirm server address.
        mServerUrl.edit().putString("apiUrl", newUrl).apply()

        v.postDelayed({
            activity.toast("地址设置成功")
            activity.finish()
        }, 500)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerHolder {
        val view = mInflater.inflate(R.layout.item_server, parent, false)
        return if (viewType == ServerConfig.VIEW_TYPE_SERVER) {
            ServerHolder(view, mColorSpan)
        } else {
            LocalHolder(view, activity, mColorSpan)
        }
    }

    /**
     * 移除本地结果
     */
    fun removeLocal(index: Int) {
        val removing = mList[index]

        // 若删除额选择正在使用，需让用户选择其他服务器
        if (removing.url == mDefServerUrl) {
            activity.toast("删除前请选择其他服务器")
            return
        }

        mList.removeAt(index)
        activity.toast("已删除配置：${removing.name}")
        notifyItemRemoved(index)

        // Remove url from SP.
        activity.getSharedPreferences(APP_LOCAL_URLS_LIST_SHARED_PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(removing.name)
                .apply()
    }

    override fun getItemCount() = mList.size

    override fun onBindViewHolder(holder: ServerHolder, position: Int) {
        holder.onBind(mList[position], position, itemCount, this, mDefServerUrl)
    }

    override fun getItemViewType(position: Int) = mList[position].viewType
}
