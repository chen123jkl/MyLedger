package com.example.myledger.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myledger.data.AppDatabase
import com.example.myledger.data.Transaction
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).transactionDao()

    val allTransactions = dao.getAll().asLiveData()

    fun insert(transaction: Transaction) = viewModelScope.launch {
        dao.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        dao.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        dao.delete(transaction)
    }

    fun getMonthlyTotal(yearMonth: String): LiveData<Double?> =
        dao.getTotalByMonth(yearMonth).asLiveData()
}