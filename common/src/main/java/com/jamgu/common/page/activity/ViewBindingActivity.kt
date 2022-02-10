package com.jamgu.common.page.activity

import android.os.Bundle
import androidx.viewbinding.ViewBinding

/**
 * Created by jamgu on 2022/01/21
 *
 * 封装了ViewBinding的Activity，继承自[BaseActivity]
 */
abstract class ViewBindingActivity<T: ViewBinding>: BaseActivity() {

    protected lateinit var mBinding : T

    override fun onSetContentView(savedInstanceState: Bundle?) {
        mBinding = getViewBinding()
        setContentView(mBinding.root)
    }

    abstract fun getViewBinding(): T

}