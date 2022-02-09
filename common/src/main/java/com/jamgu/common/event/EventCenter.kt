package com.jamgu.common.event

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Created by jamgu on 2022/02/09
 */
class EventCenter private constructor(){

    private var mLooper: Looper
    private var mHandler: Handler
    private val mLock = ReentrantReadWriteLock()
    private val mHandlerMap = HashMap<String, HashSet<WeakReference<IEventHandler>>>()

    init {
        if (!singleTonFlag) {
            singleTonFlag = true
        } else {
            throw Throwable("class[EventCenter]'s singleTon is being attacked.")
        }

        val handlerThread = HandlerThread("event_center_thread", Process.THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()

        mLooper = handlerThread.looper
        mHandler = object : Handler(mLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val eventMessage = msg.obj as? EventMessage ?: return
                val eventId = eventMessage.eventId
                val eventHandlerSet = HashSet<IEventHandler>()

                try {
                    mLock.readLock().lock()
                    mHandlerMap[eventId]?.let { weakRefSets ->
                        weakRefSets.forEach { weakRefHandler ->
                            val handler = weakRefHandler.get()
                            if (handler != null) {
                                eventHandlerSet.add(handler)
                            }
                        }
                    }
                } finally {
                    mLock.readLock().unlock()
                }

                eventHandlerSet.forEach { handler ->
                    handler.onEvent(eventMessage.data)
                }
            }
        }
    }

    fun getLooper() = mLooper

    /**
     * 根据 Class 类名注册事件
     * @see regEvent
     */
    fun regEvent(eventClass: Class<*>?, handler: IEventHandler?) {
        regEvent(eventClass?.canonicalName, handler)
    }

    /**
     * 通过字符串监听一个事件
     *
     * @param eventId event Id, used to identify the specific event.
     *              用来表明特定事件的ID
     * @param handler handler, supplies a callback to be invoked when specific event comes to EventCenter.
     *              提供一个当指定事件来临时的方法回调，在指定事件到来时，会被调用
     * @return Boolean, true if eventId is registered successfully, throws otherwise.
     *              注册成功时返回true，失败时会报错。
     */
    fun regEvent(eventId: String?, handler: IEventHandler?): Boolean {
        if (eventId.isNullOrEmpty() || handler == null) return false
        try {
            mLock.writeLock().lock()
            return regEventInner(eventId, handler)
        } finally {
            mLock.writeLock().unlock()
        }
    }

    private fun regEventInner(eventId: String, handler: IEventHandler): Boolean {
        var weakRefHandlerSet = mHandlerMap[eventId]
        if (weakRefHandlerSet != null) {
            var exist = false
            run outside@{
                weakRefHandlerSet?.forEach {
                    val oldHandler = it.get()
                    if (oldHandler == handler) {
                        exist = true
                        return@outside
                    }
                }
            }

            if (!exist) {
                weakRefHandlerSet.add(WeakReference(handler))
            } else {
                throw Throwable("this eventId has already registered in EventCenter, " +
                        "pls use another event id to make sure the event you want to listen is registered successfully.")
            }
        } else {
            weakRefHandlerSet = HashSet()
            weakRefHandlerSet.add(WeakReference(handler))
            mHandlerMap[eventId] = weakRefHandlerSet
        }
        return true
    }

    /**
     * 根据 Class 类名解注册事件
     * @param eventClass eventClass
     * @param handler handler
     */
    fun unRegEvent(eventClass: Class<*>?, handler: IEventHandler?) {
        unRegEvent(eventClass?.canonicalName, handler)
    }

    /**
     * 通过 事件 字符串解注册事件
     * @param eventId eventId
     * @param handler handler
     */
    fun unRegEvent(eventId: String?, handler: IEventHandler?): Boolean {
        if (eventId.isNullOrEmpty() || handler == null) return false

        try {
            mLock.writeLock().lock()
            return unRegEventInner(eventId, handler)
        } finally {
            mLock.writeLock().unlock()
        }
    }

    /**
     * 清空所有事件监听
     */
    fun unRegAllEvent() {
        try {
            mLock.writeLock().lock()
            mHandlerMap.clear()
        } finally {
            mLock.writeLock().unlock()
        }
    }

    private fun unRegEventInner(eventId: String, handler: IEventHandler): Boolean {
        val weakRefHandlerSet = mHandlerMap[eventId]
        if (weakRefHandlerSet != null) {
            var theHandlerRef: WeakReference<IEventHandler>? = null
            run outside@{
                weakRefHandlerSet.forEach {
                    if (it.get() == handler) {
                        theHandlerRef = it
                        return@outside
                    }
                }
            }
            return if (theHandlerRef != null) {
                weakRefHandlerSet.remove(theHandlerRef)
            } else false
        } else {
            // 没有提供 event id, 会遍历 Map 找相同的，会比较耗时
            mHandlerMap.keys.forEach { key ->
                val refHandlerSet = mHandlerMap[key] ?: return@forEach
                var theHandlerRef: WeakReference<IEventHandler>? = null
                run outside@{
                    refHandlerSet.forEach {
                        if (it.get() == handler) {
                            theHandlerRef = it
                            return@outside
                        }
                    }
                }
                if (theHandlerRef != null) {
                    refHandlerSet.remove(theHandlerRef)
                    return true
                }
            }
            return false
        }
    }

    /**
     * 向指定事件监听器，发送一条消息
     */
    fun postEvent(eventId: String?, data: Any?) {
        eventId ?: return

        val msg = mHandler.obtainMessage()
        msg.obj = EventMessage(eventId, data)
        mHandler.sendMessage(msg)
    }

    companion object {
        private const val TAG = "EventCenter"

        private var singleTonFlag = false

        @JvmStatic
        fun getInstance() = EventCenterSingleTonHolder.holder
    }

    /**
     * 静态内部类单例
     */
    private object EventCenterSingleTonHolder {
        val holder = EventCenter()
    }

}