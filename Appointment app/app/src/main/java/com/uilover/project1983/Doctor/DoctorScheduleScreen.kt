package com.uilover.project1983.Doctor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

// Model lưu trữ dữ liệu
data class DoctorAppointmentModel(
    val id: String = "",
    val userId: String = "",
    val patientName: String = "",
    val serviceName: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "",
    val totalAmount: Int = 0
)

data class DayModel(val dayOfWeek: String, val dateFull: String, val dayOfMonth: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorScheduleScreen() {
    val BluePrimary = Color(0xFF1E88E5)
    val BgGray = Color(0xFFF8F9FA)
    val context = LocalContext.current

    val upcomingDays = remember { getUpcomingDays(60) }
    var selectedDate by remember { mutableStateOf(upcomingDays.first().dateFull) }

    var allAppointments by remember { mutableStateOf<List<DoctorAppointmentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- LOGIC TABS CHO BÁC SĨ ---
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chờ duyệt", "Đã nhận", "Đã khám", "Đã hủy")

    // BỘ LẮNG NGHE TỰ ĐỘNG NHẬN DIỆN TÀI KHOẢN ĐANG ĐĂNG NHẬP
    val doctorUid = FirebaseAuth.getInstance().currentUser?.uid

    DisposableEffect(doctorUid) {
        val realtimeDb = FirebaseDatabase.getInstance()
        val firestoreDb = FirebaseFirestore.getInstance()
        var firestoreListener: ListenerRegistration? = null

        if (doctorUid != null) {
            isLoading = true
            // 🔴 ĐÃ FIX: Chuyển "Doctors" (Chữ D hoa) để khớp với Realtime DB
            realtimeDb.getReference("Doctors").child(doctorUid).get()
                .addOnSuccessListener { snapshot ->

                    // Lấy ra giá trị Id và ép về dạng Chuỗi cho an toàn tuyệt đối
                    val realDoctorId = snapshot.child("Id").value?.toString()?.trim()

                    if (!realDoctorId.isNullOrEmpty()) {
                        // Tìm thấy bác sĩ, bắt đầu lấy lịch từ Firestore
                        firestoreListener = firestoreDb.collection("Appointments")
                            .addSnapshotListener { querySnapshot, error ->
                                if (error != null) {
                                    isLoading = false
                                    Toast.makeText(context, "Lỗi tải lịch từ Firestore!", Toast.LENGTH_SHORT).show()
                                    return@addSnapshotListener
                                }

                                if (querySnapshot != null) {
                                    val list = mutableListOf<DoctorAppointmentModel>()
                                    for (doc in querySnapshot.documents) {
                                        // Ép doctorId trên Firestore về Chuỗi
                                        val doctorIdOnDb = doc.get("doctorId")?.toString()?.trim() ?: ""

                                        // So sánh 2 Chuỗi với nhau
                                        if (doctorIdOnDb == realDoctorId) {
                                            list.add(
                                                DoctorAppointmentModel(
                                                    id = doc.id,
                                                    userId = doc.getString("userId") ?: "",
                                                    patientName = doc.getString("patientName") ?: "",
                                                    serviceName = doc.getString("serviceName") ?: "",
                                                    date = doc.getString("date") ?: "",
                                                    time = doc.getString("time") ?: "",
                                                    status = doc.getString("status") ?: "Chưa xác định",
                                                    totalAmount = doc.getLong("totalAmount")?.toInt() ?: 0
                                                )
                                            )
                                        }
                                    }
                                    allAppointments = list
                                    isLoading = false
                                }
                            }
                    } else {
                        isLoading = false
                        Toast.makeText(context, "Tài khoản chưa được cấu hình 'Id' trong bảng Doctors!", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Lỗi đọc Realtime DB", Toast.LENGTH_SHORT).show()
                }
        } else {
            isLoading = false
        }

        onDispose { firestoreListener?.remove() }
    }

    // --- BỘ LỌC KÉP: Lọc theo Ngày TRƯỚC, lọc theo Tab SAU ---
    val appointmentsForSelectedDate = allAppointments.filter { it.date == selectedDate }.sortedBy { it.time }

    val filteredAppointments = when (selectedTabIndex) {
        // 🔴 ĐÃ FIX: Tab "Chờ duyệt" hiện cả phiếu thanh toán online lẫn phiếu trả tiền mặt
        0 -> appointmentsForSelectedDate.filter { it.status == "Đã thanh toán" || it.status == "Chưa thanh toán" }
        1 -> appointmentsForSelectedDate.filter { it.status == "Đã xác nhận" }
        2 -> appointmentsForSelectedDate.filter { it.status == "Đã khám" }
        3 -> appointmentsForSelectedDate.filter { it.status == "Đã hủy" }
        else -> appointmentsForSelectedDate
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lịch làm việc", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BluePrimary)
            )
        },
        containerColor = BgGray
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // --- 1. THANH CUỘN CHỌN NGÀY ---
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(upcomingDays.size) { index ->
                        val dayInfo = upcomingDays[index]
                        val isSelected = selectedDate == dayInfo.dateFull
                        DateCard(
                            dayOfWeek = dayInfo.dayOfWeek,
                            dayOfMonth = dayInfo.dayOfMonth,
                            isSelected = isSelected,
                            onClick = { selectedDate = dayInfo.dateFull }
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

            // --- 2. THANH TAB PHÂN LOẠI TRẠNG THÁI LỊCH ---
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

            // --- 3. THỐNG KÊ NHANH ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tổng cộng (${filteredAppointments.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(text = selectedDate, color = BluePrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }

            // --- 4. DANH SÁCH BỆNH NHÂN TRONG NGÀY ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(color = BluePrimary, modifier = Modifier.align(Alignment.Center))
                } else if (filteredAppointments.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assignment, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Không có lịch khám nào trong mục này.", color = Color.Gray, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAppointments.size) { index ->
                            val appointment = filteredAppointments[index]
                            ScheduleItemCard(
                                appointment = appointment,
                                onUpdateStatus = { newStatus ->
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("Appointments").document(appointment.id)
                                        .update("status", newStatus)
                                        .addOnSuccessListener {
                                            val notifType = if (newStatus == "Đã hủy") "cancel" else "success"

                                            // 🔴 CẬP NHẬT THÔNG BÁO CHO TRƯỜNG HỢP THU TIỀN MẶT
                                            val notifTitle = when (newStatus) {
                                                "Đã hủy" -> "Lịch khám bị hủy"
                                                "Đã thanh toán" -> "Đã nhận thanh toán"
                                                "Đã xác nhận" -> "Lịch khám đã được xác nhận"
                                                "Đã khám" -> "Hoàn thành ca khám"
                                                else -> "Cập nhật lịch khám"
                                            }
                                            val notifMessage = when (newStatus) {
                                                "Đã hủy" -> "Rất tiếc, bác sĩ không thể tiếp nhận ca khám ${appointment.serviceName} ngày ${appointment.date}."
                                                "Đã thanh toán" -> "Thanh toán cho ca khám ${appointment.serviceName} ngày ${appointment.date} đã được xác nhận. Vui lòng chờ bác sĩ duyệt lịch!"
                                                "Đã xác nhận" -> "Lịch khám ${appointment.serviceName} lúc ${appointment.time} ngày ${appointment.date} đã được bác sĩ chốt. Vui lòng đến đúng giờ!"
                                                "Đã khám" -> "Ca khám ${appointment.serviceName} ngày ${appointment.date} đã hoàn thành. Chúc bạn nhiều sức khỏe!"
                                                else -> ""
                                            }

                                            val notifData = hashMapOf(
                                                "userId" to appointment.userId,
                                                "title" to notifTitle,
                                                "message" to notifMessage,
                                                "type" to notifType,
                                                "time" to "Vừa xong",
                                                "isRead" to false
                                            )
                                            db.collection("Notifications").add(notifData)
                                            Toast.makeText(context, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// --- CÁC THÀNH PHẦN GIAO DIỆN (UI Components) ---

@Composable
fun DateCard(dayOfWeek: String, dayOfMonth: String, isSelected: Boolean, onClick: () -> Unit) {
    val BluePrimary = Color(0xFF1E88E5)
    val bgColor = if (isSelected) BluePrimary else Color(0xFFF5F5F5)
    val textColor = if (isSelected) Color.White else Color.Gray
    val textBoldColor = if (isSelected) Color.White else Color.Black

    Column(
        modifier = Modifier.width(60.dp).height(75.dp).clip(RoundedCornerShape(16.dp)).background(bgColor).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = dayOfWeek, color = textColor, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = dayOfMonth, color = textBoldColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun ScheduleItemCard(appointment: DoctorAppointmentModel, onUpdateStatus: (String) -> Unit) {
    val BluePrimary = Color(0xFF1E88E5)

    // 🔴 ĐÃ BỔ SUNG TRẠNG THÁI "CHƯA THANH TOÁN" VỚI MÀU CAM CẢNH BÁO
    val (displayStatus, statusColor) = when (appointment.status) {
        "Chưa thanh toán" -> "Cần thu tiền" to Color(0xFFFF9800)
        "Đã thanh toán" -> "Chờ duyệt" to Color(0xFFE65100)
        "Đã xác nhận" -> "Đã nhận lịch" to Color(0xFF4CAF50)
        "Đã khám" -> "Đã hoàn thành" to Color(0xFF2196F3)
        "Đã hủy" -> "Đã từ chối" to Color(0xFFF44336)
        else -> appointment.status to Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
                    Text(text = appointment.time.split(" - ")[0], fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = BluePrimary)
                }
                Box(modifier = Modifier.padding(horizontal = 12.dp).width(2.dp).height(40.dp).background(statusColor.copy(alpha = 0.5f)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = appointment.patientName.split(" - ")[0], fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = appointment.serviceName, color = Color.DarkGray, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = displayStatus, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 🔴 XỬ LÝ CÁC NÚT BẤM DỰA VÀO TỪNG TRẠNG THÁI
                when (appointment.status) {
                    "Chưa thanh toán" -> {
                        Button(
                            onClick = { onUpdateStatus("Đã thanh toán") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("Đã thu tiền", fontSize = 13.sp) }
                    }
                    "Đã thanh toán" -> {
                        Row {
                            OutlinedButton(
                                onClick = { onUpdateStatus("Đã hủy") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Red.copy(alpha = 0.5f))),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(36.dp)
                            ) { Text("Từ chối", fontSize = 13.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onUpdateStatus("Đã xác nhận") },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(36.dp)
                            ) { Text("Xác nhận", fontSize = 13.sp) }
                        }
                    }
                    "Đã xác nhận" -> {
                        Button(
                            onClick = { onUpdateStatus("Đã khám") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("Hoàn thành", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

// Logic tạo danh sách ngày
fun getUpcomingDays(daysCount: Int): List<DayModel> {
    val list = mutableListOf<DayModel>()
    val calendar = Calendar.getInstance()
    val fullDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dayOfMonthFormatter = SimpleDateFormat("dd", Locale.getDefault())
    val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale("vi", "VN"))

    for (i in 0 until daysCount) {
        list.add(
            DayModel(
                dayOfWeek = dayOfWeekFormatter.format(calendar.time).replaceFirstChar { it.uppercase() },
                dateFull = fullDateFormatter.format(calendar.time),
                dayOfMonth = dayOfMonthFormatter.format(calendar.time)
            )
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}