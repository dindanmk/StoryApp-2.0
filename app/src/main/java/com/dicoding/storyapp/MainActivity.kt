package com.dicoding.storyapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.dicoding.storyapp.api.ApiConfig
import com.dicoding.storyapp.api.LoginResponse
import com.dicoding.storyapp.databinding.ActivityMainBinding
import com.dicoding.storyapp.model.UserAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: SharedViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        binding.btnLogin.setOnClickListener {
            val inputEmail = binding.etEmailLogin.text.toString()
            val inputPassword = binding.etPasswordLogin.text.toString()

            login(inputEmail, inputPassword)
        }

        binding.btnRegister.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupViewModel() {
        mainViewModel = ViewModelProvider(this, ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[SharedViewModel::class.java]

        mainViewModel.getUser().observe(this) {user ->
            if (user.isLogin) {
                val intent = Intent(this, StoryActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)

        val addMenu = menu.findItem(R.id.menu_add)
        val logoutMenu = menu.findItem(R.id.menu_logout)
        val mapMenu = menu.findItem(R.id.menu_map)

        addMenu.isVisible = false
        logoutMenu.isVisible = false
        mapMenu.isVisible = false

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_language) {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
        }
        return true
    }

    private fun login(inputEmail: String, inputPassword: String) {
        showLoading(true)

        val client = ApiConfig.getApiService().login(inputEmail, inputPassword)
        client.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                showLoading(false)
                val responseBody = response.body()
                if (response.isSuccessful && responseBody?.message == "success") {
                    mainViewModel.saveUser(UserAuth(responseBody.loginResult.token, true))
                    Toast.makeText(this@MainActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@MainActivity, StoryActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.login_fail), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(R.string.login_fail), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}