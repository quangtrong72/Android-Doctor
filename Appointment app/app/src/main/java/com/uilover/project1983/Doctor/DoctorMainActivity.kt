package com.uilover.project1983.Doctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// IMPORT GIAO DIỆN HỒ SƠ, CHAT, VÀ VIDEO
import com.uilover.project1983.Activity.DoctorProfileScreen
import com.uilover.project1983.Chat.ChatDetailScreen
import com.uilover.project1983.Chat.VideoCallScreen

val DoctorBlue = Color(0xFF1E88E5)
val DoctorLightBlue = Color(0xFFE3F2FD)
val DoctorBg = Color(0xFFF5F7FB)
val TextPink = Color(0xFFE91E63)
val StatusGreen = Color(0xFF4CAF50)

class DoctorMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DoctorMainScreen()
            }
        }
    }
}

data class ChatRoom(
    val roomId: String,
    val uid: String,
    val nameState: MutableState<String>,
    val lastMessage: String,
    val time: String,
    val isUnread: Boolean,
    val rawTime: Long
)

data class AppointmentModel(
    val id: String,
    val patientUid: String,
    val patientName: String,
    val time: String,
    val date: String,
    val status: String
)

data class NotificationModel(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long
)

fun fetchPatientNameFromFirebase(uid: String, nameState: MutableState<String>) {
    val rtdbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users").child(uid)
    rtdbRef.get().addOnSuccessListener { snapshot ->
        val rtdbName = snapshot.child("Name").getValue(String::class.java)
            ?: snapshot.child("name").getValue(String::class.java)
            ?: snapshot.child("fullName").getValue(String::class.java)

        if (!rtdbName.isNullOrEmpty()) {
            nameState.value = rtdbName
        } else {
            val fsRef = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Users").document(uid)
            fsRef.get().addOnSuccessListener { doc ->
                val fsName = doc.getString("Name") ?: doc.getString("name") ?: doc.getString("fullName")
                if (!fsName.isNullOrEmpty()) {
                    nameState.value = fsName
                } else {
                    nameState.value = "Bệnh nhân ẩn danh"
                }
            }.addOnFailureListener { nameState.value = "Bệnh nhân ẩn danh" }
        }
    }.addOnFailureListener { nameState.value = "Bệnh nhân ẩn danh" }
}

@Composable
fun DoctorMainScreen() {
    val currentDoctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val db = FirebaseFirestore.getInstance()
    var selectedItem by remember { mutableIntStateOf(0) }

    var showVideoCall by remember { mutableStateOf(false) }
    var showChatDetail by remember { mutableStateOf(false) }
    var chatContactName by remember { mutableStateOf("") }
    var chatReceiverId by remember { mutableStateOf("") }

    var showIncomingCallDialog by remember { mutableStateOf(false) }
    var incomingCallRoomId by remember { mutableStateOf("") }
    var incomingCallerId by remember { mutableStateOf("") }

    var doctorName by remember { mutableStateOf("Đang tải...") }
    var doctorAvatar by remember { mutableStateOf("") }
    var doctorIsActive by remember { mutableStateOf(false) }
    var doctorWorkingHours by remember { mutableStateOf("08:00 - 17:00") }

    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var chatHistory by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<AppointmentModel>>(emptyList()) }
    var isLoadingChats by remember { mutableStateOf(true) }

    // 🔴 ĐẾM SỐ TIN NHẮN CHƯA ĐỌC TỰ ĐỘNG
    val unreadChatCount = chatHistory.count { it.isUnread }

    LaunchedEffect(currentDoctorId) {
        if (currentDoctorId.isNotEmpty()) {
            val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Doctors").child(currentDoctorId)
            rtdb.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {
                        doctorName = snapshot.child("Name").value?.toString() ?: "Bác sĩ"
                        doctorAvatar = snapshot.child("Picture").value?.toString() ?: ""
                        val status = snapshot.child("status").value?.toString() ?: "offline"
                        doctorIsActive = (status == "online")
                        doctorWorkingHours = snapshot.child("WorkingHours").value?.toString() ?: "08:00 - 17:00"
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }

    DisposableEffect(currentDoctorId) {
        if (currentDoctorId.isEmpty()) return@DisposableEffect onDispose {}
        val listener = db.collection("Notifications").whereEqualTo("doctorId", currentDoctorId).orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    notifications = snapshot.documents.mapNotNull { doc ->
                        NotificationModel(doc.id, doc.getString("title") ?: "Thông báo", doc.getString("content") ?: "", doc.getLong("timestamp") ?: 0L)
                    }
                }
            }
        onDispose { listener.remove() }
    }

    DisposableEffect(currentDoctorId) {
        if (currentDoctorId.isEmpty()) return@DisposableEffect onDispose {}
        val listener = db.collection("Appointments").whereEqualTo("doctorId", currentDoctorId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    appointments = snapshot.documents.mapNotNull { doc ->
                        AppointmentModel(doc.id, doc.getString("patientId") ?: "", doc.getString("patientName") ?: "Bệnh nhân", doc.getString("time") ?: "00:00", doc.getString("date") ?: "", doc.getString("status") ?: "pending")
                    }
                }
            }
        onDispose { listener.remove() }
    }

    DisposableEffect(currentDoctorId) {
        if (currentDoctorId.isEmpty()) { isLoadingChats = false; return@DisposableEffect onDispose {} }
        val listener = db.collection("Chats").whereArrayContains("participants", currentDoctorId).addSnapshotListener { snapshot, error ->
            isLoadingChats = false
            if (error != null || snapshot == null) return@addSnapshotListener
            chatHistory = snapshot.documents.mapNotNull { doc ->
                val participants = doc.get("participants") as? List<String> ?: return@mapNotNull null
                val patientUid = participants.firstOrNull { it != currentDoctorId } ?: return@mapNotNull null
                val lastMsg = doc.getString("lastMessage") ?: ""
                val timestamp = doc.getLong("timestamp") ?: 0L
                val timeStr = if (timestamp > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)) else ""

                // 🔴 LOGIC KIỂM TRA CHƯA ĐỌC TUYỆT ĐỐI CHÍNH XÁC
                val lastSenderId = doc.getString("lastSenderId") ?: ""
                val isRead = doc.getBoolean("isRead") ?: true // Mặc định tin cũ là true
                val isUnread = (lastSenderId.isNotEmpty() && lastSenderId != currentDoctorId && isRead == false)

                val nameState = mutableStateOf("Đang tải...")
                fetchPatientNameFromFirebase(patientUid, nameState)
                ChatRoom(doc.id, patientUid, nameState, lastMsg, timeStr, isUnread, timestamp)
            }.sortedByDescending { it.rawTime }
        }
        onDispose { listener.remove() }
    }

    DisposableEffect(currentDoctorId) {
        if (currentDoctorId.isEmpty()) return@DisposableEffect onDispose {}
        val callListener = db.collection("Calls").whereEqualTo("receiverId", currentDoctorId).whereEqualTo("status", "calling")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    incomingCallerId = doc.getString("callerId") ?: ""
                    incomingCallRoomId = doc.id
                    chatContactName = "Bệnh nhân đang gọi..."
                    showIncomingCallDialog = true
                } else {
                    showIncomingCallDialog = false
                }
            }
        onDispose { callListener.remove() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showVideoCall) {
            VideoCallScreen(contactName = chatContactName, receiverId = chatReceiverId, onEndCall = { showVideoCall = false })
        } else if (showChatDetail) {
            ChatDetailScreen(contactName = chatContactName, receiverId = chatReceiverId, onBackClick = { showChatDetail = false }, onVideoCallClick = { showVideoCall = true })
        } else {
            Scaffold(
                // 🔴 Đẩy số tin nhắn chưa đọc vào hàm BottomNav
                bottomBar = { DoctorBottomNav(selectedItem, unreadChatCount) { index -> selectedItem = index } }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (selectedItem) {
                        0 -> DoctorDashboardContent(
                            doctorId = currentDoctorId, doctorName = doctorName, doctorAvatar = doctorAvatar, isActive = doctorIsActive, workingHours = doctorWorkingHours, notifications = notifications, appointments = appointments, recentChats = chatHistory.take(3),
                            onChatClick = { patientName, patientUid, roomId ->
                                if (roomId.isNotEmpty()) db.collection("Chats").document(roomId).update("isRead", true)
                                chatContactName = patientName; chatReceiverId = patientUid; showChatDetail = true
                            }
                        )
                        1 -> DoctorScheduleScreen()
                        2 -> DoctorProfileScreen(onBack = { selectedItem = 0 })
                        3 -> Box(modifier = Modifier.fillMaxSize()) {
                            DoctorChatListScreen(
                                chatList = chatHistory, isLoading = isLoadingChats,
                                onChatClick = { name, uid, roomId ->
                                    if (roomId.isNotEmpty()) db.collection("Chats").document(roomId).update("isRead", true)
                                    chatContactName = name; chatReceiverId = uid; showChatDetail = true
                                }
                            )
                        }
                        4 -> Box(modifier = Modifier.fillMaxSize()) { DoctorAccountScreen() }
                    }
                }
            }
        }

        if (showIncomingCallDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(text = "Cuộc gọi đến 📞", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Bạn có một cuộc gọi Video từ $chatContactName. Bạn có muốn nhận cuộc gọi này không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            db.collection("Calls").document(incomingCallRoomId).update("status", "answered")
                            chatReceiverId = incomingCallerId
                            showIncomingCallDialog = false
                            showVideoCall = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) { Text("Đồng ý") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { db.collection("Calls").document(incomingCallRoomId).update("status", "rejected"); showIncomingCallDialog = false }) { Text("Từ chối", color = Color.Red) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardContent(
    doctorId: String, doctorName: String, doctorAvatar: String, isActive: Boolean, workingHours: String, notifications: List<NotificationModel>, appointments: List<AppointmentModel>, recentChats: List<ChatRoom>, onChatClick: (String, String, String) -> Unit
) {
    val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Doctors").child(doctorId)
    var showEditDialog by remember { mutableStateOf(false) }
    var inputHours by remember { mutableStateOf(workingHours) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false }, title = { Text("Chỉnh sửa giờ làm việc") },
            text = { OutlinedTextField(value = inputHours, onValueChange = { inputHours = it }, label = { Text("Ví dụ: 08:00 - 17:00") }, singleLine = true) },
            confirmButton = { Button(onClick = { rtdb.child("WorkingHours").setValue(inputHours); showEditDialog = false }) { Text("Lưu lại") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Hủy") } }
        )
    }

    Scaffold(containerColor = DoctorBg) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            item { Spacer(modifier = Modifier.height(30.dp)) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = if (doctorAvatar.isNotEmpty()) doctorAvatar else "https://placehold.co/100x100.png", contentDescription = "Avatar", modifier = Modifier.size(70.dp).clip(CircleShape).border(2.dp, DoctorBlue, CircleShape), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Xin chào Bác sĩ,", fontSize = 14.sp, color = Color.Gray)
                        Text(text = doctorName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text(text = "🕒 $workingHours", fontSize = 13.sp, color = DoctorBlue, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { inputHours = workingHours; showEditDialog = true }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(14.dp)) }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Switch(checked = isActive, onCheckedChange = { checked -> rtdb.child("status").setValue(if (checked) "online" else "offline") }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusGreen))
                        Text(text = if (isActive) "Sẵn sàng" else "Nghỉ ngơi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) StatusGreen else Color.Gray)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            if (notifications.isNotEmpty()) {
                item { Text(text = "Thông báo mới", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                items(notifications) { notif ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                Text(text = notif.content, fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            item { Text(text = "Lịch khám tiếp theo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                if (appointments.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Text("Hiện tại không có lịch hẹn nào.", color = Color.Gray, modifier = Modifier.padding(20.dp)) }
                } else { ActiveConsultationCard(appointment = appointments.first()) }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Tin nhắn gần đây", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "Xem tất cả", color = DoctorBlue, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (recentChats.isEmpty()) {
                item { Text("Chưa có tin nhắn nào.", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp)) }
            } else { items(recentChats) { chat -> RecentChatItem(chat = chat, onClick = { onChatClick(chat.nameState.value, chat.uid, chat.roomId) }) } }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ActiveConsultationCard(appointment: AppointmentModel) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (appointment.status == "Chưa thanh toán") Color(0xFFFF9800) else Color(0xFF4CAF50)
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (appointment.status == "Chưa thanh toán") "Cần thu tiền - ${appointment.date}" else "Sắp diễn ra - ${appointment.date}", color = if (appointment.status == "Chưa thanh toán") Color(0xFFFF9800) else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = appointment.patientName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = "Khung giờ: ${appointment.time}", fontSize = 14.sp, color = DoctorBlue, modifier = Modifier.padding(top = 4.dp))
            Row(modifier = Modifier.padding(top = 16.dp)) {
                if (appointment.status == "Chưa thanh toán") {
                    Button(onClick = { FirebaseFirestore.getInstance().collection("Appointments").document(appointment.id).update("status", "Đã thanh toán") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), shape = RoundedCornerShape(12.dp)) { Text("Đã thu tiền", color = Color.White, fontWeight = FontWeight.Bold) }
                } else {
                    OutlinedButton(onClick = { FirebaseFirestore.getInstance().collection("Appointments").document(appointment.id).update("status", "Đã khám") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Hoàn thành", color = StatusGreen, fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DoctorBlue), shape = RoundedCornerShape(12.dp)) { Text("Liên hệ") }
            }
        }
    }
}

@Composable
fun RecentChatItem(chat: ChatRoom, onClick: () -> Unit) {
    val isUnread = chat.isUnread
    // 🔴 GIAO DIỆN TIN NHẮN CHƯA ĐỌC NỔI BẬT LÊN
    val msgColor = if (isUnread) Color.Black else Color.Gray
    val msgWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Normal
    val nameWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.Gray) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = chat.nameState.value, fontWeight = nameWeight, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = chat.lastMessage, fontSize = 13.sp, color = msgColor, fontWeight = msgWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = chat.time, fontSize = 11.sp, color = msgColor, fontWeight = msgWeight)
                if (isUnread) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DoctorBlue))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorChatListScreen(chatList: List<ChatRoom>, isLoading: Boolean, onChatClick: (String, String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7F9))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) }
            Text("Tin nhắn bệnh nhân", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.Black) }
        }

        var searchQuery by remember { mutableStateOf("") }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f).shadow(2.dp, RoundedCornerShape(24.dp)),
                placeholder = { Text("Tìm kiếm bệnh nhân...", color = Color.Gray, fontSize = 14.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(24.dp), singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(top = 24.dp)) {
            if (isLoading) CircularProgressIndicator(color = DoctorBlue, modifier = Modifier.align(Alignment.Center))
            else if (chatList.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hộp thư trống", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hiện chưa có bệnh nhân nào nhắn tin.", fontSize = 14.sp, color = Color.LightGray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatList) { chat ->
                        val isUnread = chat.isUnread
                        val msgColor = if (isUnread) Color.Black else Color.Gray
                        val msgWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Normal
                        val nameWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold

                        Row(modifier = Modifier.fillMaxWidth().clickable { onChatClick(chat.nameState.value, chat.uid, chat.roomId) }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(30.dp)) }
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.White).padding(2.dp)) { Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF4CAF50))) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = chat.nameState.value, fontWeight = nameWeight, fontSize = 16.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = chat.lastMessage, fontSize = 13.sp, color = msgColor, fontWeight = msgWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = chat.time, fontSize = 12.sp, color = msgColor, fontWeight = msgWeight)
                                if (isUnread) { Spacer(modifier = Modifier.height(4.dp)); Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DoctorBlue)) }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// 🔴 THÊM BADGEDBOX (Huy hiệu đỏ) VÀO THANH BOTTOM NAV
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorBottomNav(selectedItem: Int, unreadChatCount: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected = selectedItem == 0, onClick = { onItemSelected(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Trang chủ", maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorBlue, indicatorColor = DoctorLightBlue))
        NavigationBarItem(selected = selectedItem == 1, onClick = { onItemSelected(1) }, icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Lịch", maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorBlue, indicatorColor = DoctorLightBlue))
        NavigationBarItem(selected = selectedItem == 2, onClick = { onItemSelected(2) }, icon = { Icon(Icons.Default.ListAlt, null) }, label = { Text("Hồ sơ", maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorBlue, indicatorColor = DoctorLightBlue))

        NavigationBarItem(
            selected = selectedItem == 3,
            onClick = { onItemSelected(3) },
            icon = {
                if (unreadChatCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color.Red, contentColor = Color.White) {
                                Text(text = unreadChatCount.toString(), fontSize = 10.sp)
                            }
                        }
                    ) { Icon(Icons.Default.Chat, null) }
                } else { Icon(Icons.Default.Chat, null) }
            },
            label = { Text("Chat", maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorBlue, indicatorColor = DoctorLightBlue)
        )

        NavigationBarItem(selected = selectedItem == 4, onClick = { onItemSelected(4) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Tôi", maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorBlue, indicatorColor = DoctorLightBlue))
    }
}