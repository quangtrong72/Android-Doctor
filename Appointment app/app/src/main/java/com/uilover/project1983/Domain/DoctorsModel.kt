package com.uilover.project1983.Domain

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class DoctorsModel(
    @get:PropertyName("Address") @set:PropertyName("Address") var Address: String = "",
    @get:PropertyName("Biography") @set:PropertyName("Biography") var Biography: String = "",
    @get:PropertyName("Id") @set:PropertyName("Id") var Id: Int = 0,
    @get:PropertyName("Name") @set:PropertyName("Name") var Name: String = "",
    @get:PropertyName("Picture") @set:PropertyName("Picture") var Picture: String = "",
    @get:PropertyName("Special") @set:PropertyName("Special") var Special: String = "",

    // Đã sửa dấu phẩy và đổi tên biến thành Experience cho chuẩn (vẫn map tới Expriense ở Firebase)
    @get:PropertyName("Expriense") @set:PropertyName("Expriense") var Experience: Int = 0,

    @get:PropertyName("Location") @set:PropertyName("Location") var Location: String = "",
    @get:PropertyName("Mobile") @set:PropertyName("Mobile") var Mobile: String = "",
    @get:PropertyName("Patiens") @set:PropertyName("Patiens") var Patiens: String = "",
    @get:PropertyName("Rating") @set:PropertyName("Rating") var Rating: Double = 0.0,
    @get:PropertyName("Site") @set:PropertyName("Site") var Site: String = "",

    // Đã thêm dấu phẩy ngăn cách ở các trường mới
    @get:PropertyName("ClinicalFields") @set:PropertyName("ClinicalFields") var ClinicalFields: String = "",
    @get:PropertyName("Education") @set:PropertyName("Education") var Education: String = "",
    @get:PropertyName("WorkHistory") @set:PropertyName("WorkHistory") var WorkHistory: String = "",
    @get:PropertyName("Certificates") @set:PropertyName("Certificates") var Certificates: String = "",
    @get:PropertyName("Publications") @set:PropertyName("Publications") var Publications: String = "",
    @get:PropertyName("Price") @set:PropertyName("Price") var Price: String = "300.000 VNĐ",
    @get:PropertyName("Schedule") @set:PropertyName("Schedule") var Schedule: String = "Thứ 2 - Thứ 6 (8:00 - 17:00)",

    // 🔴 THÊM MỚI: Trường này dùng để lưu UID dài của Firebase Authentication
    // Không cần gắn PropertyName vì ta sẽ tự gán nó bằng code lúc gọi API
    var docUid: String = ""
) : Serializable