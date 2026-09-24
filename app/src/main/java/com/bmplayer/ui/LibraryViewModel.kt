package com.bmplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmplayer.data.repository.MusicRepository
import com.bmplayer.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repository: MusicRepository) : ViewModel() {
    val tracks: StateFlow<List<Track>> = repository.tracks

    fun refresh() { viewModelScope.launch { repository.refresh() } }
}
