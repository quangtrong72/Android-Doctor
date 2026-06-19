package com.uilover.project1983.Activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.uilover.project1983.Domain.DoctorsModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Model cấu trúc dữ liệu hồ sơ bệnh nhân trên Cloud Firestore
data class PatientProfileModel(
    var id: String = "",
    var fullName: String = "",
    var dob: String = "",
    var phone: String = ""
)

class BookingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val doctor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("doctor", DoctorsModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("doctor") as? DoctorsModel
        }

        setContent {
            MaterialTheme {
                if (doctor != null) {
                    BookingScreen(doctor = doctor, onBack = { finish() })
                } else {
                    Toast.makeText(this, "Lỗi dữ liệu bác sĩ", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}

val BluePrimary = Color(0xFF1E88E5)
val BgGray = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(doctor: DoctorsModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }
    var isSavingBooking by remember { mutableStateOf(false) }
    var generatedTicketId by remember { mutableStateOf("Đang khởi tạo...") }

    // 🔴 1. KHAI BÁO BIẾN LƯU TRẠNG THÁI REALTIME CỦA BÁC SĨ
    var doctorRealtimeStatus by remember { mutableStateOf(doctor.status) }

    // 🔴 2. LẮNG NGHE SỰ THAY ĐỔI TRẠNG THÁI TỪ FIREBASE REALTIME DB
    DisposableEffect(doctor.docUid) {
        if (doctor.docUid.isNotEmpty()) {
            val ref = FirebaseDatabase.getInstance().getReference("Doctors").child(doctor.docUid)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    if (status != null) doctorRealtimeStatus = status
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        } else {
            onDispose { }
        }
    }

    // --- Trạng thái Cổng thanh toán ---
    var showPaymentGateway by remember { mutableStateOf(false) }
    var tempBookingId by remember { mutableStateOf("") }

    // 0 là Chuyển khoản QR, 1 là Thanh toán trực tiếp
    var selectedPaymentMethod by remember { mutableIntStateOf(0) }
    var finalTicketStatus by remember { mutableStateOf("Chưa thanh toán") }

    // --- Trạng thái Bước 1 ---
    var selectedService by remember { mutableStateOf<String?>(null) }
    var servicePrice by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    // Dữ liệu quản lý Lịch & Giờ
    var bookedTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingTimes by remember { mutableStateOf(false) }

    val (morningSlots, afternoonSlots) = remember(doctor.Schedule) {
        generateTimeSlots(doctor.Schedule)
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            selectedDate = formattedDate
            selectedTime = null
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.minDate = calendar.timeInMillis

    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            isFetchingTimes = true
            val db = FirebaseFirestore.getInstance()
            db.collection("Appointments")
                .whereEqualTo("doctorId", doctor.Id)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener { snapshot ->
                    bookedTimes = snapshot.documents
                        .filter { it.getString("status") != "Đã hủy" }
                        .mapNotNull { it.getString("time") }
                    isFetchingTimes = false
                }
                .addOnFailureListener {
                    bookedTimes = emptyList()
                    isFetchingTimes = false
                }
        } else {
            bookedTimes = emptyList()
        }
    }

    // TÍNH TOÁN CÁC GIỜ BỊ KHÓA
    val disabledTimes = remember(bookedTimes, selectedDate, morningSlots, afternoonSlots) {
        val disabled = bookedTimes.toMutableList()

        if (selectedDate != null) {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val currentTime = Calendar.getInstance().timeInMillis

            val allSlots = morningSlots + afternoonSlots
            allSlots.forEach { slot ->
                val slotStartTimeStr = slot.split(" - ")[0]
                val slotDateTimeStr = "$selectedDate $slotStartTimeStr"
                try {
                    val slotDate = formatter.parse(slotDateTimeStr)
                    if (slotDate != null && slotDate.time <= (currentTime + 3600000)) {
                        disabled.add(slot)
                    }
                } catch (e: Exception) {
                    // Ignore lỗi
                }
            }
        }
        disabled.toList()
    }

    // --- Trạng thái Bước 2 ---
    var patientProfiles by remember { mutableStateOf<List<PatientProfileModel>>(emptyList()) }
    var selectedProfile by remember { mutableStateOf<PatientProfileModel?>(null) }
    var isFetchingPatient by remember { mutableStateOf(true) }

    var refreshKey by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshKey++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshKey) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("PatientProfiles")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val profiles = mutableListOf<PatientProfileModel>()
                    for (document in querySnapshot.documents) {
                        val name = document.getString("fullName") ?: ""
                        if (name.isNotEmpty()) {
                            profiles.add(PatientProfileModel(document.id, name, document.getString("dob") ?: "", document.getString("phone") ?: ""))
                        }
                    }
                    patientProfiles = profiles
                    if (profiles.isNotEmpty() && selectedProfile == null) selectedProfile = profiles[0]
                    isFetchingPatient = false
                }.addOnFailureListener { isFetchingPatient = false }
        } else {
            isFetchingPatient = false
        }
    }

    // --- Trạng thái Bước 3 & Bước 4 ---
    var addonService by remember { mutableStateOf("Không") }
    var addonPrice by remember { mutableIntStateOf(0) }
    val platformFee = 9100
    val discount = 2000
    val finalTotal = servicePrice + addonPrice + platformFee - discount

    val saveBookingToFirebase: (String) -> Unit = { status ->
        isSavingBooking = true
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val appointmentData = hashMapOf(
            "userId" to uid,
            "patientId" to (selectedProfile?.id ?: ""),
            "patientName" to (selectedProfile?.fullName ?: ""),
            "patientPhone" to (selectedProfile?.phone ?: ""),
            "patientDob" to (selectedProfile?.dob ?: ""),
            "doctorId" to doctor.Id,
            "doctorName" to doctor.Name,
            "specialty" to doctor.Special,
            "serviceName" to (selectedService ?: ""),
            "date" to (selectedDate ?: ""),
            "time" to (selectedTime ?: ""),
            "totalAmount" to finalTotal,
            "status" to status,
            "paymentMethod" to if (selectedPaymentMethod == 0) "Chuyển khoản QR" else "Tiền mặt",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("Appointments")
            .add(appointmentData)
            .addOnSuccessListener { docRef ->
                generatedTicketId = docRef.id
                val notifData = hashMapOf(
                    "userId" to uid,
                    "title" to "Đặt lịch thành công!",
                    "message" to "Lịch khám ${doctor.Special} với ${doctor.Name} lúc ${selectedTime} ngày ${selectedDate} đã được ghi nhận.",
                    "type" to "success",
                    "time" to "Vừa xong",
                    "isRead" to false
                )
                db.collection("Notifications").add(notifData)
                isSavingBooking = false
                finalTicketStatus = status
                currentStep = 5
            }
            .addOnFailureListener {
                isSavingBooking = false
                Toast.makeText(context, "Lỗi kết nối Firebase", Toast.LENGTH_SHORT).show()
            }
    }

    if (showPaymentGateway) {
        PaymentGatewayScreen(
            amount = finalTotal,
            bookingId = tempBookingId,
            onSuccess = {
                showPaymentGateway = false
                saveBookingToFirebase("Đã thanh toán")
            },
            onCancel = { showPaymentGateway = false }
        )
    }

    Scaffold(
        topBar = {
            if (currentStep < 5) {
                Column(modifier = Modifier.background(BluePrimary)) {
                    CenterAlignedTopAppBar(
                        title = { Text(getStepTitle(currentStep), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = { if (currentStep > 1) currentStep-- else onBack() }) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    StepperRow(currentStep = currentStep)
                }
            }
        },
        bottomBar = {
            if (currentStep < 5) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {

                    // 🔴 3. KIỂM TRA NẾU BÁC SĨ OFFLINE -> KHÓA NÚT NGAY LẬP TỨC TRONG QUÁ TRÌNH BOOKING
                    if (doctorRealtimeStatus == "offline") {
                        Button(
                            onClick = { Toast.makeText(context, "Xin lỗi, bác sĩ vừa báo bận đột xuất và không nhận thêm lịch hôm nay.", Toast.LENGTH_LONG).show() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Bác sĩ đang bận (Không nhận lịch)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        // NẾU ONLINE -> HIỂN THỊ NÚT TIẾP TỤC / XÁC NHẬN BÌNH THƯỜNG
                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> if (selectedService != null && selectedDate != null && selectedTime != null) currentStep++
                                    2 -> if (selectedProfile != null) currentStep++
                                    3 -> currentStep++
                                    4 -> {
                                        if (selectedPaymentMethod == 0) {
                                            tempBookingId = "BK" + System.currentTimeMillis().toString().takeLast(6)
                                            showPaymentGateway = true
                                        } else {
                                            saveBookingToFirebase("Chưa thanh toán")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSavingBooking
                        ) {
                            if (isSavingBooking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text(if (currentStep == 4) "Xác nhận đặt lịch" else "Tiếp Tục", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(BgGray).padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

                if (currentStep < 3 && currentStep != 2) {
                    DoctorHeaderCard(doctor)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (currentStep) {
                    1 -> {
                        SectionTitle("Dịch vụ")
                        ServiceSelectionCard(
                            selected = selectedService,
                            onSelect = { s, p -> selectedService = s; servicePrice = p; selectedDate = null; selectedTime = null }
                        )

                        AnimatedVisibility(visible = selectedService != null) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                SectionTitle("Ngày khám")
                                DatePickerField(selectedDate = selectedDate) { datePickerDialog.show() }

                                Spacer(modifier = Modifier.height(16.dp))
                                AnimatedVisibility(visible = selectedDate != null) {
                                    Column {
                                        if (isFetchingTimes) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = BluePrimary)
                                            }
                                        } else {
                                            if (morningSlots.isNotEmpty()) {
                                                SectionTitle("Giờ khám (Buổi Sáng)")
                                                TimeSelectionGrid(morningSlots, selectedTime, disabledTimes) { selectedTime = it }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            if (afternoonSlots.isNotEmpty()) {
                                                SectionTitle("Giờ khám (Buổi Chiều)")
                                                TimeSelectionGrid(afternoonSlots, selectedTime, disabledTimes) { selectedTime = it }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("Danh sách hồ sơ (${patientProfiles.size}/10)", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        if (isFetchingPatient) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BluePrimary) }
                        } else {
                            patientProfiles.forEach { profile ->
                                PatientProfileCardUI(profile = profile, isSelected = selectedProfile?.id == profile.id) { selectedProfile = profile }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { context.startActivity(Intent(context, NewPatientRegistrationActivity::class.java)) },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Chưa từng khám đăng ký mới", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        ConfirmInfoCard(doctor, selectedProfile?.fullName ?: "", selectedService ?: "", selectedDate ?: "", selectedTime ?: "", servicePrice)
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("Dịch vụ giúp việc cá nhân (Tuỳ chọn)")
                        AddonSelectionCard("Không cần", 0, addonService == "Không") { addonService = "Không"; addonPrice = 0 }
                        AddonSelectionCard("Giúp việc cá nhân (3 giờ)", 399000, addonService == "Cơ bản") { addonService = "Cơ bản"; addonPrice = 399000 }
                        AddonSelectionCard("Giúp việc Tiếng Anh (3 giờ)", 700000, addonService == "Tiếng Anh") { addonService = "Tiếng Anh"; addonPrice = 700000 }
                        Spacer(modifier = Modifier.height(24.dp))
                        SubTotalCard(servicePrice, addonPrice)
                    }
                    4 -> {
                        ConfirmInfoCard(doctor, selectedProfile?.fullName ?: "", selectedService ?: "", selectedDate ?: "", selectedTime ?: "", servicePrice)
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("Phương thức thanh toán")

                        PaymentMethodSelector(
                            selectedMethod = selectedPaymentMethod,
                            onMethodSelected = { selectedPaymentMethod = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        DetailedBillCard(servicePrice, addonPrice, platformFee, discount)
                    }
                    5 -> {
                        TicketScreen(
                            doctor = doctor,
                            patient = selectedProfile?.fullName ?: "",
                            service = selectedService ?: "",
                            date = selectedDate ?: "",
                            time = selectedTime ?: "",
                            total = finalTotal,
                            ticketId = generatedTicketId,
                            status = finalTicketStatus,
                            onHome = onBack
                        )
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ====================================================================
// ================= CỔNG THANH TOÁN QR CODE GIẢ LẬP ==================
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayScreen(
    amount: Int,
    bookingId: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(600) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft == 0) onCancel()
    }

    val minutes = String.format("%02d", timeLeft / 60)
    val seconds = String.format("%02d", timeLeft % 60)

    val qrUrl = "https://img.vietqr.io/image/970422-0123456789-compact2.png?amount=$amount&addInfo=$bookingId&accountName=NGUYEN NGOC SON"

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = BgGray) {
            Column(modifier = Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    title = { Text("Cổng thanh toán an toàn", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Đóng") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Thời gian thanh toán còn lại", color = Color.Gray, fontSize = 14.sp)
                            Text("$minutes:$seconds", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Red, modifier = Modifier.padding(top = 8.dp))

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Quét mã QR để thanh toán", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            AsyncImage(
                                model = qrUrl,
                                contentDescription = "QR Code",
                                modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            InfoRow(label = "Số tiền:", value = "%,d VNĐ".format(amount), isBold = true, color = BluePrimary)
                            InfoRow(label = "Nội dung CK:", value = bookingId)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Hướng dẫn: Mở ứng dụng ngân hàng hoặc ví điện tử (MoMo, ZaloPay, VNPay...) và chọn Quét Mã QR.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onSuccess,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Giả lập: Đã chuyển khoản xong", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color, fontSize = 14.sp)
    }
}

// ====================================================================
// ================= CÁC COMPONENT GIAO DIỆN HỖ TRỢ ===================
// ====================================================================

fun generateTimeSlots(scheduleStr: String?): Pair<List<String>, List<String>> {
    val defaultSchedule = scheduleStr ?: "08:00 - 17:00"

    val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
    val matches = timeRegex.findAll(defaultSchedule).toList()

    var startHour = 8; var startMin = 0
    var endHour = 17; var endMin = 0

    if (matches.size >= 2) {
        startHour = matches[0].groupValues[1].toInt()
        startMin = matches[0].groupValues[2].toInt()
        endHour = matches[1].groupValues[1].toInt()
        endMin = matches[1].groupValues[2].toInt()
    }

    val morningSlots = mutableListOf<String>()
    val afternoonSlots = mutableListOf<String>()

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, startHour)
        set(Calendar.MINUTE, startMin)
    }

    val endCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, endHour)
        set(Calendar.MINUTE, endMin)
    }

    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    while (cal.before(endCal)) {
        val startStr = formatter.format(cal.time)
        cal.add(Calendar.MINUTE, 30)
        val endStr = formatter.format(cal.time)

        if (cal.after(endCal)) break

        val slot = "$startStr - $endStr"
        val startHourOfSlot = startStr.substring(0, 2).toInt()

        if (startHourOfSlot < 12) {
            morningSlots.add(slot)
        } else if (startHourOfSlot >= 13) {
            afternoonSlots.add(slot)
        }
    }
    return Pair(morningSlots, afternoonSlots)
}

@Composable
fun TimeSelectionGrid(times: List<String>, selectedTime: String?, disabledTimes: List<String>, onSelect: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.heightIn(max = 250.dp),
        userScrollEnabled = false
    ) {
        items(times) { time ->
            val isDisabled = disabledTimes.contains(time)
            val isSelected = selectedTime == time

            val bgColor = when {
                isDisabled -> Color(0xFFEEEEEE)
                isSelected -> BluePrimary
                else -> Color.White
            }
            val textColor = when {
                isDisabled -> Color.Gray
                isSelected -> Color.White
                else -> Color.Black
            }

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, if (!isDisabled && !isSelected) Color.LightGray else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable(enabled = !isDisabled) { onSelect(time) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = time,
                    color = textColor,
                    fontSize = 14.sp,
                    textDecoration = if(isDisabled) TextDecoration.LineThrough else null
                )
            }
        }
    }
}

fun getStepTitle(step: Int): String = when (step) {
    1 -> "Chọn thông tin khám"
    2 -> "Hồ sơ bệnh nhân"
    3 -> "Xác nhận thông tin"
    4 -> "Thông tin thanh toán"
    else -> "Phiếu khám bệnh"
}

@Composable
fun StepperRow(currentStep: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        StepCircle(Icons.Default.MedicalServices, isActive = currentStep >= 1)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = Color.White.copy(0.5f))
        StepCircle(Icons.Default.Person, isActive = currentStep >= 2)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = Color.White.copy(0.5f))
        StepCircle(Icons.Default.CheckCircle, isActive = currentStep >= 3)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = Color.White.copy(0.5f))
        StepCircle(Icons.Default.AccountBalanceWallet, isActive = currentStep >= 4)
    }
}

@Composable
fun StepCircle(icon: ImageVector, isActive: Boolean) {
    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(if (isActive) Color.White else Color.White.copy(0.3f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (isActive) BluePrimary else Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun DoctorHeaderCard(doctor: DoctorsModel) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bác sĩ: ${doctor.Name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Chuyên khoa: ${doctor.Special}", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun ServiceSelectionCard(selected: String?, onSelect: (String, Int) -> Unit) {
    Column {
        SelectableRow("Khám dịch vụ", 150000, selected == "Khám dịch vụ") { onSelect("Khám dịch vụ", 150000) }
        Spacer(modifier = Modifier.height(8.dp))
        SelectableRow("Tư vấn Online (Qua Video)", 100000, selected == "Tư vấn Online") { onSelect("Tư vấn Online", 100000) }
    }
}

@Composable
fun SelectableRow(title: String, price: Int, isSelected: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }.border(1.dp, if (isSelected) BluePrimary else Color.Transparent, RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) BluePrimary else Color.Black)
                Text("%,d đ".format(price), color = Color.Gray, fontSize = 14.sp)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = BluePrimary)
        }
    }
}

@Composable
fun DatePickerField(selectedDate: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.border(1.dp, BluePrimary, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = BluePrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = selectedDate ?: "Bấm để chọn ngày khám",
                    color = if (selectedDate != null) Color.Black else Color.Gray,
                    fontWeight = if (selectedDate != null) FontWeight.Bold else FontWeight.Normal
                )
            }
            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = BluePrimary)
        }
    }
}

@Composable
fun PatientProfileCardUI(profile: PatientProfileModel, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.border(width = 1.dp, color = if (isSelected) BluePrimary else Color.Transparent, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.fullName.uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Ngày sinh: ${profile.dob}", color = Color.Gray, fontSize = 13.sp)
                Text(text = "SĐT: ${profile.phone}", color = Color.Gray, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun AddonSelectionCard(title: String, price: Int, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = BluePrimary))
        Text(title, modifier = Modifier.weight(1f))
        if (price > 0) Text("%,d đ".format(price), fontWeight = FontWeight.Bold, color = BluePrimary)
    }
}

@Composable
fun ConfirmInfoCard(doctor: DoctorsModel, patient: String, service: String, date: String, time: String, price: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Thông tin bệnh nhân", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(patient.split(" - ")[0], fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Thông tin đặt khám", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MedicalServices, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(doctor.Name)
            }
            Text("Dịch vụ: $service", modifier = Modifier.padding(start = 26.dp, top = 4.dp))
            Text("Thời gian: $date ($time)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 26.dp, top = 4.dp))
            Text("Giá: %,d đ".format(price), color = BluePrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 26.dp, top = 4.dp))
        }
    }
}

@Composable
fun SubTotalCard(servicePrice: Int, addonPrice: Int) {
    val total = servicePrice + addonPrice
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tiền khám", color = Color.Gray, fontSize = 15.sp)
            Text("%,d VNĐ".format(servicePrice), fontSize = 15.sp)
        }
        if (addonPrice > 0) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dịch vụ đặt thêm", color = Color.Gray, fontSize = 15.sp)
                Text("%,d VNĐ".format(addonPrice), fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tạm tính", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("%,d VNĐ".format(total), fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 16.sp)
        }
    }
}

@Composable
fun PaymentMethodSelector(selectedMethod: Int, onMethodSelected: (Int) -> Unit) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMethodSelected(0) }
                .border(1.dp, if (selectedMethod == 0) BluePrimary else Color.Transparent, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = BluePrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("Chuyển khoản QR (Mô phỏng VietQR)", fontWeight = FontWeight.Medium)
                }
                RadioButton(selected = selectedMethod == 0, onClick = { onMethodSelected(0) }, colors = RadioButtonDefaults.colors(selectedColor = BluePrimary))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMethodSelected(1) }
                .border(1.dp, if (selectedMethod == 1) BluePrimary else Color.Transparent, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = BluePrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("Thanh toán trực tiếp tại quầy y tế", fontWeight = FontWeight.Medium)
                }
                RadioButton(selected = selectedMethod == 1, onClick = { onMethodSelected(1) }, colors = RadioButtonDefaults.colors(selectedColor = BluePrimary))
            }
        }
    }
}

@Composable
fun DetailedBillCard(servicePrice: Int, addonPrice: Int, platformFee: Int, discount: Int) {
    val total = servicePrice + addonPrice + platformFee - discount
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            BillRow("Tiền khám", servicePrice)
            if (addonPrice > 0) BillRow("Dịch vụ đặt thêm", addonPrice)
            BillRow("Phí nền tảng + Phí TGTT", platformFee)
            Text("Giảm %,dđ, ưu đãi từ App".format(discount), color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng tiền", fontWeight = FontWeight.Bold)
                Text("%,dđ".format(total), fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun BillRow(title: String, price: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Color.Gray, fontSize = 14.sp)
        Text("%,dđ".format(price), fontSize = 14.sp)
    }
}

@Composable
fun TicketScreen(doctor: DoctorsModel, patient: String, service: String, date: String, time: String, total: Int, ticketId: String, status: String, onHome: () -> Unit) {
    val badgeColor = when (status) {
        "Đã thanh toán" -> Color(0xFF4CAF50)
        "Chưa thanh toán" -> Color(0xFFFF9800)
        "Đã khám" -> Color(0xFF2196F3)
        "Đã hủy" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PHIẾU KHÁM BỆNH", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BluePrimary)
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                Text("|||| | |||||| || | ||||", fontSize = 32.sp, letterSpacing = 4.sp)
            }
            Text("Mã phiếu: $ticketId", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(badgeColor).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(status, color = Color.White, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            ConfirmInfoCard(doctor, patient, service, date, time, total)
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                Text("Về trang chủ")
            }
        }
    }
}