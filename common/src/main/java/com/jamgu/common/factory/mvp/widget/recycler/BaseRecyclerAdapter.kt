package com.jamgu.common.factory.mvp.widget.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.R

/**
 * Created by jamgu on 2022/02/09
 */
abstract class BaseRecyclerAdapter<Data>(data: ArrayList<Data> = ArrayList()) : RecyclerView.Adapter<BaseRecyclerAdapter.BaseViewHolder<Data>>(),
    View.OnClickListener, View.OnLongClickListener, AdapterCallback<Data> {

    private var mData: ArrayList<Data> = data
    private var mListener: AdapterClickListener<Data>? = null

    constructor(dataList: ArrayList<Data>?, listener: AdapterClickListener<Data>? = null): this(dataList?: ArrayList()) {
        this.mListener = listener
    }

    /**
     * 设置RecyclerView的点击事件
     */
    fun setAdapterClickListener(listener: AdapterClickListener<Data>?) {
        this.mListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return getItemViewType(position, mData[position])
    }

    /**
     * @return  返回数据想要的布局类型
     *          根据数据显示的需要指定特定的布局类型，
     *          可直接为 layout res id
     */
    protected abstract fun getItemViewType(position: Int, data: Data): Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Data> {
        val inflater = LayoutInflater.from(parent.context)
        val rootView = inflater.inflate(viewType, parent, false)

        val holder = onCreateViewHolder(rootView, viewType)

        rootView.setOnClickListener(this)
        rootView.setOnLongClickListener(this)

        rootView.setTag(R.id.tag_mvp_recycler_holder, holder)

        holder.mAdapterCallback = this

        return holder
    }

    /**
     * @return 根据[getItemViewType]为每个子View指定的布局类型，返回指定的ViewHolder
     */
    protected abstract fun onCreateViewHolder(view: View, viewType: Int): BaseViewHolder<Data>

    override fun onBindViewHolder(holder: BaseViewHolder<Data>, position: Int) {
        val data = mData[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun add(data: Data) {
        mData.add(data)
        notifyItemInserted(mData.size - 1)
    }

    fun add(vararg dataList: Data?) {
        dataList.forEach {
            var addSize = 0
            if (it != null) {
                mData.add(it)
                addSize++
            }

            notifyItemChanged(mData.size - addSize, mData.size)
        }
    }

    fun clear() {
        mData.clear()
        notifyDataSetChanged()
    }

    fun replace(dataList: List<Data>?) {
        mData.clear()
        if (dataList == null || dataList.isEmpty()) return
        mData.addAll(dataList)
        notifyDataSetChanged()
    }

    /**
     * 设置RecyclerView的子View更新事件，默认实现如下，可重写
     */
    override fun updateChildData(data: Data, holder: BaseViewHolder<Data>) {
        val pos = holder.adapterPosition
        if (pos >= 0) {
            mData.removeAt(pos)
            mData.add(pos, data)
            notifyItemChanged(pos)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onClick(v: View?) {
        if (v == null) return

        val holder = v.getTag(R.id.tag_mvp_recycler_holder) as? BaseViewHolder<Data> ?: return

        mListener?.onItemClick(holder, mData[holder.adapterPosition])
    }

    @Suppress("UNCHECKED_CAST")
    override fun onLongClick(v: View?): Boolean {
        if (v == null) return false
        val holder = v.getTag(R.id.tag_mvp_recycler_holder) as? BaseViewHolder<Data> ?: return false

        return mListener?.onItemLongClick(holder, mData[holder.adapterPosition]) ?: false
    }

    fun getItems(): ArrayList<Data> {
        return mData
    }


    abstract class BaseViewHolder<Data>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        protected var mData: Data? = null
        var mAdapterCallback: AdapterCallback<Data>? = null

        fun bind(data: Data?) {
            mData = data
            onBind(data)
        }

        protected abstract fun onBind(data: Data?)

        /**
         * 用于更新该子View
         */
        fun updateData(data: Data?) {
            if (data == null) return
            mAdapterCallback?.updateChildData(data, this)
        }

    }

    /**
     * RecyclerView的点击事件，推荐继承[SimpleAdapterClicker]来实现
     */
    interface AdapterClickListener<Data> {
        /**
         * recyclerView 单个Item的点击事件
         */
        fun onItemClick(holder: BaseViewHolder<Data>?, data: Data?)

        /**
         * recyclerView 单个Item的长按事件
         */
        fun onItemLongClick(holder: BaseViewHolder<Data>?, data: Data?): Boolean
    }

    abstract class SimpleAdapterClicker<Data>: AdapterClickListener<Data> {

        override fun onItemClick(holder: BaseViewHolder<Data>?, data: Data?) {
        }

        override fun onItemLongClick(holder: BaseViewHolder<Data>?, data: Data?): Boolean {
            return false
        }

    }

}