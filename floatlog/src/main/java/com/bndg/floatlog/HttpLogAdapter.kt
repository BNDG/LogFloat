package com.bndg.floatlog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HttpLogAdapter(
    private val logEvents: MutableList<HttpLogEvent>,
    private val onItemClick: (HttpLogEvent) -> Unit,
    private val onItemLongClick: (View, HttpLogEvent) -> Unit
) : RecyclerView.Adapter<HttpLogAdapter.SimpleViewHolder>() {

    fun getData(): List<HttpLogEvent> {
        return logEvents
    }

    // 3. 创建 ViewHolder
    class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv_url: TextView = itemView.findViewById(R.id.tv_url)
        val tv_params: TextView = itemView.findViewById(R.id.tv_params)
        val tv_header: TextView = itemView.findViewById(R.id.tv_header)
    }

    // 4. 创建 ViewHolder 实例并返回
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.float_view_logitem, parent, false)
        return SimpleViewHolder(view)
    }

    // 6. 获取 HttpLogEvent 数量
    override fun getItemCount(): Int {
        return logEvents.size
    }

    // 5. 绑定数据
    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val currentHttpLogEvent = logEvents[position]
        holder.tv_url.text = currentHttpLogEvent.url
        holder.tv_params.text = currentHttpLogEvent.params
        holder.tv_header.text = "{\"accessToken\":\"" + currentHttpLogEvent.header + "\"}"
        if (currentHttpLogEvent.header?.isNotEmpty() == true) {
            holder.tv_header.visibility = View.VISIBLE
        } else {
            holder.tv_header.visibility = View.GONE
        }
        // 设置点击监听
        holder.tv_url.setOnClickListener {
            onItemClick(currentHttpLogEvent) // 执行点击回调
        }

        holder.tv_params.setOnClickListener {
            onItemClick(currentHttpLogEvent) // 执行点击回调
        }

        // 设置长按监听
        holder.tv_url.setOnLongClickListener {
            onItemLongClick(holder.tv_url, currentHttpLogEvent) // 执行长按回调
            true // 返回 true 表示长按事件已被消费
        }

        holder.tv_params.setOnLongClickListener {
            onItemLongClick(holder.tv_params, currentHttpLogEvent) // 执行长按回调
            true // 返回 true 表示长按事件已被消费
        }
    }

    fun addData(event: HttpLogEvent) {
        logEvents.add(event)
        notifyItemInserted(logEvents.size - 1)
    }
}