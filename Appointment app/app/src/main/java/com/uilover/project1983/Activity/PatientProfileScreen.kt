package com.uilover.project1983.Activity

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// 1. CẬP NHẬT MODEL: Thêm đầy đủ các trường khớp với lúc đăng ký Firebase
data class PatientModel(
    var id: String = "",
    val fullName: String = "",
    val dob: String = "",
    val gender: String = "",
    val phone: String = "",
    val cccd: String = "",
    val bhyt: String = "",
    val job: String = "",
    val email: String = "",
    val country: String = "",
    val ethnicity: String = "",
    val province: String = "",
    val district: String = "",
    val ward: String = "",
    val street: String = ""
)

@Composable
fun PatientProfileScreen(
    paddingValues: PaddingValues,
    onBackClick: () -> Unit,
    onRegisterNewClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser // Lấy thông tin user hiện tại

    var patients by remember { mutableStateOf<List<PatientModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 🔴 BIẾN QUẢN LÝ HỘP THOẠI ĐĂNG NHẬP (CHO KHÁCH)
    var showLoginDialog by remember { mutableStateOf(false) }

    // BIẾN QUẢN LÝ TRẠNG THÁI XEM CHI TIẾT
    // Nếu null -> Hiện danh sách. Nếu có dữ liệu -> Hiện chi tiết.
    var selectedPatient by remember { mutableStateOf<PatientModel?>(null) }

    // Gọi dữ liệu từ Firebase (ĐÃ BỔ SUNG PHÂN QUYỀN)
    LaunchedEffect(Unit) {
        val uid = currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("PatientProfiles")
                .whereEqualTo("userId", uid) // Lọc: Chỉ lấy hồ sơ của user hiện tại
                .get()
                .addOnSuccessListener { result ->
                    val list = result.map { document ->
                        val patient = document.toObject(PatientModel::class.java)
                        patient.id = document.id
                        patient
                    }
                    patients = list
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    Log.d("Firestore", "Lỗi lấy dữ liệu: ", exception)
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- KIỂM TRA TRẠNG THÁI ĐỂ ĐIỀU HƯỚNG MÀN HÌNH ---
        if (selectedPatient != null) {
            // MÀN HÌNH CHI TIẾT HỒ SƠ
            PatientDetailScreen(
                patient = selectedPatient!!,
                paddingValues = paddingValues,
                onBackClick = { selectedPatient = null } // Bấm quay lại thì gán null để về danh sách
            )
        } else {
            // MÀN HÌNH DANH SÁCH HỒ SƠ (GIAO DIỆN CHÍNH)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F9FC))
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onBackClick() })
                    Text("Hồ sơ bệnh nhân", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.size(28.dp))
                }

                // NỘI DUNG (LOADING / TRỐNG / DANH SÁCH)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isLoading) {
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.align(Alignment.Center))
                    } else if (patients.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.size(130.dp).background(LightBlueCard, shape = CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Assignment, null, tint = PrimaryBlue, modifier = Modifier.size(65.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Chưa có hồ sơ bệnh nhân nào", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Bạn được tạo tối đa 10 hồ sơ cá nhân\nvà người thân trong gia đình", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text("Danh sách hồ sơ (${patients.size}/10)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            items(patients) { patient ->
                                // Bắt sự kiện click để mở Chi tiết
                                PatientCard(patient, onClick = { selectedPatient = patient })
                            }
                        }
                    }
                }

                // NÚT ĐĂNG KÝ MỚI
                Button(
                    onClick = {
                        // 🔴 KIỂM TRA QUYỀN TRUY CẬP CỦA KHÁCH
                        if (currentUser == null) {
                            showLoginDialog = true
                        } else {
                            onRegisterNewClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .height(54.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chưa từng khám đăng ký mới", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 🔴 HIỂN THỊ HỘP THOẠI YÊU CẦU ĐĂNG NHẬP
        if (showLoginDialog) {
            AlertDialog(
                onDismissRequest = { showLoginDialog = false },
                title = { Text("Yêu cầu đăng nhập", fontWeight = FontWeight.Bold) },
                text = { Text("Tính năng này yêu cầu tài khoản. Bạn có muốn chuyển đến trang Đăng nhập để tiếp tục không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLoginDialog = false
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Đăng nhập")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showLoginDialog = false }) {
                        Text("Hủy", color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

// --- GIAO DIỆN MỚI: CHI TIẾT HỒ SƠ ---
@Composable
fun PatientDetailScreen(patient: PatientModel, paddingValues: PaddingValues, onBackClick: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        // HEADER XANH
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onBackClick() })
            Text("Chi tiết hồ sơ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.size(28.dp))
        }

        // NỘI DUNG CUỘN CHI TIẾT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Phần Avatar và Tên
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp).background(LightBlueCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(45.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = patient.fullName.uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue)
                Text(text = "ID: ${patient.id.take(8).uppercase()}", fontSize = 14.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Khối Thông tin chung
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("THÔNG TIN CHUNG", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(bottom = 12.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("Ngày sinh", patient.dob)
                    DetailRow("Giới tính", patient.gender)
                    DetailRow("CCCD", patient.cccd)
                    DetailRow("Bảo hiểm y tế", patient.bhyt.ifEmpty { "Không có" })
                    DetailRow("Nghề nghiệp", patient.job)
                    DetailRow("Số điện thoại", patient.phone)
                    DetailRow("Email", patient.email.ifEmpty { "Không có" })
                    DetailRow("Dân tộc", patient.ethnicity)
                    DetailRow("Quốc gia", patient.country)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Khối Địa chỉ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ĐỊA CHỈ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(bottom = 12.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("Tỉnh/Thành phố", patient.province)
                    DetailRow("Quận/Huyện", patient.district)
                    DetailRow("Phường/Xã", patient.ward)
                    DetailRow("Số nhà/Đường", patient.street)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Widget dòng chi tiết (Tái sử dụng)
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDarkBlue, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
    }
}

// --- THẺ HỒ SƠ DANH SÁCH (Đã thêm sự kiện Click) ---
@Composable
fun PatientCard(patient: PatientModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(LightBlueCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = patient.fullName.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Ngày sinh: ${patient.dob}", fontSize = 14.sp, color = TextSecondary)
                Text(text = "SĐT: ${patient.phone}", fontSize = 14.sp, color = TextSecondary)
            }

            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Chi tiết", tint = Color.LightGray)
        }
    }
}