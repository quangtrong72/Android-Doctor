package com.uilover.project1983.Domain

data class UserModel(
    val email: String = "",
    val role: String = "PATIENT", // PATIENT, DOCTOR, hoặc ADMIN
    val name: String = "",
    val doctorId: Int? = null
)