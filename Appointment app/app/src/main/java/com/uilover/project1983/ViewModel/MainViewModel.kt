package com.uilover.project1983.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.uilover.project1983.Domain.CategoryModel
import com.uilover.project1983.Domain.DoctorsModel

class MainViewModel : ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val _category = MutableLiveData<MutableList<CategoryModel>>()
    private val _doctors = MutableLiveData<MutableList<DoctorsModel>>()

    val category: LiveData<MutableList<CategoryModel>> = _category
    val doctors: LiveData<MutableList<DoctorsModel>> = _doctors

    fun loadCategory() {
        val ref = firebaseDatabase.getReference("Category")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<CategoryModel>()
                for (childSnapshot in snapshot.children) {
                    val list = childSnapshot.getValue(CategoryModel::class.java)
                    if (list != null) {
                        lists.add(list)
                    }
                }
                _category.value = lists
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi bị hủy lệnh kết nối
            }
        })
    }

    fun loadDoctors() {
        val ref = firebaseDatabase.getReference("Doctors")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<DoctorsModel>()
                for (childSnapshot in snapshot.children) {
                    val list = childSnapshot.getValue(DoctorsModel::class.java)
                    if (list != null) {
                        // 🔴 GÁN UID CỦA BÁC SĨ TỪ TÊN NHÁNH (KEY) TRÊN FIREBASE
                        list.docUid = childSnapshot.key ?: ""

                        lists.add(list)
                    }
                }
                _doctors.value = lists
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi bị hủy lệnh kết nối
            }
        })
    }
}