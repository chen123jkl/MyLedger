package com.example.myledger

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myledger.adapter.TransactionAdapter
import com.example.myledger.databinding.ActivityMainBinding
import com.example.myledger.utils.CommonUtils // 导入工具类
import com.example.myledger.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        setupRecyclerView()
        observeData()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                val intent = Intent(this, AddEditActivity::class.java).apply {
                    putExtra("transaction_id", transaction.id)
                    putExtra("amount", transaction.amount)
                    putExtra("category", transaction.category)
                    putExtra("note", transaction.note)
                    putExtra("date", transaction.date)
                }
                startActivity(intent)
            },
            onItemDelete = { transaction ->
                viewModel.delete(transaction)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun observeData() {
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        viewModel.allTransactions.observe(this) { transactions ->
            adapter.submitList(transactions)

            // ======================
            // 新增：今日/本月消费 TOP10 排行榜
            // 工具类自动筛选 + 排序
            // ======================
            val todayTop10 = CommonUtils.getTodayTop10(transactions)       // 今日最贵10笔
            val monthTop10 = CommonUtils.getMonthTop10(transactions)     // 本月最贵10笔

            // 你可以在这里使用 TOP10 数据
            // 示例：打印日志查看是否生效
            if (todayTop10.isNotEmpty()) {
                val maxToday = todayTop10[0].amount
                // binding.tvTodayMax.text = "今日最高：¥$maxToday"
            }
            if (monthTop10.isNotEmpty()) {
                val maxMonth = monthTop10[0].amount
                // binding.tvMonthMax.text = "本月最高：¥$maxMonth"
            }

            // 原来的月度统计逻辑（不动）
            viewModel.getMonthlyTotal(currentYearMonth).observe(this) { total ->
                binding.tvMonthlyTotal.text = "¥${String.format("%.2f", total ?: 0.0)}"
            }
        }
    }
}