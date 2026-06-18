package com.uilover.project1983.Domain

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// 1. Mô hình dữ liệu (Data Models) chuẩn của API mới
data class Province(val code: Int, val name: String)
data class District(val code: Int, val name: String)
data class Ward(val code: Int, val name: String)

data class ProvinceDetail(val code: Int, val name: String, val districts: List<District>)
data class DistrictDetail(val code: Int, val name: String, val wards: List<Ward>)

// 2. Dịch vụ (API Service)
interface ApiService {
    @GET("p/")
    suspend fun getProvinces(): List<Province>

    @GET("p/{code}")
    suspend fun getProvinceDetail(@Path("code") code: Int, @Query("depth") depth: Int = 2): ProvinceDetail

    @GET("d/{code}")
    suspend fun getDistrictDetail(@Path("code") code: Int, @Query("depth") depth: Int = 2): DistrictDetail
}

// 3. Khởi tạo (Retrofit Instance)
object RetrofitInstance {
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://provinces.open-api.vn/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}