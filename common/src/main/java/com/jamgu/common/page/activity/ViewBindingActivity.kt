package com.jamgu.common.page.activity

import android.app.Activity
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.jamgu.common.util.viewbinding.ActivityViewBindingDelegate

/**
 * Created by jamgu on 2022/01/21
 *
 * 封装了ViewBinding的Activity，继承自[BaseActivity]
 *
 */
@Deprecated(message = "继承需要强耦合，不推荐使用",
    replaceWith = ReplaceWith(
        expression = "private val binding: ActivityYouWannaBind_Binding by viewBinding()",
        imports = ["com.jamgu.common.util.viewbinding"]))
abstract class ViewBindingActivity<T: ViewBinding>: BaseActivity() {

    protected lateinit var mBinding : T

    override fun onSetContentView(savedInstanceState: Bundle?) {
        mBinding = getViewBinding()
        setContentView(mBinding.root)
    }

    abstract fun getViewBinding(): T

}