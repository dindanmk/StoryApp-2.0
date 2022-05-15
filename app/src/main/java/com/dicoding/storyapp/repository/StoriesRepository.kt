package com.dicoding.storyapp.repository

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.dicoding.storyapp.api.ApiService
import com.dicoding.storyapp.api.ListStoryItem
import com.dicoding.storyapp.db.StoriesDatabase

class StoriesRepository(private val storiesDatabase: StoriesDatabase, private val apiService: ApiService) {
    fun getStoriesForPaging(header: String) : LiveData<PagingData<ListStoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5,
            ),
            pagingSourceFactory = {
                StoriesPagingSource(apiService, header)
            }
        ).liveData
    }
}