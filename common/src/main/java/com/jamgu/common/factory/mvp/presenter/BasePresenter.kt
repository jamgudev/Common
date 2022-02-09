package com.jamgu.common.factory.mvp.presenter

/**
 * Created by jamgu on 2022/02/09
 */
abstract class BasePresenter<out V: BaseContract.View<BaseContract.Presenter>>(v: V?) : BaseContract.Presenter {

    private var mView: V? = null

    init {
        setView(v)
    }

    private fun setView(v: V?) {
        mView = v
        v?.setPresenter(this)
    }

    fun getView(): V? {
        return mView
    }

    override fun start() {
    }

    override fun destroy() {
        val view = mView
        mView = null
        view?.setPresenter(null)
    }
}