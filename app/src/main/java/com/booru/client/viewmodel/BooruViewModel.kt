package com.booru.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.booru.client.data.model.PostModel
import com.booru.client.engine.GelbooruScraper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that bridges the Gelbooru scraper with the UI.
 * Manages pagination, loading states, and search queries.
 */
class BooruViewModel(application: Application) : AndroidViewModel(application) {

    private val scraper = GelbooruScraper(application)

    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts: StateFlow<List<PostModel>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Load initial content on startup
        search("")
    }

    /**
     * Perform a new search with the given tags.
     * Resets pagination and clears existing posts.
     */
    fun search(tags: String) {
        _searchQuery.value = tags
        _currentPage.value = 0
        _posts.value = emptyList()
        _error.value = null
        loadPage(tags, 0)
    }

    /**
     * Load the next page of results for the current search query.
     */
    fun loadNextPage() {
        if (_isLoadingMore.value || _isLoading.value) return
        val nextPage = _currentPage.value + 1
        _currentPage.value = nextPage
        loadPage(_searchQuery.value, nextPage)
    }

    /**
     * Refresh current search from page 0.
     */
    fun refresh() {
        search(_searchQuery.value)
    }

    /**
     * Clear all results and search state.
     */
    fun clear() {
        _posts.value = emptyList()
        _searchQuery.value = ""
        _currentPage.value = 0
        _error.value = null
    }

    private fun loadPage(tags: String, page: Int) {
        viewModelScope.launch {
            try {
                if (page == 0) {
                    _isLoading.value = true
                } else {
                    _isLoadingMore.value = true
                }

                val newPosts = scraper.scrapePosts(tags, page)

                if (newPosts.isEmpty() && page == 0) {
                    _error.value = "No posts found. Try different tags."
                }

                // Append or replace posts
                _posts.value = if (page == 0) {
                    newPosts
                } else {
                    _posts.value + newPosts
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Scrape failed: ${e.message}"
                // On error, revert page counter
                if (page > 0) {
                    _currentPage.value = page - 1
                }
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
            }
        }
    }
}
