package com.uilover.project1983.Doctor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
// Nhớ import đúng tên file LoginActivity của bạn nếu nó báo đỏ nhé
import com.uilover.project1983.Activity.LoginActivity

@Composable
fun DoctorAccountScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Tạm lấy Email từ Firebase Auth, sau này có thể load thêm Tên từ Firestore
    val currentUserEmail = auth.currentUser?.email ?: "bacsianhdanh@gmail.com"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB)) // Màu nền DoctorBg
    ) {
        // --- PHẦN HEADER THÔNG TIN ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)), // DoctorLightBlue
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(50.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tài Khoản Bác Sĩ",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentUserEmail,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- PHẦN MENU LỰA CHỌN ---
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = "Cài đặt chung", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))

            // Các menu chức năng (Hiện tại là giao diện, bạn có thể ghép logic sau)
            AccountMenuItem(icon = Icons.Default.PersonOutline, title = "Chỉnh sửa hồ sơ cá nhân") {}
            AccountMenuItem(icon = Icons.Outlined.Lock, title = "Đổi mật khẩu") {}
            AccountMenuItem(icon = Icons.Default.NotificationsNone, title = "Cài đặt thông báo") {}

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Khác", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))

            AccountMenuItem(icon = Icons.Default.HelpOutline, title = "Trung tâm hỗ trợ") {}

            // NÚT ĐĂNG XUẤT CÓ CHỨC NĂNG THẬT
            AccountMenuItem(icon = Icons.Default.Logout, title = "Đăng xuất", titleColor = Color.Red, iconColor = Color.Red) {
                // Xóa phiên đăng nhập Firebase
                auth.signOut()

                // Chuyển hướng về màn hình Đăng nhập và xóa lịch sử các trang trước đó
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}

// Component vẽ từng dòng Menu cho đẹp
@Composable
fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = Color.Black,
    iconColor: Color = Color(0xFF1E88E5), // DoctorBlue
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = titleColor, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}