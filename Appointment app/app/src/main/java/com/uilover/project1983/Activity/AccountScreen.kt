package com.uilover.project1983.Activity

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val BluePrimary = Color(0xFF1E88E5)
    val BgGray = Color(0xFFF8F9FA)

    // Lấy thông tin user hiện tại (SĐT hoặc Email)
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userDisplayInfo = currentUser?.phoneNumber?.takeIf { it.isNotEmpty() }
        ?: currentUser?.email
        ?: "Người dùng ẩn danh"

    // ĐÃ FIX LỖI Ở ĐÂY: Thêm : () -> Unit để xác định rõ kiểu dữ liệu
    val onLogoutClick: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(context, LoginActivity::class.java) // Chuyển về trang Đăng nhập
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Xóa ngăn xếp activity
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Gọi điện thoại tổng đài */ },
                containerColor = Color(0xFFFF9800), // Màu cam giống thiết kế
                shape = CircleShape
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Gọi hỗ trợ", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(paddingValues)
        ) {
            // --- 1. HEADER (Thông tin cá nhân) ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(BluePrimary)
                        .padding(top = 40.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.LightGray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // SĐT / Tên hiển thị
                        Text(
                            text = userDisplayInfo,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nút Đăng xuất nhỏ
                        OutlinedButton(
                            onClick = onLogoutClick,
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Đăng xuất", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // --- 2. DANH SÁCH MENU ---
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Điều khoản và quy định",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    AccountMenuItem(icon = Icons.Default.GppGood, iconBg = Color(0xFFE0F7FA), iconTint = Color(0xFF00BCD4), title = "Quy định sử dụng")
                    AccountMenuItem(icon = Icons.Default.Lock, iconBg = Color(0xFFF3E5F5), iconTint = Color(0xFF9C27B0), title = "Chính sách bảo mật")
                    AccountMenuItem(icon = Icons.Default.Handshake, iconBg = Color(0xFFFBE9E7), iconTint = Color(0xFFFF5722), title = "Điều khoản dịch vụ")

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 8.dp)

                    AccountMenuItem(icon = Icons.Default.MedicalInformation, iconBg = Color(0xFFE3F2FD), iconTint = BluePrimary, title = "Xem/Lưu thông tin sức khỏe")
                    AccountMenuItem(icon = Icons.Default.PhoneInTalk, iconBg = Color(0xFFE0F2F1), iconTint = Color(0xFF009688), title = "Hỗ trợ tư vấn/đặt khám 19002115")
                    AccountMenuItem(icon = Icons.Default.ThumbUp, iconBg = Color(0xFFFFF8E1), iconTint = Color(0xFFFFC107), title = "Đánh giá ứng dụng")
                    AccountMenuItem(icon = Icons.Default.Group, iconBg = Color(0xFFF1F8E9), iconTint = Color(0xFF8BC34A), title = "Tham gia cộng đồng dự án")
                    AccountMenuItem(icon = Icons.Default.Share, iconBg = Color(0xFFF3E5F5), iconTint = Color(0xFF9C27B0), title = "Chia sẻ ứng dụng")
                    AccountMenuItem(icon = Icons.Default.HelpOutline, iconBg = Color(0xFFECEFF1), iconTint = Color(0xFF607D8B), title = "Một số câu hỏi thường gặp")

                    // Mục chọn Ngôn ngữ có text phụ
                    AccountMenuItem(
                        icon = Icons.Default.Language,
                        iconBg = Color(0xFFE0F7FA),
                        iconTint = Color(0xFF00BCD4),
                        title = "Ngôn ngữ",
                        trailingText = "Tiếng Việt"
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 8.dp)

                    // Nút Đăng xuất ở cuối danh sách (Màu Đỏ)
                    AccountMenuItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        iconBg = Color(0xFFFFEBEE),
                        iconTint = Color(0xFFF44336),
                        title = "Đăng xuất",
                        isRedText = true,
                        onClick = onLogoutClick
                    )
                }
            }

            // --- 3. FOOTER (Thông tin công ty / ứng dụng) ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE))
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Dự án App Đặt Lịch Khám - VKU",
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Phát triển bởi: Nguyễn Ngọc Sơn & Trần Lê Quang Trọng",
                            color = BluePrimary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Lớp: 24SE2 - MSSV: 24IT232",
                            color = BluePrimary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Email: contact@vku.udn.vn\nHotline: 1900 2115",
                            color = BluePrimary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Phiên bản 1.0.0",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                // Chừa khoảng trống cho Bottom Navigation bar
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// Hàm giao diện tái sử dụng cho từng dòng Menu
@Composable
fun AccountMenuItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    trailingText: String? = null,
    isRedText: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon với nền bo tròn
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tên mục
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isRedText) Color(0xFFF44336) else Color.Black,
                modifier = Modifier.weight(1f)
            )

            // Text phụ ở cuối (vd: Tiếng Việt)
            if (trailingText != null) {
                Box(modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = trailingText, color = Color.DarkGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Mũi tên điều hướng
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(start = 68.dp))
    }
}