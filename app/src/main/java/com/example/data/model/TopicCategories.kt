package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector

data class StarterQuestion(
    val title: String,
    val question: String,
    val category: String,
    val socraticMode: String,
    val teaserHint: String
)

data class CategoryItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)

object ThinkingDataCatalog {
    val categories = listOf(
        CategoryItem("science", "Khoa học & Tự nhiên", "Khám phá quy luật vật lý, hóa học, vũ trụ", Icons.Default.Science),
        CategoryItem("logic", "Toán học & Logic", "Rèn luyện tư duy phân tích, giải đố logic", Icons.Default.Calculate),
        CategoryItem("philosophy", "Triết học & Cuộc sống", "Góc nhìn sâu sắc về nhân sinh và quyết định", Icons.Default.Psychology),
        CategoryItem("technology", "Công nghệ & AI", "Hiểu bản chất của thuật toán và tương lai số", Icons.Default.AutoAwesome)
    )

    val starterQuestions = listOf(
        StarterQuestion(
            title = "Màu của bầu trời",
            question = "Tại sao ban ngày bầu trời có màu xanh nhưng hoàng hôn lại có màu đỏ rực?",
            category = "Khoa học & Tự nhiên",
            socraticMode = "Co_ban",
            teaserHint = "Ánh sáng mặt trời thực ra có màu gì và khí quyển có những gì?"
        ),
        StarterQuestion(
            title = "Nước đá nổi trên nước",
            question = "Tại sao hầu hết các chất khi đông đặc đều chìm, riêng nước đá lại nổi trên mặt nước?",
            category = "Khoa học & Tự nhiên",
            socraticMode = "Chuyen_sau",
            teaserHint = "Cấu trúc mạng tinh thể và mật độ phân tử thay đổi ra sao khi nhiệt độ giảm?"
        ),
        StarterQuestion(
            title = "Phép chia cho số 0",
            question = "Tại sao chúng ta có thể chia 0 cho mọi số nhưng không bao giờ được chia một số cho 0?",
            category = "Toán học & Logic",
            socraticMode = "Chuyen_sau",
            teaserHint = "Phép chia thực chất là phép toán ngược của phép tính nào?"
        ),
        StarterQuestion(
            title = "Đam mê hay Thực tế",
            question = "Người trẻ nên chọn công việc theo đam mê hay theo nhu cầu thực tế của thị trường?",
            category = "Triết học & Cuộc sống",
            socraticMode = "Phan_bien",
            teaserHint = "Điều gì nuôi dưỡng đam mê lâu dài và rủi ro của từng lựa chọn là gì?"
        ),
        StarterQuestion(
            title = "Ngôn ngữ nhị phân 0 & 1",
            question = "Tại sao máy tính hiện đại lại xử lý mọi thông tin chỉ bằng hai số 0 và 1?",
            category = "Công nghệ & AI",
            socraticMode = "Co_ban",
            teaserHint = "Phần cứng vật lý cơ bản nhất bên trong bóng bán dẫn (transistor) hoạt động như thế nào?"
        ),
        StarterQuestion(
            title = "Nghịch lý Con Tàu Theseus",
            question = "Nếu thay thế từng tấm gỗ mục của một con tàu cho đến khi không còn tấm gỗ cũ nào, đó còn là con tàu ban đầu không?",
            category = "Triết học & Cuộc sống",
            socraticMode = "Phan_bien",
            teaserHint = "Bản sắc của một thực thể nằm ở chất liệu cấu thành hay ở hình thức và ký ức?"
        )
    )
}
