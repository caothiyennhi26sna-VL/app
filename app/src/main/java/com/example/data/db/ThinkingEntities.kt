package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SessionStatus {
    EXPLORING,   // Đang trong quá trình gợi mở & suy luận
    CONCLUDED    // Đã chốt vấn đề & đúc kết
}

enum class MessageStage {
    USER_QUERY,    // Câu hỏi ban đầu của người dùng
    BOT_HINT,      // Bot gợi mở tư duy & đặt câu hỏi định hướng
    USER_THOUGHT,  // Người dùng thử sức giải thích / đưa ra suy luận
    BOT_FEEDBACK,  // Bot nhận xét suy luận và dẫn dắt tiếp
    BOT_CONCLUSION // Bot chốt vấn đề, đúc kết đáp án và bài học
}

@Entity(tableName = "thinking_sessions")
data class ThinkingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val initialQuestion: String,
    val category: String, // Khoa học, Triết học, Toán học, Đời sống, v.v.
    val socraticMode: String, // Co_ban, Chuyen_sau, Phan_bien
    val status: SessionStatus = SessionStatus.EXPLORING,
    val conclusionSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val exchangeCount: Int = 0
)

@Entity(
    tableName = "thinking_messages",
    foreignKeys = [
        ForeignKey(
            entity = ThinkingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ThinkingMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val sender: String, // "USER" hoặc "BOT"
    val content: String,
    val stage: MessageStage,
    val timestamp: Long = System.currentTimeMillis()
)
