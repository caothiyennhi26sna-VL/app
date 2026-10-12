package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.db.ThinkingDatabase
import com.example.data.db.ThinkingMessageEntity
import com.example.data.db.ThinkingSessionEntity
import com.example.data.repository.SocraticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThinkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SocraticRepository
    init {
        val database = ThinkingDatabase.getDatabase(application)
        repository = SocraticRepository(database.thinkingDao())
    }

    val allSessions: StateFlow<List<ThinkingSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSessionsCount: StateFlow<Int> = repository.totalSessionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val concludedSessionsCount: StateFlow<Int> = repository.concludedSessionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _currentSession = MutableStateFlow<ThinkingSessionEntity?>(null)
    val currentSession: StateFlow<ThinkingSessionEntity?> = _currentSession.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ThinkingMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<ThinkingMessageEntity>> = _currentMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedMode = MutableStateFlow("Co_ban")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _hasGeminiApiKey = MutableStateFlow(false)
    val hasGeminiApiKey: StateFlow<Boolean> = _hasGeminiApiKey.asStateFlow()

    init {
        checkApiKeyStatus()
    }

    fun checkApiKeyStatus() {
        val key = BuildConfig.GEMINI_API_KEY
        _hasGeminiApiKey.value = key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    fun setSelectedMode(mode: String) {
        _selectedMode.value = mode
    }

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val session = repository.getSessionById(sessionId)
            _currentSession.value = session
            repository.getMessagesForSession(sessionId).collect { messages ->
                _currentMessages.value = messages
                _isLoading.value = false
            }
        }
    }

    fun startNewSession(
        question: String,
        category: String = "Tổng quát",
        mode: String = _selectedMode.value,
        onSessionCreated: (Long) -> Unit
    ) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sessionId = repository.createNewSession(question.trim(), category, mode)
                val session = repository.getSessionById(sessionId)
                _currentSession.value = session
                onSessionCreated(sessionId)

                // Lắng nghe messages ngay
                launch {
                    repository.getMessagesForSession(sessionId).collect {
                        _currentMessages.value = it
                    }
                }

                // Kích hoạt bot phân tích và đưa ra gợi ý khởi đầu
                repository.processBotReply(sessionId)
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khởi tạo phiên tư duy: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendUserThought(sessionId: Long, thought: String) {
        if (thought.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addUserThought(sessionId, thought.trim())
                // Bot phản hồi nhận xét và dẫn dắt tiếp
                repository.processBotReply(sessionId)
                // Cập nhật lại session
                _currentSession.value = repository.getSessionById(sessionId)
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi phản hồi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestConclusion(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.processBotReply(sessionId, isConclusionRequest = true)
                _currentSession.value = repository.getSessionById(sessionId)
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi chốt vấn đề: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestMoreHint(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.processBotReply(sessionId, isMoreHintRequest = true)
                _currentSession.value = repository.getSessionById(sessionId)
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi gợi ý thêm: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSession(session: ThinkingSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSession.value?.id == session.id) {
                _currentSession.value = null
                _currentMessages.value = emptyList()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearCurrentSession() {
        _currentSession.value = null
        _currentMessages.value = emptyList()
    }
}
