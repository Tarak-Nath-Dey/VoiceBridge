package com.voicebridge.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicebridge.data.local.entity.UserEntity
import com.voicebridge.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isProfileCreated = MutableStateFlow<Boolean?>(null)
    val isProfileCreated: StateFlow<Boolean?> = _isProfileCreated.asStateFlow()

    val userFlow = userRepository.getUserFlow()

    init {
        checkProfile()
    }

    private fun checkProfile() {
        viewModelScope.launch {
            _isProfileCreated.value = userRepository.isProfileCreated()
        }
    }

    fun createProfile(username: String, avatarIndex: Int) {
        viewModelScope.launch {
            userRepository.createProfile(username, avatarIndex)
            _isProfileCreated.value = true
        }
    }

    fun updateProfile(username: String, avatarIndex: Int) {
        viewModelScope.launch {
            userRepository.updateProfile(username, avatarIndex)
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch {
            userRepository.updateStatus(status)
        }
    }
}
