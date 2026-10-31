package com.example.ui.importer

import com.example.model.DiscoveredMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ImportHolder {
    private val _discoveredItems = MutableStateFlow<List<DiscoveredMedia>>(emptyList())
    val discoveredItems: StateFlow<List<DiscoveredMedia>> = _discoveredItems.asStateFlow()

    fun setDiscoveredItems(items: List<DiscoveredMedia>) {
        _discoveredItems.value = items
    }

    fun toggleItem(url: String) {
        _discoveredItems.value = _discoveredItems.value.map {
            if (it.url == url) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAll(select: Boolean) {
        _discoveredItems.value = _discoveredItems.value.map {
            if (!it.isAlreadyInLibrary) it.copy(isSelected = select) else it
        }
    }

    fun clear() {
        _discoveredItems.value = emptyList()
    }
}
