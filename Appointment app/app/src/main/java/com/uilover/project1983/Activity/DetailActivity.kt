package com.uilover.project1983.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.uilover.project1983.Domain.DoctorsModel

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nhận dữ liệu Bác sĩ từ màn hình trước (MainActivity) truyền sang
        val doctorModel = intent.getSerializableExtra("object") as DoctorsModel

        setContent {
            DetailScreen(
                doctor = doctorModel,
                onBackClick = { finish() },
                onCallClick = {
                    // Tạm thời để trống hoặc thêm logic gọi điện sau
                },
                onDirectionClick = {
                    // Tạm thời để trống hoặc thêm logic mở Google Maps sau
                },
                onWebsiteClick = {
                    // Tạm thời để trống hoặc thêm logic mở trình duyệt sau
                }
            )
        }
    }
}