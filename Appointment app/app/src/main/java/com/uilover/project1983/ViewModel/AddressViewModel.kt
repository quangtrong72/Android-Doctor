package com.uilover.project1983.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uilover.project1983.Domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddressViewModel : ViewModel() {
    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces: StateFlow<List<Province>> = _provinces

    private val _districts = MutableStateFlow<List<District>>(emptyList())
    val districts: StateFlow<List<District>> = _districts

    private val _wards = MutableStateFlow<List<Ward>>(emptyList())
    val wards: StateFlow<List<Ward>> = _wards

    init {
        fetchProvinces()
    }

    private fun fetchProvinces() {
        viewModelScope.launch {
            try {
                _provinces.value = RetrofitInstance.api.getProvinces()
            } catch (e: Exception) {
                e.printStackTrace()
                // Dữ liệu dự phòng (Fallback Data) nếu máy không có mạng
                _provinces.value = listOf(
                    Province(1, "Thành phố Hà Nội"),
                    Province(48, "Thành phố Đà Nẵng"),
                    Province(79, "Thành phố Hồ Chí Minh")
                )
            }
        }
    }

    fun fetchDistricts(provinceCode: Int) {
        viewModelScope.launch {
            try {
                val detail = RetrofitInstance.api.getProvinceDetail(provinceCode)
                _districts.value = detail.districts
                _wards.value = emptyList() // Làm sạch (Reset) phường xã khi đổi tỉnh
            } catch (e: Exception) {
                e.printStackTrace()
                // Dữ liệu dự phòng (Fallback Data)
                _wards.value = emptyList()
                when(provinceCode) {
                    1 -> _districts.value = listOf(District(1, "Quận Ba Đình"), District(5, "Quận Cầu Giấy"))
                    48 -> _districts.value = listOf(District(492, "Quận Hải Châu"), District(495, "Quận Liên Chiểu"))
                    79 -> _districts.value = listOf(District(760, "Quận 1"), District(761, "Quận 12"))
                }
            }
        }
    }

    fun fetchWards(districtCode: Int) {
        viewModelScope.launch {
            try {
                val detail = RetrofitInstance.api.getDistrictDetail(districtCode)
                _wards.value = detail.wards
            } catch (e: Exception) {
                e.printStackTrace()
                // Dữ liệu dự phòng (Fallback Data)
                _wards.value = listOf(Ward(1, "Phường 1"), Ward(2, "Phường 2"), Ward(3, "Phường 3"))
            }
        }
    }
}