package com.example.myledger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myledger.data.Transaction
import com.example.myledger.databinding.ItemExpenseRecordBinding  // 注意：文件名变了
import com.example.myledger.utils.CommonUtils
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onItemDelete: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var transactions = listOf<Transaction>()

    fun submitList(list: List<Transaction>) {
        transactions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = transactions[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemDelete(item)
            true
        }
    }

    override fun getItemCount() = transactions.size

    class ViewHolder(private val binding: ItemExpenseRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            // 分类
            binding.tvExpenseType.text = transaction.category

            // 图标 + 颜色
            val style = CommonUtils.getCategoryStyle(transaction.category)
            binding.ivCategoryIcon.setImageResource(style.iconRes)
            binding.tvExpenseType.setTextColor(itemView.context.getColor(style.colorRes))

            // 时间格式化
            binding.tvExpenseTime.text = CommonUtils.formatTimeAgo(transaction.date)

            // 金额
            binding.tvExpenseAmount.text = "- ¥ ${String.format("%.2f", transaction.amount)}"
        }
    }
}