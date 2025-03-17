package com.blueeve.remoney

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SquareAdapter(
    private val items: MutableList<Device>,
    private val onItemLongClick: (Device) -> Unit
) : RecyclerView.Adapter<SquareAdapter.ViewHolder>() {

    val selectedItems = mutableSetOf<Device>() // 存储选中的设备
    private var isSelectionMode = false

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_square, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = items[position]
        holder.textView.text = "ID: ${device.id},\n 名称: ${device.name},\n价格: ${device.price},\n 购买时间: ${device.buy_time},\n"

        // 计算并显示购买时间距今的天数
        val purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(device.buy_time)
        val daysSincePurchase = purchaseDate?.let {
            val currentDate = Date()
            val diff = currentDate.time - it.time // 获取时间差
            ((diff / (1000 * 60 * 60 * 24)) + 1).toInt() // 计算天数，+1 以包含当天
        } ?: 0

        holder.textView.append(" (距今 $daysSincePurchase 天)") // 显示天数

        // 计算日均值
        val dailyMoney = device.price.toDouble() / daysSincePurchase
        // 使用 String.format 格式化为小数点后 2 位
        val formattedDailyMoney = String.format("%.2f", dailyMoney)
        // 显示日均值
        holder.textView.append("\n\n日均: $formattedDailyMoney")

        // 根据选择状态改变视图外观
        holder.itemView.isSelected = selectedItems.contains(device)
        holder.itemView.background = if (selectedItems.contains(device)) {
            holder.itemView.context.getDrawable(R.drawable.card_background_selected)
        } else {
            holder.itemView.context.getDrawable(R.drawable.card_background_default)
        }

        // 设置长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick(device)
            true
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(device)
            }
        }
    }

    override fun getItemCount() = items.size

    fun addItem(device: Device) {
        items.add(device)
        notifyItemInserted(items.size - 1)
    }

    fun updateItem(position: Int, device: Device) {
        items[position] = device
        notifyItemChanged(position)
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun enableSelectionMode() {
        isSelectionMode = true
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun disableSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(device: Device) {
        if (selectedItems.contains(device)) {
            selectedItems.remove(device)
        } else {
            selectedItems.add(device)
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    // 批量删除选中的设备
    fun deleteSelectedItems() {
        selectedItems.forEach { device ->
            val position = items.indexOf(device)
            if (position >= 0) {
                removeItem(position)
            }
        }
        clearSelection() // 清空选择
    }
}
