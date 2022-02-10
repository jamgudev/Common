package com.jamgu.common.page.fragment

import androidx.fragment.app.Fragment
import com.jamgu.common.page.activity.BaseActivity

/**
 * Created by jamgu on 2022/02/10
 */
open class BaseFragment: Fragment() {

    @JvmOverloads
    fun showLoading(text: String? = "loading...") {
        (activity as? BaseActivity)?.showProgress(text)
    }

    fun hideLoading() {
        (activity as? BaseActivity)?.hideProgress()
    }

}