package com.example.movielist.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.movielist.data.local.MovieAppPreferences
import com.example.movielist.data.local.database.AppDatabase
import com.example.movielist.data.repository.MovieRepositoryImpl
import com.example.movielist.data.remote.api.RetrofitClient
import com.example.movielist.databinding.ActivityMainBinding
import com.example.movielist.domain.usecase.GetPopularMoviesUseCase
import com.example.movielist.presentation.ui.adapter.MovieAdapter
import com.example.movielist.presentation.viewmodel.MovieViewModel
import com.example.movielist.presentation.viewmodel.ViewModelFactory
import com.example.movielist.utils.Result
import com.example.movielist.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var movieAppPreferences: MovieAppPreferences

    private val movieViewModel: MovieViewModel by viewModels {
        val apiService = RetrofitClient.tmdbApiService
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
        val movieDao = database.movieDao()
        val tmdbApiKey = "71819bfeac768c2a5b9a32b26e50cae1"
        movieAppPreferences.saveApiKey(tmdbApiKey)

        val movieRepositoryImpl = MovieRepositoryImpl(apiService, movieDao, tmdbApiKey)
        val getPopularMoviesUseCase = GetPopularMoviesUseCase(movieRepositoryImpl)
        ViewModelFactory(getPopularMoviesUseCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "Popular Movies"

        movieAppPreferences = MovieAppPreferences(this)

        setupRecyclerView()
        observeViewModel()
        setupDarkModeToggle()

        binding.btnRetry.setOnClickListener {
            movieViewModel.fetchPopularMovies()
        }
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter()
        binding.rvMovies.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = movieAdapter
        }

        movieAdapter.onItemClick = { movie ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_MOVIE, movie)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        movieViewModel.popularMovies.observe(this, Observer { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                    binding.rvMovies.visibility = View.GONE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                    binding.rvMovies.visibility = View.VISIBLE
                    movieAdapter.submitList(result.data)
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvMovies.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.VISIBLE
                    binding.tvError.text = "Error: ${result.exception.message}"
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun setupDarkModeToggle() {
        val isDarkMode = movieAppPreferences.getDarkModeState()
        applyTheme(isDarkMode)
        updateToggleIcon(isDarkMode)

        binding.btnDarkModeToggle.setOnClickListener
            val currentMode = movieAppPreferences.getDarkModeState()
            val newMode = !currentMode
            movieAppPreferences.saveDarkModeState(newMode)
            applyTheme(newMode)
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun updateToggleIcon(isDarkMode: Boolean) {
        if (isDarkMode) {
            binding.btnDarkModeToggle.setImageResource(R.drawable.ic_light_bulb_off)
        } else {
            binding.btnDarkModeToggle.setImageResource(R.drawable.ic_light_bulb_on)
        }
    }
}