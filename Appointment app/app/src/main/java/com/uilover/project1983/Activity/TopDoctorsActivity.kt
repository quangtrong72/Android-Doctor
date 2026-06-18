package com.uilover.project1983.Activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project1983.Adapter.TopDoctorAdapter2
import com.uilover.project1983.ViewModel.MainViewModel
import com.uilover.project1983.databinding.ActivityTopDoctorsBinding

class TopDoctorsActivity : BaseActivity() {
    private lateinit var binding: ActivityTopDoctorsBinding
    private val viewModel = MainViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTopDoctors()
    }

    private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility = View.VISIBLE
            viewModel.doctors.observe(this@TopDoctorsActivity, Observer {
                viewTopDoctorList.layoutManager =
                    LinearLayoutManager(this@TopDoctorsActivity, LinearLayoutManager.VERTICAL, false)
                viewTopDoctorList.adapter = TopDoctorAdapter2(it)
                progressBarTopDoctor.visibility = View.GONE
            })
            viewModel.loadDoctors()

            backBtn.setOnClickListener { finish() }
        }
    }
}