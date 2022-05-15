package com.dicoding.storyapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.dicoding.storyapp.api.ApiConfig
import com.dicoding.storyapp.api.ListStoryItem
import com.dicoding.storyapp.api.StoriesResponse
import com.dicoding.storyapp.databinding.ActivityStoryMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "setting")

class StoryMapsActivity: AppCompatActivity(), OnMapReadyCallback {

    private lateinit var storyMapsViewModel: SharedViewModel
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityStoryMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val _listStoriesLocation = MutableLiveData<List<ListStoryItem>>()
    private val listStoriesLocation: LiveData<List<ListStoryItem>> = _listStoriesLocation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStoryMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.app_name)

        setupViewModel()
        getStoriesLocation()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                    getMyLocation()
                }
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> {
                    getMyLocation()
                }
                else -> {
                }
            }
        }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getMyLocation() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ){
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    mMap.isMyLocationEnabled = true
                    showMyLocationMarker(location)
                } else{
                    Toast.makeText(
                        this@StoryMapsActivity,
                        getString(R.string.location_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else{
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showMyLocationMarker(location: Location) {
        LAT = location.latitude
        LON = location.longitude

        val startLocation = LatLng(LAT, LON)
        mMap.addMarker(
            MarkerOptions()
                .position(startLocation)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .draggable(true)
                .title(getString(R.string.your_location))
        )
    }

    private fun getStoriesLocation() {
        storyMapsViewModel.getUser().observe(this) {
            if (it != null) {
                val client = ApiConfig.getApiService().getStoriesWithLocation("Bearer " + it.token, 1)
                client.enqueue(object : Callback<StoriesResponse> {
                    override fun onResponse(
                        call: Call<StoriesResponse>,
                        response: Response<StoriesResponse>
                    ) {
                        val responseBody = response.body()
                        if(response.isSuccessful && responseBody?.message == "Stories fetched successfully") {
                            _listStoriesLocation.value = responseBody.listStory
                        }
                    }

                    override fun onFailure(call: Call<StoriesResponse>, t: Throwable) {
                        Toast.makeText(this@StoryMapsActivity, getString(R.string.fail_load_stories), Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun setupViewModel() {
        storyMapsViewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreferences.getInstance(dataStore))
        )[SharedViewModel::class.java]
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)

        val mapsMenu = menu.findItem(R.id.menu_map)
        mapsMenu.isVisible = false

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
                intent.putExtra(AddStoryActivity.LAT, LAT.toFloat())
                intent.putExtra(AddStoryActivity.LON, LON.toFloat())
                startActivity(intent)
            }

            R.id.menu_language -> {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
            }

            R.id.menu_logout -> {
                storyMapsViewModel.logout()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isIndoorLevelPickerEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        setMapStyle()
        getMyLocation()

        val jakarta = LatLng(-6.23, 106.76)

        listStoriesLocation.observe(this) {
            for (i in listStoriesLocation.value?.indices!!) {
                val location = LatLng(listStoriesLocation.value?.get(i)?.lat!!, listStoriesLocation.value?.get(i)?.lon!!)
                mMap.addMarker(MarkerOptions().position(location).title(getString(R.string.story_uploaded_by) + listStoriesLocation.value?.get(i)?.name))
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 2f))
        }
    }

    private fun setMapStyle() {
        try {
            val success =
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (exception: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", exception)
        }
    }

    companion object {
        const val TAG = "StoryMapsActivity"
        var LAT = 0.0
        var LON = 0.0
    }
}