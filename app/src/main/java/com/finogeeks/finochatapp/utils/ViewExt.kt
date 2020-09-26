package com.finogeeks.finochatapp.utils

import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.finogeeks.finochat.utils.Log
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

/**
 * The extensions of [View]
 */

private const val TAG = "RxViewExt"

/**
 * The interval time between every double clicks of [View]
 */
private const val CLICK_INTERVAL_TIME: Long = 300

/**
 * Set clicks observer of [View],
 * This observer can observe single click or multi clicks of a [View] object.
 */
fun View.clicks(): Observable<Int> {
    return Observable.create<Int> {
        val observable = RxView.clicks(this).share()

        observable
                .doOnNext { _ -> it.onNext(1) } // 单击事件马上响应
                .buffer(observable.debounce(CLICK_INTERVAL_TIME, TimeUnit.MILLISECONDS))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ objects ->
                    // 非单击事件需间隔 CLICK_INTERVAL_TIME 的时长响应
                    if (objects.size > 1) { // 点击的次数大于1
                        it.onNext(objects.size)
                    }
                }, { Log.e(TAG, it.message.orEmpty()) })
    }
}

/**
 * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘，因为当用户点击EditText时没必要隐藏
 */
fun View?.isShouldHideInput(event: MotionEvent): Boolean {
    if (this is EditText) {
        val l = intArrayOf(0, 0)
        getLocationInWindow(l)
        val left = l[0]
        val top = l[1]
        val bottom = top + height
        val right = left + width
        return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
    }
    // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditView上，和用户用轨迹球选择其他的焦点
    return false
}