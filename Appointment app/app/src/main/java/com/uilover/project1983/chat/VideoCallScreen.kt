package com.uilover.project1983.Chat

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

@Composable
fun VideoCallScreen(
    contactName: String,
    receiverId: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
    val db = FirebaseFirestore.getInstance()

    // 1. CẤU HÌNH ZEGOCLOUD
    val appID: Long = 885006500L
    val appSign = "6b96d76ee9484537261a4fb535c6707729d7b5b9737723d01a7a1610ddfd5fe5"

    // 2. Tạo mã phòng chung (RoomID)
    val roomId = remember(currentUserId, receiverId) {
        if (currentUserId < receiverId) "${currentUserId}_$receiverId" else "${receiverId}_$currentUserId"
    }

    // 🔴 ĐÃ FIX LỖI KHÔNG ĐỔ CHUÔNG BÊN KIA:
    // Vừa vào màn hình là đẩy ngay trạng thái "calling" lên Firebase
    LaunchedEffect(roomId) {
        val callRef = db.collection("Calls").document(roomId)
        val callData = hashMapOf(
            "callerId" to currentUserId,
            "receiverId" to receiverId,
            "status" to "calling",
            "timestamp" to System.currentTimeMillis()
        )
        // Ghi lên Firebase để máy bên kia bắt được sự kiện
        callRef.set(callData, SetOptions.merge())
    }

    // 3. LẮNG NGHE TÍN HIỆU CÚP MÁY TỪ BÊN KIA
    DisposableEffect(roomId) {
        val listener = db.collection("Calls").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.getString("status") == "ended") {
                        onEndCall()
                    }
                }
            }
        onDispose { listener.remove() }
    }

    // 4. HÀM TỰ ĐỘNG GỬI TIN NHẮN THÔNG BÁO CUỘC GỌI KẾT THÚC
    fun sendCallEndedMessageToChat() {
        val messageContent = "📞 Cuộc gọi Video đã kết thúc"
        val timestamp = System.currentTimeMillis()

        db.collection("Chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val participants = doc.get("participants") as? List<String> ?: continue
                    if (participants.contains(receiverId)) {
                        val chatRoomId = doc.id
                        val messageData = hashMapOf(
                            "senderId" to currentUserId,
                            "text" to messageContent,
                            "timestamp" to timestamp
                        )
                        db.collection("Chats").document(chatRoomId).collection("Messages").add(messageData)

                        val updateData = mapOf(
                            "lastMessage" to messageContent,
                            "lastSenderId" to currentUserId,
                            "timestamp" to timestamp,
                            "isRender" to false
                        )
                        db.collection("Chats").document(chatRoomId).update(updateData)
                        break
                    }
                }
            }
    }

    // 5. HIỂN THỊ CAMERA ZEGOCLOUD
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FragmentContainerView(ctx).apply { id = View.generateViewId() }
        },
        update = { view ->
            val activity = context as? FragmentActivity
                ?: throw IllegalStateException("Activity phải kế thừa AppCompatActivity")

            // Cấu hình mặc định bật sẵn Camera và Mic
            val config = ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
            config.turnOnCameraWhenJoining = true
            config.turnOnMicrophoneWhenJoining = true

            // BẮT SỰ KIỆN KHI MÌNH CHỦ ĐỘNG BẤM CÚP MÁY
            config.leaveCallListener = object : ZegoUIKitPrebuiltCallFragment.LeaveCallListener {
                override fun onLeaveCall() {
                    // Cập nhật Firebase để máy kia tắt theo
                    db.collection("Calls").document(roomId).update("status", "ended")
                    sendCallEndedMessageToChat()
                    onEndCall()
                }
            }

            val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                appID,
                appSign,
                currentUserId,
                "Người dùng", // Bạn có thể thay bằng tên thật nếu truyền biến vào
                roomId,
                config
            )

            activity.supportFragmentManager.beginTransaction()
                .replace(view.id, fragment)
                .commit()
        }
    )
}