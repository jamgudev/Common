package com.jamgu.common.page.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

/**
 * Created by jamgu on 2022/01/21
 */
abstract class ViewBindingFragment<T: ViewBinding>: BaseFragment() {

    protected val mBinding get() = _binding!!
    private var _binding : T? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (_binding == null) {
            _binding = getViewBinding()
        }

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (arguments == null || checkArgs(arguments!!)) {
            initView()
            initData()
        }
    }

    /**
     * 检查入口参数是否正确
     */
    protected fun checkArgs(bundle: Bundle) = true

    /**
     * 在此初始化布局
     */
    abstract fun initView()

    /**
     * 在此初始化数据
     */
    abstract fun initData()

    /**
     * 返回对应的ViewBinding
     */
    abstract fun getViewBinding(): T

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}