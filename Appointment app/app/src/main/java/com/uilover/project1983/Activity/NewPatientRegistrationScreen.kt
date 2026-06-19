package com.uilover.project1983.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uilover.project1983.ViewModel.AddressViewModel
import java.text.SimpleDateFormat
import java.util.*

// Activity class (Lớp Activity) để hệ thống nhận diện và điều hướng trang
class NewPatientRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Gọi giao diện form và xử lý nút quay lại
                NewPatientRegistrationScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

// Screen UI (Giao diện màn hình) và Logic xử lý Firestore
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPatientRegistrationScreen(
    onBackClick: () -> Unit,
    addressViewModel: AddressViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val PrimaryBlue = Color(0xFF1E88E5)

    // --- State (Trạng thái) dữ liệu chung ---
    var fullName by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var bhyt by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Việt Nam") }
    var ethnicity by remember { mutableStateOf("Kinh") }

    // --- State (Trạng thái) & Logic Ngày Sinh ---
    var dob by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // --- State (Trạng thái) dữ liệu Địa chỉ ---
    var province by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }

    // --- State (Trạng thái) xử lý logic Firebase ---
    var isLoading by remember { mutableStateOf(false) }

    // --- Fetch data (Lấy dữ liệu) từ API thông qua ViewModel ---
    val provincesList by addressViewModel.provinces.collectAsState()
    val districtsList by addressViewModel.districts.collectAsState()
    val wardsList by addressViewModel.wards.collectAsState()

    val genders = listOf("Nam", "Nữ", "Khác")
    val countries = listOf("Việt Nam", "Mỹ", "Anh", "Pháp", "Nhật Bản", "Hàn Quốc")
    val ethnicities = listOf("Kinh", "Tày", "Thái", "Mường", "Khmer", "Hoa")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- HEADER (Thanh tiêu đề) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White,
                modifier = Modifier.size(28.dp).clickable { onBackClick() }
            )
            Text(
                text = "Thêm hồ sơ mới",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(28.dp))
        }

        // --- FORM CONTENT (Nội dung biểu mẫu) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Text(
                text = "THÔNG TIN CHUNG",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên (có dấu)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = dob, onValueChange = {}, readOnly = true,
                        label = { Text("Ngày sinh") },
                        trailingIcon = { Icon(Icons.Default.DateRange, null, tint = PrimaryBlue) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
                Box(modifier = Modifier.weight(1f)) {
                    DropdownSelector(label = "Giới tính", selectedValue = gender, options = genders, onValueChange = { gender = it })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = cccd, onValueChange = { cccd = it }, label = { Text("Mã định danh/CCCD") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = bhyt, onValueChange = { bhyt = it }, label = { Text("Mã Bảo hiểm Y tế") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = job, onValueChange = { job = it }, label = { Text("Nghề nghiệp") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (nhận phiếu khám)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) { DropdownSelector("Quốc gia", country, countries, { country = it }) }
                Box(modifier = Modifier.weight(1f)) { DropdownSelector("Dân tộc", ethnicity, ethnicities, { ethnicity = it }) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ĐỊA CHỈ TRÊN CCCD",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val provinceNames = provincesList.map { it.name }
            DropdownSelector("Tỉnh/Thành phố", province, provinceNames, { selectedName ->
                province = selectedName
                district = ""; ward = ""
                val selectedId = provincesList.find { it.name == selectedName }?.code
                if (selectedId != null) {
                    addressViewModel.fetchDistricts(selectedId)
                }
            })
            Spacer(modifier = Modifier.height(12.dp))

            val districtNames = districtsList.map { it.name }
            DropdownSelector("Quận/Huyện", district, districtNames, { selectedName ->
                district = selectedName
                ward = ""
                val selectedId = districtsList.find { it.name == selectedName }?.code
                if (selectedId != null) {
                    addressViewModel.fetchWards(selectedId)
                }
            })
            Spacer(modifier = Modifier.height(12.dp))

            val wardNames = wardsList.map { it.name }
            DropdownSelector("Phường/Xã", ward, wardNames, { selectedName -> ward = selectedName })
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Số nhà/Tên đường/Ấp thôn xóm") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Spacer(modifier = Modifier.height(40.dp))

            // --- SAVE DATA BUTTON (Nút lưu dữ liệu lên Firestore) ---
            Button(
                onClick = {
                    if (fullName.isBlank() || phone.isBlank() || cccd.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập Tên, Số điện thoại và CCCD!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    // Current User UID (Mã định danh người dùng hiện tại) từ FirebaseAuth
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    val db = FirebaseFirestore.getInstance()
                    val patientData = hashMapOf(
                        "userId" to uid, // Đóng dấu quyền sở hữu riêng biệt cho từng tài khoản
                        "fullName" to fullName,
                        "dob" to dob,
                        "gender" to gender,
                        "cccd" to cccd,
                        "bhyt" to bhyt,
                        "job" to job,
                        "phone" to phone,
                        "email" to email,
                        "country" to country,
                        "ethnicity" to ethnicity,
                        "province" to province,
                        "district" to district,
                        "ward" to ward,
                        "street" to street
                    )

                    // Add document (Thêm tài liệu) vào Collection (Bộ sưu tập) PatientProfiles
                    db.collection("PatientProfiles")
                        .add(patientData)
                        .addOnSuccessListener {
                            isLoading = false
                            Toast.makeText(context, "Tạo hồ sơ thành công!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = "Tạo hồ sơ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        // --- DATE PICKER DIALOG (Hộp thoại chọn ngày sinh) ---
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formatter.timeZone = TimeZone.getTimeZone("UTC")
                            dob = formatter.format(Date(millis))
                        }
                    }) {
                        Text("Chọn", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Hủy", color = Color.Gray) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// Dropdown component (Thành phần menu thả xuống) dùng chung trong biểu mẫu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, selectedValue: String, options: List<String>, onValueChange: (String) -> Unit) {
    val PrimaryBlue = Color(0xFF1E88E5)
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                focusedLabelColor = PrimaryBlue
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Đang tải / Không có dữ liệu...", color = Color.Gray) },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.Black) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}