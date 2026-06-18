package com.uilover.project1983.Activity

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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

    // Gọi dữ liệu từ Cloud Firestore (Có Lắng nghe Realtime)
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()

            // Dùng addSnapshotListener để giao diện tự cập nhật khi bệnh nhân hủy lịch
            db.collection("Appointments")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (querySnapshot != null) {
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
                }
        } else {
            isLoading = false
        }
    }

    // --- LỌC DỮ LIỆU ---
    val filteredAppointments = when (selectedTabIndex) {
        0 -> appointments
        // Tab đợi xác nhận hiện cả phiếu chưa thanh toán và đã thanh toán chờ duyệt
        1 -> appointments.filter { it.status == "Đã thanh toán" || it.status == "Chưa thanh toán" }
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

    // Quy định màu sắc và text hiển thị
    val (displayStatus, badgeColor) = when (appointment.status) {
        "Chưa thanh toán" -> "Chờ thanh toán" to Color(0xFFFF9800)
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
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF4CAF50).copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 6.dp)
                        ) { Text("Đã thanh toán", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    }

                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(badgeColor.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(displayStatus, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = BluePrimary) }
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = appointment.serviceName, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "%,d đ".format(appointment.totalAmount), fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 16.sp)
            }
        }
    }
}

// 🔴 ĐÃ CẬP NHẬT: Thêm nút "Hủy lịch" nếu trạng thái chưa được bác sĩ xác nhận
@Composable
fun TicketDetailDialog(appointment: AppointmentModel, onDismiss: () -> Unit) {
    val BluePrimary = Color(0xFF1E88E5)
    val context = LocalContext.current

    val displayStatus = when (appointment.status) {
        "Chưa thanh toán" -> "Chờ thanh toán"
        "Đã thanh toán" -> "Đợi bác sĩ duyệt"
        else -> appointment.status
    }

    // Điều kiện cho phép hủy: Bác sĩ chưa khám, chưa xác nhận và chưa hủy
    val canCancel = appointment.status == "Chưa thanh toán" || appointment.status == "Đã thanh toán"

    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // Hộp thoại xác nhận hủy kép
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Xác nhận hủy lịch", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn hủy lịch khám này không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        val db = FirebaseFirestore.getInstance()
                        db.collection("Appointments").document(appointment.id)
                            .update("status", "Đã hủy")
                            .addOnSuccessListener {
                                Toast.makeText(context, "Hủy lịch thành công", Toast.LENGTH_SHORT).show()
                                showCancelConfirmDialog = false
                                onDismiss() // Đóng dialog chi tiết
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hủy Lịch") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirmDialog = false }) { Text("Đóng") }
            }
        )
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.Gray)
                    }
                }

                Text("CHI TIẾT PHIẾU KHÁM", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BluePrimary)
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                    Text("|||| | |||||| || | ||||", fontSize = 28.sp, letterSpacing = 4.sp, color = Color.Black)
                }
                Text("Mã: ${appointment.id.uppercase()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BluePrimary.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("Trạng thái: $displayStatus", color = BluePrimary, fontWeight = FontWeight.Bold) }

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

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

                // 🔴 KHU VỰC NÚT BẤM (Nếu được hủy thì hiện 2 nút)
                if (canCancel) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showCancelConfirmDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.5f)))
                        ) { Text("Hủy Lịch", fontWeight = FontWeight.Bold) }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Đóng", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Đóng", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

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