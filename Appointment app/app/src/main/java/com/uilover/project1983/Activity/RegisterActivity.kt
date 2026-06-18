package com.uilover.project1983.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.uilover.project1983.Domain.DoctorsModel
import com.uilover.project1983.Domain.UserModel
import kotlin.random.Random

class RegisterActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RegisterTheme {
                    ComplexRegisterScreen(
                        onRegisterSuccess = {
                            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        onNavigateToLogin = {
                            finish()
                        },
                        auth = auth,
                        realtimeDb = realtimeDb
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterTheme(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF0F4F8)) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    auth: FirebaseAuth,
    realtimeDb: FirebaseDatabase
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val primaryBlue = Color(0xFF1E88E5)

    var currentStep by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }

    // Dữ liệu cơ bản
    var selectedRole by remember { mutableStateOf("PATIENT") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Dữ liệu chuyên môn Bác sĩ
    var bio by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("Tim mạch") }
    var experience by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var clinicalFields by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var workHistory by remember { mutableStateOf("") }
    var certificates by remember { mutableStateOf("") }
    var publications by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 🔵 HEADER GRADIENT ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF42A5F5), primaryBlue)))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tạo Tài Khoản", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Bắt đầu hành trình chăm sóc sức khỏe", color = Color.White.copy(0.8f), fontSize = 14.sp)
            }
        }

        // --- ⚪ MAIN CONTENT CARD ---
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Stepper (Chỉ hiện cho Bác sĩ)
                if (selectedRole == "DOCTOR") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepItem(1, currentStep, "Thông tin")
                        HorizontalDivider(modifier = Modifier.width(40.dp), color = Color.LightGray)
                        StepItem(2, currentStep, "Chuyên môn")
                        HorizontalDivider(modifier = Modifier.width(40.dp), color = Color.LightGray)
                        StepItem(3, currentStep, "Xác nhận")
                    }
                }

                // Avatar Picker
                Box(
                    modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFFF0F4F8))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Form nội dung theo Step
                when (currentStep) {
                    1 -> {
                        // Chọn vai trò
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilterChip(
                                selected = selectedRole == "PATIENT",
                                onClick = { selectedRole = "PATIENT"; currentStep = 1 },
                                label = { Text("Bệnh nhân") },
                                leadingIcon = if (selectedRole == "PATIENT") { { Icon(Icons.Default.Check, null) } } else null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            FilterChip(
                                selected = selectedRole == "DOCTOR",
                                onClick = { selectedRole = "DOCTOR" },
                                label = { Text("Bác sĩ") },
                                leadingIcon = if (selectedRole == "DOCTOR") { { Icon(Icons.Default.Check, null) } } else null
                            )
                        }

                        RegisterTextField(fullName, { fullName = it }, "Họ và tên", Icons.Default.Person)
                        RegisterTextField(email, { email = it }, "Email", Icons.Default.Email)
                        RegisterTextField(password, { password = it }, "Mật khẩu", Icons.Default.Lock, isPassword = true)
                        RegisterTextField(confirmPassword, { confirmPassword = it }, "Xác nhận mật khẩu", Icons.Default.Lock, isPassword = true)
                        RegisterTextField(phone, { phone = it }, "Số điện thoại", Icons.Default.Phone)

                        if (selectedRole == "DOCTOR") {
                            RegisterTextField(address, { address = it }, "Địa chỉ phòng khám", Icons.Default.Home)
                        }
                    }
                    2 -> {
                        // Form chuyên môn Bác sĩ
                        RegisterTextField(bio, { bio = it }, "Tiểu sử / Giới thiệu", Icons.Default.Description, singleLine = false)

                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = specialization, onValueChange = {}, readOnly = true,
                                label = { Text("Chuyên khoa chính") },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, null, tint = primaryBlue) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Tim mạch", "Nha khoa", "Nội khoa", "Nhi khoa", "Da liễu").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = { specialization = it; expanded = false })
                                }
                            }
                        }

                        RegisterTextField(experience, { experience = it }, "Số năm kinh nghiệm", Icons.Default.Timeline)
                        RegisterTextField(clinicalFields, { clinicalFields = it }, "Lĩnh vực lâm sàng sâu", Icons.Default.Add, singleLine = false)
                        RegisterTextField(education, { education = it }, "Quá trình đào tạo", Icons.Default.School, singleLine = false)
                        RegisterTextField(workHistory, { workHistory = it }, "Quá trình công tác", Icons.Default.Work, singleLine = false)
                        RegisterTextField(certificates, { certificates = it }, "Chứng chỉ & Hội nghị", Icons.Default.WorkspacePremium, singleLine = false)
                        RegisterTextField(publications, { publications = it }, "Bài báo khoa học", Icons.Default.MenuBook, singleLine = false)
                        RegisterTextField(website, { website = it }, "Website", Icons.Default.Language)
                    }
                    3 -> {
                        // Màn hình xác nhận
                        ReviewCard(fullName, email, phone, address, specialization, experience)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- HÀNG NÚT BẤM DƯỚI CÙNG ---
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Quay lại") }
                        Spacer(Modifier.width(12.dp))
                    }
                    Button(
                        onClick = {
                            if (selectedRole == "PATIENT" || currentStep == 3) {
                                // Thực hiện đăng ký
                                if (fullName.isNotEmpty() && email.isNotEmpty() && password == confirmPassword) {
                                    isLoading = true
                                    auth.createUserWithEmailAndPassword(email.trim(), password.trim()).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val uid = auth.currentUser?.uid ?: ""
                                            realtimeDb.getReference("Users").child(uid).setValue(UserModel(email, selectedRole, fullName))

                                            if (selectedRole == "DOCTOR") {
                                                val lat = String.format("%.4f", 15.0 + Random.nextDouble(2.0))
                                                val lng = String.format("%.4f", 108.0 + Random.nextDouble(2.0))

                                                // Khởi tạo model đầy đủ
                                                val doc = DoctorsModel(
                                                    Address = address,
                                                    Biography = bio,
                                                    Id = (100..999).random(),
                                                    Name = fullName,
                                                    Picture = "https://img.freepik.com/free-photo/doctor-with-his-arms-crossed-white-background_1368-5790.jpg",
                                                    Special = specialization,
                                                    Experience = experience.toIntOrNull() ?: 0,
                                                    Location = "geo:$lat,$lng",
                                                    Mobile = phone,
                                                    Patiens = "0+",
                                                    Rating = 5.0,
                                                    Site = website.ifEmpty { "https://google.com" },
                                                    ClinicalFields = clinicalFields,
                                                    Education = education,
                                                    WorkHistory = workHistory,
                                                    Certificates = certificates,
                                                    Publications = publications
                                                )

                                                realtimeDb.getReference("Doctors").child(uid).setValue(doc).addOnSuccessListener {
                                                    isLoading = false; onRegisterSuccess()
                                                }
                                            } else {
                                                isLoading = false; onRegisterSuccess()
                                            }
                                        } else {
                                            isLoading = false; Toast.makeText(context, task.exception?.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Kiểm tra lại thông tin & mật khẩu!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Sang bước tiếp theo
                                if (fullName.isNotEmpty() && email.isNotEmpty() && password == confirmPassword) {
                                    currentStep++
                                } else {
                                    Toast.makeText(context, "Vui lòng nhập đủ thông tin cơ bản!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text(if (selectedRole == "PATIENT" || currentStep == 3) "Đăng ký" else "Tiếp tục")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateToLogin) {
                    Text("Đã có tài khoản? Đăng nhập ngay", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF1E88E5)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1E88E5), focusedLabelColor = Color(0xFF1E88E5))
    )
}

@Composable
fun StepItem(step: Int, currentStep: Int, title: String) {
    val isActive = step <= currentStep
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(if (isActive) Color(0xFF1E88E5) else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(step.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, fontSize = 10.sp, color = if (isActive) Color(0xFF1E88E5) else Color.Gray)
    }
}

@Composable
fun ReviewCard(name: String, email: String, phone: String, address: String, spec: String, exp: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Xác nhận thông tin", fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5), fontSize = 18.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ReviewItem("Họ tên", name)
            ReviewItem("Email", email)
            ReviewItem("Số điện thoại", phone)
            ReviewItem("Địa chỉ", address)
            ReviewItem("Chuyên khoa", spec)
            ReviewItem("Kinh nghiệm", "$exp năm")
        }
    }
}

@Composable
fun ReviewItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
        Text(value, color = Color.DarkGray)
    }
}