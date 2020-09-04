package com.finogeeks.finochatapp.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object MemoryLeakUtil {
    fun fix(context: Context) {
        val m = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val views = arrayOf("mCurRootView", "mServedView", "mNextServedView")

        for (view in views) {
            try {
                val field = m.javaClass.getDeclaredField(view)
                field.isAccessible = true

                val fieldObj = field.get(m)
                if (fieldObj is View) {
                    if (fieldObj.context === context) {
                        field.set(m, null)
                    } else {
                        break
                    }
                }
            } catch (ignore: Exception) {
            }
        }
    }
}
