package com.jamgu.common.factory.mvp.widget.page

import android.content.Context
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.jamgu.common.factory.mvp.presenter.BaseContract
import com.jamgu.common.page.fragment.ViewBindingFragment

/**
 * Created by jamgu on 2022/02/09
 */
abstract class PresenterFragment<Presenter: BaseContract.Presenter, VB: ViewBinding>
    : ViewBindingFragment<VB>(), BaseContract.View<Presenter>{

    protected var mPresenter: Presenter? = null

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)

        initPresenter()
    }

    override fun setPresenter(presenter: Presenter?) {
        mPresenter = presenter
    }

    /**
     * 初始化Presenter
     */
    abstract fun initPresenter(): Presenter

    override fun onDestroy() {
        super.onDestroy()
        mPresenter?.destroy()
    }

}