package com.uilover.project1983.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DoctorProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DoctorProfileScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF1E88E5)
    val bgGray = Color(0xFFF0F4F8)
    val scrollState = rememberScrollState()

    // Khởi tạo các biến State cho giao diện
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("Tim mạch") }
    var experience by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var clinicalFields by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var workHistory by remember { mutableStateOf("") }
    var certificates by remember { mutableStateOf("") }
    var publications by remember { mutableStateOf("") }
    var pictureUrl by remember { mutableStateOf("") }

    // Gọi dữ liệu từ Firebase khi mở màn hình
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference("Doctors").child(uid)
            dbRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    fullName = snapshot.child("Name").value?.toString() ?: ""
                    phone = snapshot.child("Mobile").value?.toString() ?: ""
                    address = snapshot.child("Address").value?.toString() ?: ""
                    bio = snapshot.child("Biography").value?.toString() ?: ""
                    specialization = snapshot.child("Special").value?.toString() ?: "Tim mạch"
                    experience = snapshot.child("Experience").value?.toString() ?: "0"
                    website = snapshot.child("Site").value?.toString() ?: ""
                    clinicalFields = snapshot.child("ClinicalFields").value?.toString() ?: ""
                    education = snapshot.child("Education").value?.toString() ?: ""
                    workHistory = snapshot.child("WorkHistory").value?.toString() ?: ""
                    certificates = snapshot.child("Certificates").value?.toString() ?: ""
                    publications = snapshot.child("Publications").value?.toString() ?: ""
                    pictureUrl = snapshot.child("Picture").value?.toString() ?: ""
                }
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hồ sơ Bác sĩ", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = primaryBlue)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = {
                        isSaving = true
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            // Dùng Map để update cụ thể các trường, không làm mất Id hay Rating
                            val updates = mapOf(
                                "Name" to fullName.trim(),
                                "Mobile" to phone.trim(),
                                "Address" to address.trim(),
                                "Biography" to bio.trim(),
                                "Special" to specialization,
                                "Experience" to (experience.toIntOrNull() ?: 0),
                                "Site" to website.trim(),
                                "ClinicalFields" to clinicalFields.trim(),
                                "Education" to education.trim(),
                                "WorkHistory" to workHistory.trim(),
                                "Certificates" to certificates.trim(),
                                "Publications" to publications.trim()
                            )

                            FirebaseDatabase.getInstance().getReference("Doctors").child(uid)
                                .updateChildren(updates)
                                .addOnSuccessListener {
                                    // Cập nhật luôn tên bên bảng Users cho đồng bộ
                                    FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                        .child("name").setValue(fullName.trim())

                                    isSaving = false
                                    Toast.makeText(context, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                    Toast.makeText(context, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Lưu Thay Đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(bgGray).padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = primaryBlue)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- AVATAR ---
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pictureUrl.isNotEmpty()) {
                            AsyncImage(model = pictureUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(50.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- CARD 1: THÔNG TIN CÁ NHÂN ---
                    ProfileSectionCard("Thông tin cá nhân") {
                        ProfileTextField(fullName, { fullName = it }, "Họ và tên", Icons.Default.Person)
                        ProfileTextField(phone, { phone = it }, "Số điện thoại", Icons.Default.Phone)
                        ProfileTextField(address, { address = it }, "Địa chỉ phòng khám", Icons.Default.Home)
                    }

                    // --- CARD 2: CHUYÊN MÔN ---
                    ProfileSectionCard("Chuyên môn") {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = specialization, onValueChange = {}, readOnly = true,
                                label = { Text("Chuyên khoa chính") },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, null, tint = primaryBlue) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryBlue, focusedLabelColor = primaryBlue)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Tim mạch", "Nha khoa", "Nội khoa", "Nhi khoa", "Da liễu").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = { specialization = it; expanded = false })
                                }
                            }
                        }

                        ProfileTextField(experience, { experience = it }, "Số năm kinh nghiệm", Icons.Default.Timeline)
                        ProfileTextField(bio, { bio = it }, "Tiểu sử / Giới thiệu", Icons.Default.Description, singleLine = false)
                        ProfileTextField(clinicalFields, { clinicalFields = it }, "Lĩnh vực lâm sàng sâu", Icons.Default.Add, singleLine = false)
                    }

                    // --- CARD 3: HỌC VẤN & CÔNG TÁC ---
                    ProfileSectionCard("Học vấn & Bằng cấp") {
                        ProfileTextField(education, { education = it }, "Quá trình đào tạo", Icons.Default.School, singleLine = false)
                        ProfileTextField(workHistory, { workHistory = it }, "Quá trình công tác", Icons.Default.Work, singleLine = false)
                        ProfileTextField(certificates, { certificates = it }, "Chứng chỉ & Hội nghị", Icons.Default.WorkspacePremium, singleLine = false)
                        ProfileTextField(publications, { publications = it }, "Bài báo khoa học", Icons.Default.MenuBook, singleLine = false)
                        ProfileTextField(website, { website = it }, "Website (Nếu có)", Icons.Default.Language)
                    }

                    Spacer(modifier = Modifier.height(80.dp)) // Tránh bị BottomBar che khuất
                }
            }
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN HỖ TRỢ ---

@Composable
fun ProfileSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E88E5), modifier = Modifier.padding(bottom = 16.dp))
            content()
        }
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 5,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1E88E5),
            focusedLabelColor = Color(0xFF1E88E5),
            unfocusedBorderColor = Color.LightGray
        )
    )
}