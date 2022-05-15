package com.dicoding.storyapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.storyapp.databinding.ActivityStoryBinding

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class StoryActivity: AppCompatActivity() {

    private lateinit var sharedViewModel: SharedViewModel
    private val storyViewModel: StoryViewModel by viewModels {
        StoryViewModel.ViewModelFactory(this)
    }
    private lateinit var binding: ActivityStoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        val layoutManager = LinearLayoutManager(this)
        binding.rvStories.layoutManager = layoutManager

        getStories()
    }

    private fun setupViewModel() {
        sharedViewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[SharedViewModel::class.java]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_map -> {
                val intent = Intent(this, StoryMapsActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_add -> {
                val intent = Intent(this, AddStoryActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_language -> {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
            }

            R.id.menu_logout -> {
                sharedViewModel.logout()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    private fun getStories() {
        val adapter = StoryAdapter()
        binding.rvStories.adapter = adapter.withLoadStateFooter(
            footer = LoadingStateAdapter {
                adapter.retry()
            }
        )
        sharedViewModel.getUser().observe(this) { userAuth ->
            if (userAuth != null) {
                storyViewModel.stories("Bearer " + userAuth.token).observe(this) { stories ->
                    adapter.submitData(lifecycle, stories)
                }
            }
        }
    }
}