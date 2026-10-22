package com.regain2.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.regain2.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UsageRepository(app)
    private val dao = AppDatabase.get(app).categoryDao()
    private var overrides: Map<String, AppCategory> = emptyMap()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _summary = MutableStateFlow(UsageSummary())
    val summary: StateFlow<UsageSummary> = _summary

    init {
        viewModelScope.launch {
            dao.observeAll().collect { list ->
                overrides = list.associate { it.packageName to AppCategory.valueOf(it.category) }
                reload()
            }
        }
    }

    /** Call on every onResume (also detects permission being granted). */
    fun refresh() {
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        val granted = repo.hasUsagePermission()
        _hasPermission.value = granted
        if (granted) {
            _summary.value = withContext(Dispatchers.IO) { repo.loadToday(overrides) }
        }
    }

    fun setCategory(pkg: String, category: AppCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsert(CategoryOverride(pkg, category.name))
        }
    }
}
