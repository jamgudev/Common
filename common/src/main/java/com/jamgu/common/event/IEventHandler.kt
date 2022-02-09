package com.jamgu.common.event

/**
 * Created by jamgu on 2022/02/09
 *
 */
interface IEventHandler {
    /**
     * 该方法在子线程回调，如有 UI 操作，需要手动切回 UI 线程
     */
    fun onEvent(data: Any?)
}