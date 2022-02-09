package com.jamgu.common.factory.mvp.widget.recycler

/**
 * Created by jamgu on 2022/02/09
 */
interface AdapterCallback<Data> {
    /**
     * 当某个子View的数据发生变化时，调用以更新子View的数据
     */
    fun updateChildData(data: Data, holder: BaseRecyclerAdapter.BaseViewHolder<Data>)
}