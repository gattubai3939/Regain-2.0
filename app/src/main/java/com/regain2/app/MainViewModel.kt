package com.regain2.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.regain2.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app
    private val repo = UsageRepository(app)
    private val db = AppDatabase.get(app)
    private val dao = db.categoryDao()
    private val ruleDao = db.ruleDao()
    private var overrides: Map<String, AppCategory> = emptyMap()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _summary = MutableStateFlow(UsageSummary())
    val summary: StateFlow<UsageSummary> = _summary

    val rules: StateFlow<List<AppRule>> =
        ruleDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _guardEnabled = MutableStateFlow(false)
    val guardEnabled: StateFlow<Boolean> = _guardEnabled
    private val _unlocks = MutableStateFlow(0)
    val unlocks: StateFlow<Int> = _unlocks
    private val _pickups = MutableStateFlow(0)
    val pickups: StateFlow<Int> = _pickups
    private val _pauseShown = MutableStateFlow(0)
    val pauseShown: StateFlow<Int> = _pauseShown
    private val _pauseBack = MutableStateFlow(0)
    val pauseBack: StateFlow<Int> = _pauseBack
    private val _installed = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val installedApps: StateFlow<List<Pair<String, String>>> = _installed

    init {
        viewModelScope.launch {
            dao.observeAll().collect { list ->
                overrides = list.associate { it.packageName to AppCategory.valueOf(it.category) }
                reload()
            }
        }
    }

    /** Call on every onResume. */
    fun refresh() {
        _guardEnabled.value = GuardService.isEnabled(ctx)
        _unlocks.value = Stats.get(ctx, "unlocks")
        _pickups.value = Stats.get(ctx, "pickups")
        _pauseShown.value = Stats.get(ctx, "pause_shown")
        _pauseBack.value = Stats.get(ctx, "pause_back")
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
        viewModelScope.launch(Dispatchers.IO) { dao.upsert(CategoryOverride(pkg, category.name)) }
    }

    fun saveRule(rule: AppRule) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!rule.blocked && rule.limitMinutes == 0 && !rule.pause) ruleDao.delete(rule.packageName)
            else ruleDao.upsert(rule)
        }
    }

    fun deleteRule(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) { ruleDao.delete(pkg) }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) { _installed.value = repo.launcherApps() }
    }
}
