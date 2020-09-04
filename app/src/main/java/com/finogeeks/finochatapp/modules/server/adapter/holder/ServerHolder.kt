package com.finogeeks.finochatapp.modules.server.adapter.holder

import androidx.recyclerview.widget.RecyclerView
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.modules.server.adapter.ServerAdapter
import com.finogeeks.finochatapp.modules.server.model.ServerConfig

open class ServerHolder(itemView: View,
                        private val colorSpan: ForegroundColorSpan) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    /**
     * 名称
     */
    private val mName: TextView = itemView.findViewById(R.id.name)

    /**
     * 图标
     */
    private val mIcon: ImageView = itemView.findViewById(R.id.icon)

    /**
     * 分割线
     */
    private val mLine: View = itemView.findViewById<View>(R.id.line)

    /**
     * Bind view.
     */
    open fun onBind(item: ServerConfig, position: Int, itemCount: Int,
                    adapter: ServerAdapter, defServer: String) {

        val name = item.name
        val str = "$name(${item.url})"
        val span = SpannableString(str)
        span.setSpan(colorSpan,
                name.length,
                str.length,
                SpannableString.SPAN_INCLUSIVE_INCLUSIVE)

        mName.text = span
        mIcon.visibility = if (item.url == defServer) View.VISIBLE else View.INVISIBLE
        mLine.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

        // OnClickListener
        itemView.setOnClickListener { v -> adapter.confirmConfig(v, adapterPosition) }
    }
}
