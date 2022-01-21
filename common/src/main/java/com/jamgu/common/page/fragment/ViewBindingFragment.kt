package com.jamgu.common.page.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Created by jamgu on 2022/01/21
 */
abstract class ViewBindingFragment<T: ViewBinding>: Fragment() {

    protected lateinit var mBinding : T

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = getViewBinding()
        return mBinding.root
    }

    abstract fun getViewBinding(): T
}