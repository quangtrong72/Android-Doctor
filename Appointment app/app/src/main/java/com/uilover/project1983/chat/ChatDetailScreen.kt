package com.uilover.project1983.Chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val BgColor = Color(0xFFF4F7F9)
val SentBubbleColor = Color(0xFF212529)
val ReceivedBubbleColor = Color.White
val TextGray = Color(0xFF888888)
val GradientSend = Brush.linearGradient(listOf(Color(0xFFD4E157), Color(0xFF66BB6A)))

data class MessageModel(
    val id: String = "",
    val text: String = "",
    val isFromMe: Boolean = false,
    val time: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    contactName: String = "Người dùng",
    receiverId: String,
    onBackClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<MessageModel>>(emptyList()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showIncomingCallDialog by remember { mutableStateOf(false) }
    var incomingCallRoomId by remember { mutableStateOf("") }
    var incomingCallerId by remember { mutableStateOf("") }
    var showLocalVideoCall by remember { mutableStateOf(false) }

    val roomId = remember(currentUserId, receiverId) {
        if (currentUserId < receiverId) "${currentUserId}_$receiverId" else "${receiverId}_$currentUserId"
    }

    // Bộ lắng nghe đọc và cập nhật trạng thái tin nhắn
    DisposableEffect(roomId) {
        if (currentUserId.isEmpty() || receiverId.isEmpty()) return@DisposableEffect onDispose {}

        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("Chats").document(roomId).collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val loadedMessages = snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: ""
                        val senderId = doc.getString("senderId") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val timeString = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(Date(timestamp))

                        MessageModel(
                            id = doc.id,
                            text = text,
                            isFromMe = senderId == currentUserId,
                            time = timeString
                        )
                    }
                    messages = loadedMessages

                    // 🔴 ĐÃ FIX LỖI MẤT IN ĐẬM:
                    // Chỉ cập nhật thành "Đã đọc" (isRead = true) khi tin nhắn cuối cùng trong phòng là do ĐỐI PHƯƠNG gửi đến cho mình
                    if (loadedMessages.isNotEmpty()) {
                        val lastMessage = loadedMessages.last()
                        if (!lastMessage.isFromMe) {
                            db.collection("Chats").document(roomId).update("isRead", true)
                        }
                    }

                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            }
        onDispose { listener.remove() }
    }

    DisposableEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@DisposableEffect onDispose {}
        val db = FirebaseFirestore.getInstance()

        val callListener = db.collection("Calls")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "calling")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val callerId = doc.getString("callerId") ?: ""
                    if (callerId == receiverId) {
                        incomingCallerId = callerId
                        incomingCallRoomId = doc.id
                        showIncomingCallDialog = true
                    }
                } else {
                    showIncomingCallDialog = false
                }
            }
        onDispose { callListener.remove() }
    }

    fun sendMessageToFirestore(text: String) {
        if (currentUserId.isEmpty() || receiverId.isEmpty() || text.isBlank()) return
        val db = FirebaseFirestore.getInstance()
        val timestamp = System.currentTimeMillis()

        val msgData = hashMapOf(
            "text" to text.trim(),
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "timestamp" to timestamp
        )

        db.collection("Chats").document(roomId).collection("Messages").add(msgData)
            .addOnFailureListener { e -> Toast.makeText(context, "Gửi thất bại: ${e.message}", Toast.LENGTH_LONG).show() }

        // Gửi tin nhắn mới đặt mặc định phòng chat là chưa đọc (isRead = false)
        val roomData = hashMapOf(
            "participants" to listOf(currentUserId, receiverId),
            "lastMessage" to text.trim(),
            "timestamp" to timestamp,
            "lastSenderId" to currentUserId,
            "isRead" to false
        )
        db.collection("Chats").document(roomId).set(roomData, SetOptions.merge())

        messageText = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(contactName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Online", fontSize = 12.sp, color = TextGray)
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.Black) }
                    },
                    actions = {
                        IconButton(onClick = onVideoCallClick) { Icon(Icons.Rounded.Videocam, contentDescription = "Gọi Video", tint = Color.Black) }
                        IconButton(onClick = { /* Gọi thoại */ }) { Icon(Icons.Rounded.Call, contentDescription = "Gọi thoại", tint = Color.Black) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
                )
            },
            bottomBar = {
                Row(modifier = Modifier.fillMaxWidth().background(BgColor).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = messageText, onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).shadow(4.dp, RoundedCornerShape(30.dp)),
                        placeholder = { Text("Type messages...", fontSize = 14.sp, color = Color.Gray) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Color(0xFF66BB6A)),
                        shape = RoundedCornerShape(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).shadow(2.dp, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Mic, contentDescription = "Mic", tint = Color.Black, modifier = Modifier.size(24.dp)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(45.dp).clip(CircleShape).background(if (messageText.isNotBlank()) GradientSend else Brush.linearGradient(listOf(Color.Gray, Color.LightGray)))
                            .clickable(enabled = messageText.isNotBlank(), onClick = { sendMessageToFirestore(messageText) }),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp).padding(start = 2.dp)) }
                }
            },
            containerColor = BgColor
        ) { paddingValues ->
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { msg -> ChatBubble(message = msg) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        if (showIncomingCallDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(text = "Cuộc gọi Video 📞", fontWeight = FontWeight.Bold) },
                text = { Text(text = "$contactName đang gọi cho bạn. Bạn có muốn nghe máy không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("Calls").document(incomingCallRoomId).update("status", "answered")
                            showIncomingCallDialog = false
                            showLocalVideoCall = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Nghe máy") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { FirebaseFirestore.getInstance().collection("Calls").document(incomingCallRoomId).update("status", "rejected"); showIncomingCallDialog = false }) { Text("Từ chối", color = Color.Red) }
                }
            )
        }

        if (showLocalVideoCall) {
            VideoCallScreen(contactName = contactName, receiverId = incomingCallerId, onEndCall = { showLocalVideoCall = false })
        }
    }
}

@Composable
fun ChatBubble(message: MessageModel) {
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) SentBubbleColor else ReceivedBubbleColor
    val textColor = if (isMe) Color.White else Color.Black
    val shape = if (isMe) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp) else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Box(modifier = Modifier.widthIn(max = 280.dp).clip(shape).background(bubbleColor).padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(text = message.text, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
            }
            Row(modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = message.time, fontSize = 11.sp, color = TextGray)
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.DoneAll, contentDescription = "Đã đọc", tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}