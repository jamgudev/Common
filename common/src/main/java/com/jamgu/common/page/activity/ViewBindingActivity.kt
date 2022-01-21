package com.jamgu.common.page.activity

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding

/**
 * Created by jamgu on 2022/01/21
 *
 * 封装了ViewBinding的Activity，继承自[BaseActivity]
 */
abstract class ViewBindingActivity<T: ViewBinding>: BaseActivity() {

    protected lateinit var mBinding : T

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = getViewBinding()
        setContentView(mBinding.root)
    }

    abstract fun getViewBinding(): T

}