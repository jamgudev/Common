package com.jamgu.common.factory.mvp.presenter

import androidx.recyclerview.widget.DiffUtil
import com.jamgu.common.thread.ThreadPool

/**
 * Created by jamgu on 2022/02/09
 */
open class BaseRecyclerPresenter<ViewModel, out View: BaseContract.RecyclerView<BaseContract.Presenter, ViewModel>>(v: View?)
    : BasePresenter<View>(v) {

    protected fun refreshData(dataList: List<ViewModel>?) {

        val view = getView()
        view?.let {
            val adapter = it.getBaseRecyclerAdapter() ?: return

            ThreadPool.runUITask {
                adapter.replace(dataList)
                view.onAdapterDataChanged()
            }
        }
    }

    protected fun refreshData(diff: DiffUtil.DiffResult?, dataList: List<ViewModel>?) {
        if (diff == null || (dataList == null || dataList.isEmpty())) return

        ThreadPool.runUITask {
            refreshDataOnUiThread(diff, dataList)
        }

    }

    private fun refreshDataOnUiThread(diff: DiffUtil.DiffResult, dataList: List<ViewModel>) {
        getView()?.let {
            val adapter = it.getBaseRecyclerAdapter() ?: return
            adapter.getItems().clear()
            adapter.getItems().addAll(dataList)

            // 通知页面数据更新
            it.onAdapterDataChanged()

            // recyclerview 进行增量更新
            diff.dispatchUpdatesTo(adapter)
        }
    }


}