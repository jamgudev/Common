package com.jamgu.common.factory.mvp.presenter

import androidx.annotation.StringRes
import com.jamgu.common.factory.mvp.widget.recycler.BaseRecyclerAdapter

/**
 * Created by jamgu on 2022/02/09
 */
interface BaseContract {

    interface View<out T: Presenter> {

        fun showError(@StringRes stringRes: Int)

        fun showLoading()

        // UnsafeVariance 告诉编译器，我们不会对协变(只读)的类型进行逆变(只写)操作
        // 即不会有类型向下强转的安全问题
        fun setPresenter(presenter: @UnsafeVariance T?)
    }

    interface Presenter {

        fun start()

        fun destroy()

    }

    interface RecyclerView<out T: Presenter, ViewModel> : View<T> {

        fun getBaseRecyclerAdapter(): BaseRecyclerAdapter<ViewModel>?

        fun onAdapterDataChanged()

    }

}