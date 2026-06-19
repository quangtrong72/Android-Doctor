package com.uilover.project1983.Activity

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.uilover.project1983.Domain.DoctorsModel
import java.text.SimpleDateFormat
import java.util.*

// ====================================================================
// MODEL DỮ LIỆU ĐÁNH GIÁ
// ====================================================================
data class ReviewModel(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientAvatar: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    doctor: DoctorsModel,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onDirectionClick: () -> Unit,
    onWebsiteClick: () -> Unit
) {
    val context = LocalContext.current
    var expandedSpecialty by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val specialties = listOf("Tim mạch", "Huyết áp", "Tiểu đường", "Mỡ máu", "Nội tiết", "Tiêu hóa")

    // 🔴 LOGIC: LẮNG NGHE TRẠNG THÁI "NGHỈ NGƠI" CỦA BÁC SĨ TỪ REALTIME DB
    var doctorRealtimeStatus by remember { mutableStateOf(doctor.status) }

    DisposableEffect(doctor.docUid) {
        val ref = FirebaseDatabase.getInstance().getReference("Doctors").child(doctor.docUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                if (status != null) {
                    doctorRealtimeStatus = status
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            AsyncImage(model = doctor.Picture, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.padding(24.dp).clickable { onBackClick() })

            // Hiện cảnh báo trên Avatar nếu bác sĩ đang bận
            if (doctorRealtimeStatus == "offline") {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Text("BÁC SĨ ĐANG BẬN", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 2.sp)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(doctor.Name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(doctor.Special, fontSize = 16.sp, color = Color.Gray)

            Text("Chuyên trị:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp), color = Color(0xFF1E88E5))
            FlowRow(modifier = Modifier.animateContentSize().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val displayList = if (expandedSpecialty) specialties else specialties.take(3)
                displayList.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
                Text(if (expandedSpecialty) "Thu gọn" else "Xem thêm", color = Color(0xFF1E88E5),
                    modifier = Modifier.clickable { expandedSpecialty = !expandedSpecialty }.padding(8.dp))
            }

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Lịch khám: ${doctor.Schedule}", fontSize = 14.sp, color = Color.DarkGray)
                    Text("Giá khám: ${doctor.Price}", fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color(0xFF1E88E5)) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Thông tin chung") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Đánh giá") })
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DoctorInfoRow("Giới thiệu", doctor.Biography)
                    if (doctor.Experience > 0) {
                        DoctorInfoRow("Kinh nghiệm", "${doctor.Experience} năm")
                    }
                    DoctorInfoRow("Lĩnh vực lâm sàng", doctor.ClinicalFields)
                    DoctorInfoRow("Quá trình đào tạo", doctor.Education)
                    DoctorInfoRow("Quá trình công tác", doctor.WorkHistory)
                    DoctorInfoRow("Chứng chỉ & Hội nghị", doctor.Certificates)
                    DoctorInfoRow("Bài báo khoa học", doctor.Publications)
                }
            } else {
                DoctorReviewsSection(doctor = doctor)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconBtn("Tư vấn", Icons.Default.ChatBubble, onClick = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("NAVIGATE_TO_TAB", 3)
                        putExtra("DOCTOR_NAME", doctor.Name)
                        putExtra("DOCTOR_ID", doctor.docUid)
                    }
                    context.startActivity(intent)
                })

                IconBtn("Gọi điện", Icons.Default.Call, onCallClick)
                IconBtn("Đường đi", Icons.Default.LocationOn, onDirectionClick)
                IconBtn("Website", Icons.Outlined.Language, onWebsiteClick)
            }

            // 🔴 LOGIC: KHÓA NÚT ĐẶT LỊCH NẾU BÁC SĨ OFFLINE
            if (doctorRealtimeStatus == "offline") {
                Button(
                    onClick = { Toast.makeText(context, "Bác sĩ có việc bận và không nhận lịch hôm nay. Vui lòng quay lại sau!", Toast.LENGTH_LONG).show() },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Bác sĩ đang bận (Không nhận lịch)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent(context, BookingActivity::class.java).apply {
                            putExtra("doctor", doctor)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("Đặt lịch hẹn khám", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ====================================================================
// COMPONENT: KHU VỰC ĐÁNH GIÁ
// ====================================================================
@Composable
fun DoctorReviewsSection(doctor: DoctorsModel) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var reviewsList by remember { mutableStateOf<List<ReviewModel>>(emptyList()) }
    var hasCompletedAppointment by remember { mutableStateOf(false) }
    var hasReviewed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId, doctor.docUid) {
        if (currentUserId.isNotEmpty()) {
            val doctorIdNumber = doctor.Id.toString().toIntOrNull()
            db.collection("Appointments")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("doctorId", doctorIdNumber ?: doctor.Id)
                .whereEqualTo("status", "Đã khám")
                .get()
                .addOnSuccessListener { snapshot ->
                    hasCompletedAppointment = !snapshot.isEmpty
                }
        }
    }

    DisposableEffect(doctor.docUid) {
        val listener = db.collection("Reviews")
            .whereEqualTo("doctorId", doctor.docUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (snapshot != null) {
                    val list = mutableListOf<ReviewModel>()
                    var foundMyReview = false

                    for (doc in snapshot.documents) {
                        val patientId = doc.getString("patientId") ?: ""
                        if (patientId == currentUserId) foundMyReview = true

                        list.add(
                            ReviewModel(
                                id = doc.id,
                                patientId = patientId,
                                patientName = doc.getString("patientName") ?: "Ẩn danh",
                                patientAvatar = doc.getString("patientAvatar") ?: "",
                                rating = doc.getLong("rating")?.toInt() ?: 5,
                                comment = doc.getString("comment") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        )
                    }
                    reviewsList = list
                    hasReviewed = foundMyReview
                }
            }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (hasCompletedAppointment && !hasReviewed) {
            WriteReviewBox(
                onSubmit = { rating, comment ->
                    val reviewData = hashMapOf(
                        "doctorId" to doctor.docUid,
                        "patientId" to currentUserId,
                        "patientName" to "Bệnh nhân",
                        "patientAvatar" to "",
                        "rating" to rating,
                        "comment" to comment,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("Reviews").add(reviewData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show()
                        }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

        } else if (hasCompletedAppointment && hasReviewed) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Text("Bạn đã gửi đánh giá cho Bác sĩ này. Cảm ơn sự đóng góp của bạn!", modifier = Modifier.padding(12.dp), color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
        } else if (currentUserId.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Text("Bạn cần hoàn thành buổi khám với bác sĩ này để có thể viết đánh giá.", modifier = Modifier.padding(12.dp), color = Color(0xFFFF9800), fontSize = 13.sp)
            }
        }

        Text(text = "Đánh giá từ bệnh nhân (${reviewsList.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(20.dp), color = Color(0xFF1E88E5))
        } else if (reviewsList.isEmpty()) {
            Text(text = "Chưa có đánh giá nào cho bác sĩ này.", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally).padding(20.dp))
        } else {
            Column { reviewsList.forEach { review -> ReviewItemCard(review) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewBox(onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Đánh giá trải nghiệm của bạn", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                for (i in 1..5) {
                    Icon(imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp).clickable { rating = i })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = comment, onValueChange = { comment = it }, placeholder = { Text("Bác sĩ tư vấn rất nhiệt tình...", fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { if (comment.isNotBlank()) onSubmit(rating, comment) }, modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)), enabled = comment.isNotBlank()) {
                Text("Gửi đánh giá")
            }
        }
    }
}

@Composable
fun ReviewItemCard(review: ReviewModel) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(review.timestamp))
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = if (review.patientAvatar.isNotEmpty()) review.patientAvatar else "https://ui-avatars.com/api/?name=${review.patientName}&background=E3F2FD&color=1E88E5", contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = review.patientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = dateStr, color = Color.Gray, fontSize = 12.sp)
            }
            Row {
                for (i in 1..5) {
                    Icon(imageVector = if (i <= review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = review.comment, fontSize = 14.sp, color = Color.DarkGray)
    }
}

@Composable
fun DoctorInfoRow(titleText: String, contentText: String) {
    if (contentText.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(text = titleText, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            Text(text = contentText, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun IconBtn(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF1E88E5), modifier = Modifier.size(24.dp))
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}