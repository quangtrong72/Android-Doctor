// 1. NHÚNG FIREBASE SDK
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import {
  getFirestore,
  collection,
  getDocs,
  query,
  orderBy,
  doc,
  updateDoc,
} from "https://www.gstatic.com/firebasejs/10.8.1/firebase-firestore.js";
import {
  getDatabase,
  ref,
  get,
  set,
} from "https://www.gstatic.com/firebasejs/10.8.1/firebase-database.js";

// 2. CẤU HÌNH FIREBASE PROJECT
const firebaseConfig = {
  apiKey: "AIzaSyDGpqFykAzdSFDo-UVP7RiBjvPjZHnMiAQ",
  authDomain: "doctorbookingapp-1d1bb.firebaseapp.com",
  databaseURL: "https://doctorbookingapp-1d1bb-default-rtdb.firebaseio.com",
  projectId: "doctorbookingapp-1d1bb",
  storageBucket: "doctorbookingapp-1d1bb.firebasestorage.app",
  messagingSenderId: "353040857970",
  appId: "1:353040857970:web:5dc432c21115a5a6c23beb",
  measurementId: "G-SCPHK2NM6X",
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const rtdb = getDatabase(app);

// ==============================================
// LOGIC CHUNG TRANG INDEX (DASHBOARD)
// ==============================================
async function loadDashboardData() {
  try {
    const appointmentsRef = collection(db, "Appointments");
    const q = query(appointmentsRef, orderBy("createdAt", "desc"));
    const snapshot = await getDocs(q);

    let tongDoanhThu = 0;
    let tongCaKham = snapshot.size;
    let uniqueDoctors = new Set();
    let uniquePatients = new Set();
    let serviceCount = { "Khám dịch vụ": 0, "Tư vấn Online": 0 };
    let bangGiaoDichHTML = "";
    let countTableRows = 0;

    snapshot.forEach((doc) => {
      const data = doc.data();
      if (data.doctorId) uniqueDoctors.add(data.doctorId);
      if (data.patientId) uniquePatients.add(data.patientId);
      if (
        data.status === "Đã thanh toán" ||
        data.status === "Đã khám" ||
        data.status === "Đã xác nhận"
      ) {
        tongDoanhThu += data.totalAmount || 0;
      }
      if (data.serviceName) {
        serviceCount[data.serviceName] =
          (serviceCount[data.serviceName] || 0) + 1;
      }
      if (countTableRows < 10) {
        let badgeColor = "bg-secondary";
        if (
          data.status === "Đã thanh toán" ||
          data.status === "Chưa thanh toán"
        )
          badgeColor = "bg-warning text-dark";
        if (data.status === "Đã xác nhận") badgeColor = "bg-primary";
        if (data.status === "Đã khám") badgeColor = "bg-success";
        if (data.status === "Đã hủy") badgeColor = "bg-danger";

        bangGiaoDichHTML += `
                    <tr>
                        <td class="font-weight-bold text-primary">#${doc.id.substring(0, 6).toUpperCase()}</td>
                        <td>${data.patientName ? data.patientName.split(" - ")[0] : "Ẩn danh"}</td>
                        <td>Bs. ${data.doctorName || "Đang cập nhật"}</td>
                        <td>${data.time} - ${data.date}</td>
                        <td class="text-danger font-weight-bold">${(data.totalAmount || 0).toLocaleString("vi-VN")} đ</td>
                        <td><span class="badge ${badgeColor}">${data.status}</span></td>
                    </tr>
                `;
        countTableRows++;
      }
    });

    document.getElementById("tong-doanh-thu").innerText =
      tongDoanhThu.toLocaleString("vi-VN") + " đ";
    document.getElementById("tong-ca-kham").innerText = tongCaKham;
    document.getElementById("tong-bac-si").innerText = uniqueDoctors.size;
    document.getElementById("tong-benh-nhan").innerText = uniquePatients.size;
    document.getElementById("bang-giao-dich").innerHTML =
      bangGiaoDichHTML ||
      `<tr><td colspan="6" class="text-center">Chưa có giao dịch nào</td></tr>`;

    new Chart(document.getElementById("serviceChart"), {
      type: "doughnut",
      data: {
        labels: Object.keys(serviceCount),
        datasets: [
          {
            data: Object.values(serviceCount),
            backgroundColor: ["#4e73df", "#1cc88a", "#36b9cc", "#f6c23e"],
          },
        ],
      },
      options: { maintainAspectRatio: false, cutout: "70%" },
    });
    new Chart(document.getElementById("revenueChart"), {
      type: "bar",
      data: {
        labels: [
          "Tháng 1",
          "Tháng 2",
          "Tháng 3",
          "Tháng 4",
          "Tháng 5",
          "Tháng 6",
        ],
        datasets: [
          {
            label: "Doanh thu (VNĐ)",
            backgroundColor: "#4e73df",
            data: [1500000, 2100000, 1800000, 3200000, 2900000, 4500000],
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        scales: { y: { beginAtZero: true } },
      },
    });
  } catch (error) {
    console.error("Lỗi Dashboard: ", error);
  }
}

// ==============================================
// LOGIC TRANG BÁC SĨ (doctors.html)
// ==============================================
async function loadDoctorsList() {
  try {
    const doctorsRef = ref(rtdb, "Doctors");
    const snapshot = await get(doctorsRef);
    let htmlContent = "";

    if (snapshot.exists()) {
      snapshot.forEach((childSnapshot) => {
        const doctor = childSnapshot.val();
        const statusBadge =
          doctor.status === "online"
            ? `<span class="badge bg-success">Đang hoạt động</span>`
            : `<span class="badge bg-secondary">Nghỉ ngơi</span>`;

        htmlContent += `
                    <tr>
                        <td class="text-center"><img src="${doctor.Picture || "https://placehold.co/40x40.png"}" alt="Avatar" class="rounded-circle" width="40" height="40" style="object-fit: cover;"></td>
                        <td class="font-weight-bold">${doctor.Name || "Chưa cập nhật"}</td>
                        <td>${doctor.Special || "Đa khoa"}</td>
                        <td>${doctor.Experience || 0} năm</td>
                        <td>${statusBadge}</td>
                        <td>
                            <button class="btn btn-sm btn-primary"><i class="fas fa-edit"></i> Sửa</button>
                            <button class="btn btn-sm btn-danger"><i class="fas fa-trash"></i> Xóa</button>
                        </td>
                    </tr>
                `;
      });
      document.getElementById("bang-bac-si").innerHTML = htmlContent;
    } else {
      document.getElementById("bang-bac-si").innerHTML =
        `<tr><td colspan="6" class="text-center">Chưa có bác sĩ nào.</td></tr>`;
    }
  } catch (error) {
    console.error("Lỗi tải Bác sĩ: ", error);
  }
}

// HÀM TẠO CHUỖI NGẪU NHIÊN 28 KÝ TỰ CHO UID
function generateRandomUID() {
  const chars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let result = "";
  for (let i = 0; i < 28; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

// LOGIC LẮNG NGHE FORM THÊM BÁC SĨ
function listenAddDoctorForm() {
  const form = document.getElementById("form-add-doctor");
  const btnRandom = document.getElementById("btn-random-uid");
  const inputUid = document.getElementById("doc-uid");

  if (!form) return;

  // 🔴 Sự kiện bấm nút "Tạo mã"
  if (btnRandom && inputUid) {
    btnRandom.addEventListener("click", () => {
      inputUid.value = generateRandomUID();
    });
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const uid = inputUid.value.trim();
    const id = parseInt(document.getElementById("doc-id").value.trim());
    const name = document.getElementById("doc-name").value.trim();
    const special = document.getElementById("doc-special").value.trim();
    const experience = parseInt(
      document.getElementById("doc-experience").value.trim(),
    );
    const price = document.getElementById("doc-price").value.trim();
    const mobile = document.getElementById("doc-mobile").value.trim();
    const schedule = document.getElementById("doc-schedule").value.trim();
    const picture =
      document.getElementById("doc-picture").value.trim() ||
      "https://placehold.co/100x100.png";

    try {
      await set(ref(rtdb, "Doctors/" + uid), {
        Id: id,
        Name: name,
        Special: special,
        Experience: experience,
        Expriense: experience,
        Price: price,
        Mobile: mobile,
        Schedule: schedule,
        Picture: picture,
        status: "offline",
        Rating: 5,
        Biography: "Thông tin tiểu sử đang cập nhật.",
        Address: "Đà Nẵng, Việt Nam",
      });

      alert("Đã thêm hồ sơ bác sĩ mới thành công lên Firebase!");
      form.reset();
      const modalElement = document.getElementById("addDoctorModal");
      const modalInstance = bootstrap.Modal.getInstance(modalElement);
      if (modalInstance) modalInstance.hide();
      loadDoctorsList();
    } catch (error) {
      console.error("Lỗi lưu DB: ", error);
      alert("Lỗi lưu dữ liệu: " + error.message);
    }
  });
}

// ==============================================
// LOGIC TRANG BỆNH NHÂN (patients.html)
// ==============================================
async function loadPatientsList() {
  try {
    const usersRef = ref(rtdb, "Users");
    const snapshot = await get(usersRef);
    let htmlContent = "";

    if (snapshot.exists()) {
      snapshot.forEach((childSnapshot) => {
        const userId = childSnapshot.key;
        const user = childSnapshot.val();
        htmlContent += `
                    <tr>
                        <td class="text-secondary small">#${userId}</td>
                        <td class="font-weight-bold">${user.Name || user.name || "Bệnh nhân ẩn danh"}</td>
                        <td>${user.Email || user.email || "Không có Email"}</td>
                        <td><button class="btn btn-sm btn-outline-danger"><i class="fas fa-ban"></i> Chặn tài khoản</button></td>
                    </tr>
                `;
      });
      document.getElementById("bang-benh-nhan").innerHTML = htmlContent;
    } else {
      document.getElementById("bang-benh-nhan").innerHTML =
        `<tr><td colspan="4" class="text-center">Chưa có bệnh nhân nào.</td></tr>`;
    }
  } catch (error) {
    console.error("Lỗi tải Bệnh nhân: ", error);
  }
}

// ==============================================
// LOGIC TRANG LỊCH KHÁM (appointments.html)
// ==============================================
async function loadAllAppointmentsList() {
  try {
    const appointmentsRef = collection(db, "Appointments");
    const q = query(appointmentsRef, orderBy("createdAt", "desc"));
    const snapshot = await getDocs(q);
    let htmlContent = "";

    snapshot.forEach((docSnapshot) => {
      const appointmentId = docSnapshot.id;
      const data = docSnapshot.data();

      let badgeColor = "bg-secondary";
      if (data.status === "Đã thanh toán" || data.status === "Chưa thanh toán")
        badgeColor = "bg-warning text-dark";
      if (data.status === "Đã xác nhận") badgeColor = "bg-primary";
      if (data.status === "Đã khám") badgeColor = "bg-success";
      if (data.status === "Đã hủy") badgeColor = "bg-danger";

      let actionButtons = "";
      if (data.status === "Chưa thanh toán") {
        actionButtons = `<button class="btn btn-xs btn-warning btn-sm me-1" onclick="updateStatusFromAdmin('${appointmentId}', 'Đã thanh toán')">Thu tiền mặt</button>`;
      } else if (data.status === "Đã thanh toán") {
        actionButtons = `<button class="btn btn-xs btn-primary btn-sm me-1" onclick="updateStatusFromAdmin('${appointmentId}', 'Đã xác nhận')">Duyệt lịch</button>`;
      }
      if (data.status !== "Đã khám" && data.status !== "Đã hủy") {
        actionButtons += `<button class="btn btn-xs btn-danger btn-sm" onclick="updateStatusFromAdmin('${appointmentId}', 'Đã hủy')">Hủy ca</button>`;
      } else {
        actionButtons = `<span class="text-muted small">Nhiệm vụ đóng</span>`;
      }

      htmlContent += `
                <tr>
                    <td class="font-weight-bold text-primary">#${appointmentId.substring(0, 8).toUpperCase()}</td>
                    <td>${data.patientName ? data.patientName.split(" - ")[0] : "Ẩn danh"}</td>
                    <td>Bs. ${data.doctorName || "Chưa phân phối"}</td>
                    <td>${data.time} - ${data.date}</td>
                    <td class="text-danger font-weight-bold">${(data.totalAmount || 0).toLocaleString("vi-VN")} đ</td>
                    <td><span class="badge ${badgeColor}">${data.status}</span></td>
                    <td>${actionButtons}</td>
                </tr>
            `;
    });

    document.getElementById("bang-quan-ly-lich-kham").innerHTML =
      htmlContent ||
      `<tr><td colspan="7" class="text-center">Hệ thống chưa phát sinh phiếu khám nào.</td></tr>`;
  } catch (error) {
    console.error("Lỗi tải toàn bộ lịch khám: ", error);
  }
}

window.updateStatusFromAdmin = async function (id, newStatus) {
  if (
    confirm(`Xác nhận chuyển trạng thái phiếu khám này sang "${newStatus}"?`)
  ) {
    try {
      const docRef = doc(db, "Appointments", id);
      await updateDoc(docRef, { status: newStatus });
      alert("Cập nhật trạng thái phiếu thành công!");
      loadAllAppointmentsList();
    } catch (e) {
      console.error("Lỗi cập nhật: ", e);
    }
  }
};

// ==============================================
// BỘ ĐIỀU PHỐI KHỞI CHẠY HÀM ĐÚNG TRANG
// ==============================================
if (document.getElementById("tong-doanh-thu")) {
  loadDashboardData();
}
if (document.getElementById("bang-bac-si")) {
  loadDoctorsList();
  listenAddDoctorForm();
}
if (document.getElementById("bang-benh-nhan")) {
  loadPatientsList();
}
if (document.getElementById("bang-quan-ly-lich-kham")) {
  loadAllAppointmentsList();
}
