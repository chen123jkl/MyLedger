package com.example.myledger


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myledger.data.Transaction
import com.example.myledger.databinding.ActivityAddEditBinding
import com.example.myledger.viewmodel.TransactionViewModel
import java.util.*

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var viewModel: TransactionViewModel
    private var editingTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        val transactionId = intent.getIntExtra("transaction_id", -1)
        if (transactionId != -1) {
            editingTransaction = Transaction(
                id = transactionId,
                amount = intent.getDoubleExtra("amount", 0.0),
                category = intent.getStringExtra("category") ?: "",
                note = intent.getStringExtra("note") ?: "",
                date = intent.getLongExtra("date", System.currentTimeMillis())
            )
            populateFields()
        }

        binding.btnSave.setOnClickListener { saveTransaction() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun populateFields() {
        editingTransaction?.let {
            binding.etAmount.setText(it.amount.toString())
            binding.etCategory.setText(it.category)
            binding.etNote.setText(it.note)
            val calendar = Calendar.getInstance().apply { timeInMillis = it.date }
            binding.datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "金额无效", Toast.LENGTH_SHORT).show()
            return
        }
        val category = binding.etCategory.text.toString().trim()
        if (category.isEmpty()) {
            Toast.makeText(this, "请输入分类", Toast.LENGTH_SHORT).show()
            return
        }
        val note = binding.etNote.text.toString().trim()

        val calendar = Calendar.getInstance().apply {
            set(binding.datePicker.year, binding.datePicker.month, binding.datePicker.dayOfMonth)
        }
        val date = calendar.timeInMillis

        val transaction = Transaction(
            id = editingTransaction?.id ?: 0,
            amount = amount,
            category = category,
            note = note,
            date = date
        )

        if (editingTransaction == null) {
            viewModel.insert(transaction)
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.update(transaction)
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}