package com.uilover.project1983.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uilover.project1983.Domain.CategoryModel
import com.uilover.project1983.Domain.DoctorsModel
import com.uilover.project1983.ViewModel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// IMPORT GIAO DIỆN CHAT, VIDEO VÀ THÔNG BÁO
import com.uilover.project1983.Chat.ChatDetailScreen
import com.uilover.project1983.Chat.VideoCallScreen
import com.uilover.project1983.Activity.NotificationScreen

val PrimaryBlue = Color(0xFF1E88E5)
val GradientBlueStart = Color(0xFF42A5F5)
val BackgroundApp = Color(0xFFF7F9FC)
val LightBlueCard = Color(0xFFE3F2FD)
val TextDarkBlue = Color(0xFF1A237E)
val TextSecondary = Color(0xFF757575)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTab = intent.getIntExtra("NAVIGATE_TO_TAB", 0)
        val doctorName = intent.getStringExtra("DOCTOR_NAME") ?: ""
        val doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""

        setContent {
            MaterialTheme {
                Surface(color = BackgroundApp, modifier = Modifier.fillMaxSize()) {
                    MainScreen(initialTab, doctorName, doctorId)
                }
            }
        }
    }
}

data class ChatSnippet(
    val roomId: String,
    val lastMessage: String,
    val time: String,
    val timestamp: Long,
    val isUnread: Boolean
)

@Composable
fun MainScreen(initialTab: Int, doctorName: String, doctorId: String, viewModel: MainViewModel = viewModel()) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var selectedItem by remember { mutableIntStateOf(initialTab) }

    var showVideoCall by remember { mutableStateOf(false) }
    var showChatDetail by remember { mutableStateOf(initialTab == 3 && doctorId.isNotEmpty()) }
    var currentChatName by remember { mutableStateOf(doctorName) }
    var currentChatReceiverId by remember { mutableStateOf(doctorId) }

    // 🔴 1. KHAI BÁO CÁC BIẾN QUẢN LÝ HỘP THOẠI
    var showIncomingCallDialog by remember { mutableStateOf(false) }
    var incomingCallRoomId by remember { mutableStateOf("") }
    var incomingCallerId by remember { mutableStateOf("") }

    val categories by viewModel.category.observeAsState(emptyList())
    val doctors by viewModel.doctors.observeAsState(emptyList())
    var chatSnippets by remember { mutableStateOf<Map<String, ChatSnippet>>(emptyMap()) }

    LaunchedEffect(Unit) {
        viewModel.loadCategory()
        viewModel.loadDoctors()
    }

    DisposableEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@DisposableEffect onDispose {}
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("Chats").whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val snippets = mutableMapOf<String, ChatSnippet>()
                for (doc in snapshot.documents) {
                    val participants = doc.get("participants") as? List<String> ?: continue
                    val doctorUid = participants.firstOrNull { it != currentUserId } ?: continue
                    val lastMsg = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val timeStr = if (timestamp > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)) else ""
                    val lastSenderId = doc.getString("lastSenderId") ?: ""
                    val isRead = doc.getBoolean("isRead") ?: true
                    val isUnread = (lastSenderId != currentUserId) && !isRead
                    snippets[doctorUid] = ChatSnippet(doc.id, lastMsg, timeStr, timestamp, isUnread)
                }
                chatSnippets = snippets
            }
        onDispose { listener.remove() }
    }

    // 🔴 2. BỘ LẮNG NGHE CUỘC GỌI TOÀN CỤC CHO BỆNH NHÂN
    DisposableEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@DisposableEffect onDispose {}
        val db = FirebaseFirestore.getInstance()
        val callListener = db.collection("Calls").whereEqualTo("receiverId", currentUserId).whereEqualTo("status", "calling")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    incomingCallerId = doc.getString("callerId") ?: ""
                    incomingCallRoomId = doc.id
                    currentChatName = "Bác sĩ đang gọi..."

                    // Bật hộp thoại, khoan bật Camera
                    showIncomingCallDialog = true
                } else {
                    showIncomingCallDialog = false
                }
            }
        onDispose { callListener.remove() }
    }

    // 🔴 3. DÙNG BOX ĐỂ XẾP LỚP: MÀN HÌNH NỀN VÀ HỘP THOẠI NỔI
    Box(modifier = Modifier.fillMaxSize()) {

        // LỚP NỀN: CÁC MÀN HÌNH BÌNH THƯỜNG
        if (showChatDetail) {
            ChatDetailScreen(
                contactName = currentChatName,
                receiverId = currentChatReceiverId,
                onBackClick = { showChatDetail = false },
                onVideoCallClick = { showVideoCall = true } // Bệnh nhân chủ động gọi đi
            )
        } else {
            Scaffold(
                bottomBar = { BottomNavigationBar(selectedItem, { selectedItem = it }) },
                containerColor = BackgroundApp
            ) { paddingValues ->
                when (selectedItem) {
                    0 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            item { HeaderSection() }
                            item { BannerAndSearchSection() }
                            item { SectionTitle("Chuyên Khoa", "Xem tất cả") }
                            item {
                                if (categories.isEmpty()) CircularProgressIndicator(modifier = Modifier.padding(20.dp), color = PrimaryBlue)
                                else CategoryListSection(categories)
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                            item { SectionTitle("Bác Sĩ Nổi Bật", "Xem tất cả") }
                            if (doctors.isEmpty()) {
                                item { CircularProgressIndicator(modifier = Modifier.padding(20.dp), color = PrimaryBlue) }
                            } else {
                                items(doctors.size) { index -> DoctorVerticalCard(doctor = doctors[index]) }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                    1 -> {
                        var showNewRegistration by remember { mutableStateOf(false) }
                        if (showNewRegistration) NewPatientRegistrationScreen(onBackClick = { showNewRegistration = false })
                        else PatientProfileScreen(paddingValues, { selectedItem = 0 }, { showNewRegistration = true })
                    }
                    2 -> Box(modifier = Modifier.padding(paddingValues)) { TicketHistoryScreen(onBackClick = { selectedItem = 0 }) }
                    3 -> {
                        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                            PatientChatListScreen(
                                doctorsList = doctors,
                                chatSnippets = chatSnippets,
                                onChatClick = { name, uid, roomId ->
                                    if (roomId.isNotEmpty()) {
                                        FirebaseFirestore.getInstance().collection("Chats").document(roomId).update("isRead", true)
                                    }
                                    currentChatName = name
                                    currentChatReceiverId = uid
                                    showChatDetail = true
                                }
                            )
                        }
                    }
                    4 -> Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) { NotificationScreen() }
                    5 -> Box(modifier = Modifier.fillMaxSize()) { AccountScreen() }
                }
            }
        }

        // 🔴 4. LỚP NỔI: HỘP THOẠI ĐỒNG Ý / TỪ CHỐI
        if (showIncomingCallDialog) {
            AlertDialog(
                onDismissRequest = { /* Không làm gì để bắt buộc người dùng bấm nút */ },
                title = { Text(text = "Cuộc gọi đến 📞", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Bác sĩ đang gọi Video cho bạn. Bạn có muốn nhận cuộc gọi này không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("Calls").document(incomingCallRoomId).update("status", "answered")

                            // Gắn ID bác sĩ gọi vào để vào đúng phòng
                            currentChatReceiverId = incomingCallerId
                            showIncomingCallDialog = false
                            showVideoCall = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Nghe máy")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("Calls").document(incomingCallRoomId).update("status", "rejected")
                            showIncomingCallDialog = false
                        }
                    ) {
                        Text("Từ chối", color = Color.Red)
                    }
                }
            )
        }

        // 🔴 5. LỚP NỔI: MÀN HÌNH CAMERA ZEGOCLOUD ĐÈ LÊN TẤT CẢ
        if (showVideoCall) {
            VideoCallScreen(
                contactName = currentChatName,
                receiverId = currentChatReceiverId,
                onEndCall = { showVideoCall = false }
            )
        }
    }
}

// CÁC HÀM UI PHÍA DƯỚI GIỮ NGUYÊN HOÀN TOÀN NHƯ CŨ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientChatListScreen(
    doctorsList: List<DoctorsModel>,
    chatSnippets: Map<String, ChatSnippet>,
    onChatClick: (String, String, String) -> Unit
) {
    val sortedDoctors = remember(doctorsList, chatSnippets) {
        doctorsList.sortedByDescending { doctor -> chatSnippets[doctor.docUid]?.timestamp ?: 0L }
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7F9))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) }
            Text("Chat List", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.Black) }
        }
        var searchQuery by remember { mutableStateOf("") }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f).shadow(2.dp, RoundedCornerShape(24.dp)),
                placeholder = { Text("Search by doctor's name", color = Color.Gray, fontSize = 14.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(24.dp), singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White).shadow(2.dp, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.Black) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(top = 24.dp)) {
            items(sortedDoctors.size) { index ->
                val doctor = sortedDoctors[index]
                val snippet = chatSnippets[doctor.docUid]
                val displayMsg = snippet?.lastMessage?.takeIf { it.isNotBlank() } ?: "Nhấn để bắt đầu trò chuyện..."
                val displayTime = snippet?.time ?: ""
                val isUnread = snippet?.isUnread == true
                val msgColor = if (isUnread) Color.Black else Color.Gray
                val msgWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Normal
                val nameWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onChatClick(doctor.Name, doctor.docUid, snippet?.roomId ?: "") }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(model = doctor.Picture, contentDescription = null, modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFE0E0E0)), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.White).padding(2.dp)) { Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF4CAF50))) }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = doctor.Name, fontWeight = nameWeight, fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = displayMsg, fontSize = 13.sp, color = msgColor, fontWeight = msgWeight, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = displayTime, fontSize = 12.sp, color = msgColor, fontWeight = msgWeight)
                        if (isUnread) { Spacer(modifier = Modifier.height(4.dp)); Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryBlue)) }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "Xin chào, Sơn", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDarkBlue)
            Text(text = "Hôm nay bạn thế nào?", fontSize = 14.sp, color = TextSecondary)
        }
        Box(modifier = Modifier.size(45.dp).background(Color.White, shape = CircleShape).shadow(2.dp, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryBlue) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerAndSearchSection() {
    var searchQuery by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 25.dp).clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(GradientBlueStart, PrimaryBlue)))) {
            Row(modifier = Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Đặt lịch hẹn với\nbác sĩ tốt nhất", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White) { Text(text = "Khám phá ngay", color = PrimaryBlue, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Tìm bác sĩ, chuyên khoa...", color = TextSecondary, fontSize = 14.sp) },
            trailingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryBlue) }, modifier = Modifier.fillMaxWidth(0.9f).height(55.dp).shadow(12.dp, RoundedCornerShape(28.dp)).background(Color.White, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), singleLine = true
        )
    }
}

@Composable
fun SectionTitle(title: String, actionText: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue)
        Text(text = actionText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue)
    }
}

@Composable
fun CategoryListSection(categories: List<CategoryModel>) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        items(categories.size) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(LightBlueCard), contentAlignment = Alignment.Center) { AsyncImage(model = categories[index].Picture, contentDescription = null, modifier = Modifier.size(35.dp), contentScale = ContentScale.Fit) }
                Text(text = categories[index].Name, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun DoctorVerticalCard(doctor: DoctorsModel) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", doctor)
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = doctor.Picture, contentDescription = null, modifier = Modifier.size(90.dp).clip(RoundedCornerShape(16.dp)).background(LightBlueCard), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = doctor.Name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue)
                Text(text = doctor.Special, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Text(text = " ${doctor.Rating}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkBlue)
                    Text(text = " | ${doctor.Experience} Năm KN", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Top, modifier = Modifier.height(90.dp)) { Icon(imageVector = Icons.Rounded.FavoriteBorder, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp)) }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedItem: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf("Trang chủ", "Hồ sơ", "Phiếu khám", "Chat", "Thông báo", "Tài khoản")
    val icons = listOf(Icons.Rounded.Home, Icons.Rounded.Assignment, Icons.Rounded.DateRange, Icons.Rounded.Chat, Icons.Rounded.Notifications, Icons.Rounded.Person)
    NavigationBar(containerColor = Color.White, tonalElevation = 16.dp, modifier = Modifier.height(85.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
        items.forEachIndexed { index, title ->
            val isSelected = selectedItem == index
            NavigationBarItem(
                icon = { Icon(imageVector = icons[index], contentDescription = title, modifier = Modifier.size(24.dp), tint = if (isSelected) PrimaryBlue else TextSecondary) },
                label = { Text(text = title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) PrimaryBlue else TextSecondary, maxLines = 1) },
                selected = isSelected, onClick = { onTabSelected(index) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}