package com.finogeeks.finochatapp.modules.server.adapter.holder

import android.app.Activity
import android.content.Intent
import android.graphics.PointF
import androidx.appcompat.widget.PopupMenu
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.finogeeks.finochatapp.R
import com.finogeeks.finochatapp.modules.server.adapter.ServerAdapter
import com.finogeeks.finochatapp.modules.server.model.ServerConfig
import com.finogeeks.finochatapp.modules.server.view.ServerSettingActivity

class LocalHolder(itemView: View, private val activity: Activity,
                  colorSpan: ForegroundColorSpan) : ServerHolder(itemView, colorSpan) {

    /**
     * Long Click point x,y
     */
    private val point = PointF()

    override fun onBind(item: ServerConfig, position: Int, itemCount: Int, adapter: ServerAdapter, defServer: String) {
        super.onBind(item, position, itemCount, adapter, defServer)
        longClick(item, adapter)
    }

    private fun longClick(config: ServerConfig, adapter: ServerAdapter) {
        // OnTouchListener
        itemView.setOnTouchListener { _, event ->
            when {
                event.actionMasked == MotionEvent.ACTION_DOWN -> {
                    point.set(event.x, event.y)
                    return@setOnTouchListener false
                }

                else -> return@setOnTouchListener false
            }
        }

        // OnLongClickListener
        itemView.setOnLongClickListener {
            val loc = IntArray(2)
            itemView.getLocationInWindow(loc)
            point.offset(loc[0].toFloat(), loc[1].toFloat())

            val anchor = View(activity)
            anchor.layoutParams = ViewGroup.LayoutParams(0, 0)

            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            root.addView(anchor)

            anchor.x = point.x
            anchor.y = point.y

            PopupMenu(activity, anchor, Gravity.CENTER).apply {
                setOnDismissListener { root.removeView(anchor) }
                menuInflater.inflate(R.menu.server_delete_address, menu)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.tags_delete -> adapter.removeLocal(adapterPosition)
                        R.id.tags_mod -> {
                            Intent(activity, ServerSettingActivity::class.java)
                                    .putExtra(ServerSettingActivity.PREV_SERVER_NAME, config.name)
                                    .putExtra(ServerSettingActivity.PREV_SERVER_URL, config.url)
                                    .run { activity.startActivity(this) }
                        }
                    }

                    return@setOnMenuItemClickListener true
                }
                show()
            }

            return@setOnLongClickListener true
        }
    }
}