package com.uilover.project1983.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// Định nghĩa bộ màu Medical Blue hiện đại
val MedicalPrimary = Color(0xFF1E88E5)
val MedicalLight = Color(0xFFE3F2FD)
val MedicalGradientStart = Color(0xFF42A5F5)
val TextGray = Color(0xFF757575)
val SocialGoogleRed = Color(0xFFDB4437)
val SocialFbBlue = Color(0xFF4267B2)

class LoginActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MedicalLight
                ) {
                    LoginScreen(
                        onNavigateToRegister = {
                            startActivity(Intent(this, RegisterActivity::class.java))
                        },
                        onAuthSuccess = { uid ->
                            checkRoleAndNavigate(uid)
                        },
                        // 🔴 BỔ SUNG: Xử lý sự kiện đăng nhập dưới tư cách Khách
                        onGuestLogin = {
                            Toast.makeText(this, "Đang vào chế độ Khách", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        auth = auth
                    )
                }
            }
        }
    }

    private fun checkRoleAndNavigate(uid: String) {
        realtimeDb.getReference("Users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val role = snapshot.child("role").getValue(String::class.java)?.uppercase() ?: "PATIENT"

                    when (role) {
                        "DOCTOR" -> {
                            Toast.makeText(this, "Xin chào Bác sĩ", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, com.uilover.project1983.Doctor.DoctorMainActivity::class.java))
                        }
                        "ADMIN" -> {
                            Toast.makeText(this, "Xin chào Admin", Toast.LENGTH_SHORT).show()
                            // TODO: Chuyển sang AdminPanelActivity
                        }
                        else -> {
                            Toast.makeText(this, "Xin chào Bệnh nhân", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                    }
                    finish()
                } else {
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onAuthSuccess: (String) -> Unit,
    onGuestLogin: () -> Unit, // 🔴 BỔ SUNG CALLBACK NÀY
    auth: FirebaseAuth
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. HEADER GRADIENT BACKGROUND
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MedicalGradientStart, MedicalPrimary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-30).dp)
            ) {
                Text("🏥", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "MediGo",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Chăm sóc sức khỏe 4.0",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // 2. MAIN LOGIN CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center)
                .offset(y = 40.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Chào Mừng Trở Lại",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Đăng nhập để tiếp tục",
                    fontSize = 14.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Ô nhập Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MedicalPrimary) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedicalPrimary, focusedLabelColor = MedicalPrimary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Ô nhập Mật khẩu
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MedicalPrimary) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = null, tint = TextGray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedicalPrimary, focusedLabelColor = MedicalPrimary)
                )

                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMessage, color = Color.Red, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Nút Đăng nhập
                Button(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            errorMessage = ""
                            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onAuthSuccess(auth.currentUser?.uid ?: "")
                                    } else {
                                        isLoading = false
                                        errorMessage = "Tài khoản hoặc mật khẩu không chính xác!"
                                    }
                                }
                        } else {
                            errorMessage = "Vui lòng nhập đầy đủ Email và Mật khẩu!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                    } else {
                        Text("Đăng nhập", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. SOCIAL LOGIN SECTION
                SocialLoginSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Chuyển sang đăng ký
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chưa có tài khoản?", color = TextGray, fontSize = 14.sp)
                    Text(
                        " Đăng ký ngay",
                        color = MedicalPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔴 BỔ SUNG: NÚT VÀO ỨNG DỤNG BẰNG CHẾ ĐỘ KHÁCH
                TextButton(onClick = { onGuestLogin() }) {
                    Text(
                        "Tiếp tục với tư cách Khách",
                        color = TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Composable
fun SocialLoginSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text(
                " Hoặc đăng nhập bằng ",
                color = TextGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SocialButton(
                iconText = "G",
                color = SocialGoogleRed,
                onClick = { /* Handle Google Login Logic */ }
            )

            Spacer(modifier = Modifier.width(20.dp))

            SocialButton(
                iconText = "f",
                color = SocialFbBlue,
                onClick = { /* Handle Facebook Login Logic */ }
            )
        }
    }
}

@Composable
fun SocialButton(iconText: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(56.dp).clickable { onClick() },
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = iconText,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}