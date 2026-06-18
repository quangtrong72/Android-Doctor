package com.uilover.project1983.Activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Model lưu dữ liệu Thông báo cá nhân
data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "success", "reminder", "cancel", "update"
    val time: String = "",
    val isRead: Boolean = false
)

// Model lưu dữ liệu Tin tức bệnh viện
data class NewsModel(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val author: String = "Bệnh viện Đa khoa"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// THÊM THAM SỐ onBackClick VÀO HÀM
fun NotificationScreen(onBackClick: () -> Unit = {}) {
    val BluePrimary = Color(0xFF1E88E5)
    val BgGray = Color(0xFFF8F9FA)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Thông báo", "Tin tức")

    // State cho Thông báo
    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var isLoadingNotif by remember { mutableStateOf(true) }

    // State cho Tin tức
    var newsList by remember { mutableStateOf<List<NewsModel>>(emptyList()) }
    var isLoadingNews by remember { mutableStateOf(true) }

    // Gọi dữ liệu thật từ Cloud Firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        // 1. LẤY THÔNG BÁO CÁ NHÂN (Lọc theo userId)
        if (uid != null) {
            db.collection("Notifications")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<NotificationModel>()
                    for (doc in querySnapshot.documents) {
                        list.add(
                            NotificationModel(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                type = doc.getString("type") ?: "success",
                                time = doc.getString("time") ?: "",
                                isRead = doc.getBoolean("isRead") ?: false
                            )
                        )
                    }
                    notifications = list.reversed() // Đảo ngược để thông báo mới nhất lên đầu
                    isLoadingNotif = false
                }
                .addOnFailureListener { isLoadingNotif = false }
        } else {
            isLoadingNotif = false
        }

        // 2. LẤY TIN TỨC CHUNG (Không cần lọc userId)
        db.collection("News")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = mutableListOf<NewsModel>()
                for (doc in querySnapshot.documents) {
                    list.add(
                        NewsModel(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            date = doc.getString("date") ?: "",
                            author = doc.getString("author") ?: "Bệnh viện Đa khoa"
                        )
                    )
                }
                newsList = list.reversed() // Tin tức mới nhất lên đầu
                isLoadingNews = false
            }
            .addOnFailureListener { isLoadingNews = false }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BluePrimary)) {
                CenterAlignedTopAppBar(
                    title = { Text("Thông tin & Cập nhật", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    // THÊM NÚT BACK Ở ĐÂY
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                // THANH TAB (Thông báo / Tin tức)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = BluePrimary,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color.White,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTabIndex == index) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 15.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> NotificationTabContent(isLoadingNotif, notifications)
                1 -> NewsTabContent(isLoadingNews, newsList)
            }
        }
    }
}

// --- GIAO DIỆN TAB 1: THÔNG BÁO ---
@Composable
fun NotificationTabContent(isLoading: Boolean, notifications: List<NotificationModel>) {
    val BluePrimary = Color(0xFF1E88E5)

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BluePrimary)
        }
    } else if (notifications.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.NotificationsOff, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Bạn chưa có thông báo cá nhân nào.", color = Color.Gray, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notifications) { notif ->
                NotificationItem(notif)
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- GIAO DIỆN TAB 2: TIN TỨC ---
@Composable
fun NewsTabContent(isLoading: Boolean, newsList: List<NewsModel>) {
    val BluePrimary = Color(0xFF1E88E5)

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BluePrimary)
        }
    } else if (newsList.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Article, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Hiện tại chưa có bảng tin nào mới từ bệnh viện.", color = Color.Gray, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(newsList) { news ->
                NewsItem(news)
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- COMPONENT: THẺ THÔNG BÁO ---
@Composable
fun NotificationItem(notif: NotificationModel) {
    val (icon, bgColor, iconColor) = when (notif.type) {
        "success" -> Triple(Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF4CAF50))
        "reminder" -> Triple(Icons.Default.AccessAlarm, Color(0xFFFFF3E0), Color(0xFFFF9800))
        "cancel" -> Triple(Icons.Default.Cancel, Color(0xFFFFEBEE), Color(0xFFF44336))
        "update" -> Triple(Icons.Default.Autorenew, Color(0xFFE3F2FD), Color(0xFF2196F3))
        else -> Triple(Icons.Default.Info, Color(0xFFF3E5F5), Color(0xFF9C27B0))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (notif.isRead) Color.White else Color(0xFFF0F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notif.isRead) 1.dp else 3.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = notif.title, fontWeight = if (notif.isRead) FontWeight.Medium else FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!notif.isRead) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = notif.message, color = Color.DarkGray, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = notif.time, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

// --- COMPONENT: THẺ TIN TỨC ---
@Composable
fun NewsItem(news: NewsModel) {
    val BluePrimary = Color(0xFF1E88E5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = news.author, color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = news.date, color = Color.Gray, fontSize = 11.sp)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = news.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A237E))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = news.content, color = Color.DarkGray, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}