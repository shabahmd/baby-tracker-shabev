package com.nestling.baby.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nestling.baby.R
import com.nestling.baby.data.NestlingRepository
import com.nestling.baby.data.SettingsStore
import com.nestling.baby.di.Graph
import com.nestling.baby.domain.ActiveTimer
import com.nestling.baby.domain.AppSettings
import com.nestling.baby.domain.BabyEvent
import com.nestling.baby.domain.DiaperKind
import com.nestling.baby.domain.EventType
import com.nestling.baby.domain.NightSuggestion
import com.nestling.baby.domain.Side
import com.nestling.baby.domain.VolumeUnit
import com.nestling.baby.platform.AndroidPlatformEffects
import com.nestling.baby.platform.PlatformEffects
import com.nestling.baby.util.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NestlingUiState(
    val settings: AppSettings = AppSettings(),
    val activeTimer: ActiveTimer? = null,
    val today: LocalDate = LocalDate.now(),
    val todayEvents: List<BabyEvent> = emptyList(),
    val historyDate: LocalDate = LocalDate.now(),
    val historyEvents: List<BabyEvent> = emptyList(),
    val query: String = "",
    val searchResults: List<BabyEvent> = emptyList(),
    val suggestNightMode: Boolean = false,
) {
    val searching: Boolean get() = query.isNotBlank()
    val unit: VolumeUnit get() = settings.unit
}

/** A one-shot message for the snackbar. Errors are snackbars here, never crash dialogs. */
data class UiMessage(
    @param:StringRes val text: Int,
    val detail: String? = null,
    val editAmountForEventId: Long? = null,
)

/**
 * One ViewModel for a two screen app. It owns nothing but state: all writes go straight
 * to Room, so killing the process mid-feed loses nothing.
 */
class NestlingViewModel(
    private val repository: NestlingRepository,
    private val settingsStore: SettingsStore,
    private val effects: PlatformEffects = PlatformEffects.None,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    private val _state = MutableStateFlow(NestlingUiState(today = todayDate(), historyDate = todayDate()))
    val state: StateFlow<NestlingUiState> = _state.asStateFlow()

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    private val dayFlow = MutableStateFlow(todayDate())
    private val queryFlow = MutableStateFlow("")
    private val todayFlow = MutableStateFlow(todayDate())

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                _state.update { it.copy(settings = settings, suggestNightMode = shouldSuggestNight(settings)) }
            }
        }
        viewModelScope.launch {
            repository.observeActiveTimer().collect { timer ->
                _state.update { it.copy(activeTimer = timer) }
            }
        }
        viewModelScope.launch {
            todayFlow.flatMapLatest { date -> repository.observeDay(date) }.collect { events ->
                _state.update { it.copy(todayEvents = events, today = todayFlow.value) }
            }
        }
        viewModelScope.launch {
            dayFlow.flatMapLatest { date -> repository.observeDay(date) }.collect { events ->
                _state.update { it.copy(historyEvents = events, historyDate = dayFlow.value) }
            }
        }
        viewModelScope.launch {
            queryFlow.flatMapLatest { query ->
                if (query.isBlank()) flowOf(emptyList()) else repository.search(query)
            }.collect { results ->
                _state.update { it.copy(searchResults = results, query = queryFlow.value) }
            }
        }
    }

    // ---------------------------------------------------------------- logging

    /**
     * Bottle and Sleep are toggles: first tap starts the timer, second tap stops and
     * saves. Two taps, no dialogs in between.
     */
    fun toggleTimer(type: EventType) {
        viewModelScope.launch {
            val running = repository.currentTimer()
            when {
                running == null -> startTimer(type)
                running.type == type -> stopTimer()
                else -> {
                    stopTimer()
                    startTimer(type)
                }
            }
        }
    }

    private suspend fun startTimer(type: EventType) {
        val settings = _state.value.settings
        repository.startTimer(type, settings.lastSide.takeIf { type == EventType.BOTTLE })
        effects.timerStarted(type)
    }

    /**
     * Stopping saves immediately using the last amount the parent entered. The amount
     * sheet is offered afterwards from the snackbar — the event is already safe on disk.
     */
    fun stopTimer() {
        viewModelScope.launch {
            val running = repository.currentTimer() ?: return@launch
            val settings = _state.value.settings
            val bottle = running.type == EventType.BOTTLE
            val amount = settings.lastAmountMl.takeIf { bottle }
            val id = repository.stopTimer(amountMl = amount, side = settings.lastSide.takeIf { bottle })
            effects.timerStopped()
            effects.dataChanged()
            messageChannel.send(
                UiMessage(
                    text = if (bottle) R.string.msg_saved_bottle else R.string.msg_saved_sleep,
                    detail = amount?.let { TimeFormat.amount(it, settings.unit) },
                    editAmountForEventId = id.takeIf { bottle },
                ),
            )
        }
    }

    fun discardTimer() {
        viewModelScope.launch {
            repository.discardTimer()
            effects.timerStopped()
            messageChannel.send(UiMessage(R.string.msg_timer_discarded))
        }
    }

    fun logDiaper(kind: DiaperKind) {
        viewModelScope.launch {
            repository.logDiaper(kind)
            effects.dataChanged()
            messageChannel.send(UiMessage(R.string.msg_saved_diaper))
        }
    }

    fun saveAmount(eventId: Long, amountMl: Int, side: Side?) {
        viewModelScope.launch {
            repository.updateEvent(eventId, amountMl = amountMl, side = side)
            settingsStore.update { it.copy(lastAmountMl = amountMl, lastSide = side ?: it.lastSide) }
            effects.dataChanged()
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            effects.dataChanged()
            messageChannel.send(UiMessage(R.string.msg_deleted))
        }
    }

    suspend fun eventById(id: Long): BabyEvent? = repository.eventById(id)

    suspend fun allEventsForExport(): List<BabyEvent> = repository.allEventsAscending()

    // --------------------------------------------------------------- browsing

    fun selectDate(date: LocalDate) {
        dayFlow.value = date
        _state.update { it.copy(historyDate = date) }
    }

    fun shiftDay(days: Long) {
        val next = dayFlow.value.plusDays(days)
        if (next.isAfter(todayDate())) return
        selectDate(next)
    }

    fun setQuery(query: String) {
        queryFlow.value = query
        _state.update { it.copy(query = query) }
    }

    /** Called when the app comes back to the foreground so "Today" is still today. */
    fun refreshToday() {
        val today = todayDate()
        if (todayFlow.value != today) {
            todayFlow.value = today
            _state.update { it.copy(today = today, suggestNightMode = shouldSuggestNight(it.settings)) }
        } else {
            _state.update { it.copy(suggestNightMode = shouldSuggestNight(it.settings)) }
        }
    }

    // --------------------------------------------------------------- settings

    fun setNightMode(enabled: Boolean) {
        viewModelScope.launch { settingsStore.update { it.copy(nightMode = enabled) } }
    }

    fun setUnit(unit: VolumeUnit) {
        viewModelScope.launch { settingsStore.update { it.copy(unit = unit) } }
    }

    fun setBabyName(name: String) {
        val cleaned = name.trim().take(24)
        if (cleaned.isEmpty()) return
        viewModelScope.launch { settingsStore.update { it.copy(babyName = cleaned) } }
    }

    /** "Not tonight" — stop asking about night mode until tomorrow. */
    fun dismissNightSuggestion() {
        viewModelScope.launch {
            settingsStore.update { it.copy(nightPromptSuppressedOn = todayDate().toString()) }
            _state.update { it.copy(suggestNightMode = false) }
        }
    }

    fun showError() {
        viewModelScope.launch { messageChannel.send(UiMessage(R.string.msg_generic_error)) }
    }

    fun showMessage(@StringRes text: Int) {
        viewModelScope.launch { messageChannel.send(UiMessage(text)) }
    }

    private fun todayDate(): LocalDate = Instant.ofEpochMilli(clock()).atZone(zone()).toLocalDate()

    /** After 10pm (or before 5am) offer the dim red night theme, once per day. */
    private fun shouldSuggestNight(settings: AppSettings): Boolean = NightSuggestion.shouldSuggest(
        hourOfDay = Instant.ofEpochMilli(clock()).atZone(zone()).hour,
        nightModeEnabled = settings.nightMode,
        suppressedOn = settings.nightPromptSuppressedOn,
        today = todayDate().toString(),
    )

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    NestlingViewModel(
                        repository = Graph.repository(appContext),
                        settingsStore = Graph.settings(appContext),
                        effects = AndroidPlatformEffects(appContext),
                    )
                }
            }
        }
    }
}
