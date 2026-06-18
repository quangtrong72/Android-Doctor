package com.uilover.project1983.Activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Model để chứa dữ liệu Lịch khám trả về từ Firestore
data class AppointmentModel(
    val id: String = "",
    val doctorName: String = "",
    val specialty: String = "",
    val serviceName: String = "",
    val date: String = "",
    val time: String = "",
    val patientName: String = "",
    val totalAmount: Int = 0,
    val status: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketHistoryScreen(onBackClick: () -> Unit = {}) {
    val BluePrimary = Color(0xFF1E88E5)
    val BgGray = Color(0xFFF8F9FA)

    var appointments by remember { mutableStateOf<List<AppointmentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Biến trạng thái để lưu phiếu khám đang được chọn để xem chi tiết
    var selectedAppointment by remember { mutableStateOf<AppointmentModel?>(null) }

    // --- LOGIC PHÂN TRANG (TABS) ---
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tất cả", "Đợi xác nhận", "Lịch khám", "Đã khám", "Đã hủy")

    // Gọi dữ liệu từ Cloud Firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Appointments")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<AppointmentModel>()
                    for (doc in querySnapshot.documents) {
                        list.add(
                            AppointmentModel(
                                id = doc.id,
                                doctorName = doc.getString("doctorName") ?: "",
                                specialty = doc.getString("specialty") ?: "",
                                serviceName = doc.getString("serviceName") ?: "",
                                date = doc.getString("date") ?: "",
                                time = doc.getString("time") ?: "",
                                patientName = doc.getString("patientName") ?: "",
                                totalAmount = doc.getLong("totalAmount")?.toInt() ?: 0,
                                status = doc.getString("status") ?: "Chưa xác định"
                            )
                        )
                    }
                    appointments = list.reversed()
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // --- LỌC DỮ LIỆU ---
    val filteredAppointments = when (selectedTabIndex) {
        0 -> appointments
        1 -> appointments.filter { it.status == "Đã thanh toán" }
        2 -> appointments.filter { it.status == "Đã xác nhận" }
        3 -> appointments.filter { it.status == "Đã khám" }
        4 -> appointments.filter { it.status == "Đã hủy" }
        else -> appointments
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lịch sử Khám bệnh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(paddingValues)
        ) {
            // --- THANH TABS CUỘN NGANG ---
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = BluePrimary,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = BluePrimary,
                            height = 3.dp
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) BluePrimary else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // --- KHU VỰC HIỂN THỊ DANH SÁCH ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(color = BluePrimary, modifier = Modifier.align(Alignment.Center))
                } else if (filteredAppointments.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "Bạn chưa có phiếu khám nào." else "Không có phiếu khám nào trong mục '${tabs[selectedTabIndex]}'.",
                            color = Color.Gray, fontSize = 15.sp, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredAppointments) { appointment ->
                            // Truyền sự kiện bấm vào thẻ để mở Dialog
                            TicketHistoryItem(appointment, onClick = {
                                selectedAppointment = appointment
                            })
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // HIỂN THỊ DIALOG NẾU CÓ PHIẾU KHÁM ĐƯỢC CHỌN
    if (selectedAppointment != null) {
        TicketDetailDialog(
            appointment = selectedAppointment!!,
            onDismiss = { selectedAppointment = null }
        )
    }
}

@Composable
fun TicketHistoryItem(appointment: AppointmentModel, onClick: () -> Unit) {
    val BluePrimary = Color(0xFF1E88E5)

    val (displayStatus, badgeColor) = when (appointment.status) {
        "Đã thanh toán" -> "Đợi xác nhận" to Color(0xFFFF9800)
        "Đã xác nhận" -> "Đã xác nhận" to Color(0xFF4CAF50)
        "Đã khám" -> "Đã khám" to Color(0xFF2196F3)
        "Đã hủy" -> "Đã hủy" to Color(0xFFF44336)
        else -> appointment.status to Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Mã phiếu & Cụm Trạng thái song song
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mã: ${appointment.id.take(8).uppercase()}",
                    fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (appointment.status == "Đã thanh toán") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Đã thanh toán", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(displayStatus, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

            // Body: Thông tin Bác sĩ & Bệnh nhân
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = BluePrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = appointment.doctorName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                    Text(text = appointment.specialty, color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Bệnh nhân: ${appointment.patientName.split(" - ")[0]}", fontSize = 14.sp, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${appointment.time} - ${appointment.date}", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

            // Footer: Dịch vụ & Giá tiền
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = appointment.serviceName, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "%,d đ".format(appointment.totalAmount), fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 16.sp)
            }
        }
    }
}

// Hộp thoại hiển thị chi tiết phiếu khám
@Composable
fun TicketDetailDialog(appointment: AppointmentModel, onDismiss: () -> Unit) {
    val BluePrimary = Color(0xFF1E88E5)

    val displayStatus = when (appointment.status) {
        "Đã thanh toán" -> "Đợi xác nhận"
        else -> appointment.status
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nút đóng góc trên
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.Gray)
                    }
                }

                Text("CHI TIẾT PHIẾU KHÁM", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(16.dp))

                // Mã vạch giả lập cho chuyên nghiệp
                Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                    Text("|||| | |||||| || | ||||", fontSize = 28.sp, letterSpacing = 4.sp, color = Color.Black)
                }
                Text("Mã: ${appointment.id.uppercase()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

                Spacer(modifier = Modifier.height(16.dp))

                // Trạng thái
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BluePrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Trạng thái: $displayStatus", color = BluePrimary, fontWeight = FontWeight.Bold)
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

                // 🔴 BẢNG THÔNG TIN CHI TIẾT (Đã đổi tên hàm thành TicketInfoRow)
                TicketInfoRow("Bác sĩ:", appointment.doctorName)
                TicketInfoRow("Chuyên khoa:", appointment.specialty)
                TicketInfoRow("Bệnh nhân:", appointment.patientName.split(" - ")[0])
                TicketInfoRow("Dịch vụ:", appointment.serviceName)
                TicketInfoRow("Ngày khám:", appointment.date)
                TicketInfoRow("Giờ khám:", appointment.time)

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng thanh toán:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("%,d đ".format(appointment.totalAmount), fontWeight = FontWeight.ExtraBold, color = BluePrimary, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 🔴 ĐÃ ĐỔI TÊN HÀM: Tránh trùng lặp với file khác trong project
@Composable
fun TicketInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
}