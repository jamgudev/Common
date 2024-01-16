package com.jamgu.common.factory.mvp.widget.page

import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.jamgu.common.factory.mvp.presenter.BaseContract
import com.jamgu.common.page.activity.BaseActivity
import com.jamgu.common.page.activity.ViewBindingActivity

/**
 * Created by jamgu on 2022/02/09
 */
abstract class PresenterActivity<Presenter: BaseContract.Presenter>
    : BaseContract.View<Presenter>, BaseActivity(){

    protected var mPresenter: Presenter? = null

    @CallSuper
    override fun initWidget() {
        initPresenter()
        super.initWidget()
    }

    abstract fun initPresenter(): Presenter

    override fun setPresenter(presenter: Presenter?) {
        mPresenter = presenter
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destroy()
    }
}