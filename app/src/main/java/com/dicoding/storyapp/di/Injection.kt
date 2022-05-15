package com.dicoding.storyapp.di

import android.content.Context
import com.dicoding.storyapp.api.ApiConfig
import com.dicoding.storyapp.db.StoriesDatabase
import com.dicoding.storyapp.repository.StoriesRepository

object Injection {
    fun provideRepository(context: Context): StoriesRepository {
        val database = StoriesDatabase.getDatabase(context)
        val apiService = ApiConfig.getApiService()

        return StoriesRepository(database, apiService)
    }
}