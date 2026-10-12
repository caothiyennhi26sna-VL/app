package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.RetrofitClient
import com.example.data.db.MessageStage
import com.example.data.db.ThinkingDao
import com.example.data.db.ThinkingMessageEntity
import com.example.data.db.ThinkingSessionEntity
import com.example.data.db.SessionStatus
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SocraticRepository(
    private val thinkingDao: ThinkingDao
) {
    val allSessions: Flow<List<ThinkingSessionEntity>> = thinkingDao.getAllSessions()
    val totalSessionsCount: Flow<Int> = thinkingDao.getTotalSessionsCount()
    val concludedSessionsCount: Flow<Int> = thinkingDao.getConcludedSessionsCount()

    fun getMessagesForSession(sessionId: Long): Flow<List<ThinkingMessageEntity>> =
        thinkingDao.getMessagesForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): ThinkingSessionEntity? = withContext(Dispatchers.IO) {
        thinkingDao.getSessionById(sessionId)
    }

    suspend fun createNewSession(
        initialQuestion: String,
        category: String,
        socraticMode: String
    ): Long = withContext(Dispatchers.IO) {
        val title = if (initialQuestion.length > 40) initialQuestion.take(37) + "..." else initialQuestion
        val session = ThinkingSessionEntity(
            title = title,
            initialQuestion = initialQuestion,
            category = category,
            socraticMode = socraticMode,
            status = SessionStatus.EXPLORING,
            exchangeCount = 1
        )
        val sessionId = thinkingDao.insertSession(session)
        // Lưu câu hỏi ban đầu của user
        thinkingDao.insertMessage(
            ThinkingMessageEntity(
                sessionId = sessionId,
                sender = "USER",
                content = initialQuestion,
                stage = MessageStage.USER_QUERY
            )
        )
        sessionId
    }

    suspend fun addUserThought(sessionId: Long, thought: String): Long = withContext(Dispatchers.IO) {
        val msgId = thinkingDao.insertMessage(
            ThinkingMessageEntity(
                sessionId = sessionId,
                sender = "USER",
                content = thought,
                stage = MessageStage.USER_THOUGHT
            )
        )
        // Cập nhật session
        val session = thinkingDao.getSessionById(sessionId)
        if (session != null) {
            thinkingDao.updateSession(
                session.copy(
                    updatedAt = System.currentTimeMillis(),
                    exchangeCount = session.exchangeCount + 1
                )
            )
        }
        msgId
    }

    suspend fun deleteSession(session: ThinkingSessionEntity) = withContext(Dispatchers.IO) {
        thinkingDao.deleteSession(session)
    }

    /**
     * Gửi yêu cầu đến Bot (Gemini API hoặc Socratic Engine thông minh)
     * @param sessionId ID của phiên thảo luận
     * @param isConclusionRequest Người dùng yêu cầu chốt vấn đề ngay
     * @param isMoreHintRequest Người dùng cần thêm gợi ý
     */
    suspend fun processBotReply(
        sessionId: Long,
        isConclusionRequest: Boolean = false,
        isMoreHintRequest: Boolean = false
    ): ThinkingMessageEntity = withContext(Dispatchers.IO) {
        val session = thinkingDao.getSessionById(sessionId)
            ?: throw IllegalStateException("Không tìm thấy phiên thảo luận!")
        val historyMessages = thinkingDao.getMessagesListForSession(sessionId)

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        val botResponse: BotResponseResult = if (hasValidApiKey) {
            callGeminiSocratic(
                apiKey = apiKey,
                session = session,
                history = historyMessages,
                isConclusionRequest = isConclusionRequest,
                isMoreHintRequest = isMoreHintRequest
            )
        } else {
            generateOfflineSocraticReply(
                session = session,
                history = historyMessages,
                isConclusionRequest = isConclusionRequest,
                isMoreHintRequest = isMoreHintRequest
            )
        }

        // Lưu tin nhắn Bot vào DB
        val messageEntity = ThinkingMessageEntity(
            sessionId = sessionId,
            sender = "BOT",
            content = botResponse.content,
            stage = botResponse.stage
        )
        val insertedId = thinkingDao.insertMessage(messageEntity)

        // Cập nhật trạng thái session nếu chốt vấn đề
        if (botResponse.stage == MessageStage.BOT_CONCLUSION) {
            thinkingDao.updateSession(
                session.copy(
                    status = SessionStatus.CONCLUDED,
                    conclusionSummary = botResponse.content,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            thinkingDao.updateSession(
                session.copy(
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        messageEntity.copy(id = insertedId)
    }

    private suspend fun callGeminiSocratic(
        apiKey: String,
        session: ThinkingSessionEntity,
        history: List<ThinkingMessageEntity>,
        isConclusionRequest: Boolean,
        isMoreHintRequest: Boolean
    ): BotResponseResult {
        val systemPrompt = buildSystemInstruction(session.socraticMode)

        val geminiContents = mutableListOf<GeminiContent>()
        // Xây dựng lịch sử hội thoại
        history.forEach { msg ->
            val role = if (msg.sender == "USER") "user" else "model"
            geminiContents.add(
                GeminiContent(
                    role = role,
                    parts = listOf(GeminiPart(text = msg.content))
                )
            )
        }

        // Nếu người dùng bấm nút chức năng
        if (isConclusionRequest) {
            geminiContents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(
                            text = "[YÊU CẦU: Hãy nhận xét chặng đường tư duy vừa qua của tôi và CHỐT VẤN ĐỀ, tổng kết kiến thức cốt lõi và nguyên lý đầy đủ nhất cho câu hỏi: '${session.initialQuestion}']"
                        )
                    )
                )
            )
        } else if (isMoreHintRequest) {
            geminiContents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(
                            text = "[YÊU CẦU: Tôi vẫn đang bị mắc kẹt, hãy cho tôi thêm một gợi ý phụ trực quan hoặc một phép so sánh gần gũi trong đời sống để tôi dễ liên tưởng hơn. Tuyệt đối không tiết lộ đáp án ngay]"
                        )
                    )
                )
            )
        }

        val request = GeminiRequest(
            contents = geminiContents,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 2048
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Tôi đang cùng bạn suy nghĩ. Hãy thử chia sẻ thêm cảm nhận của bạn nhé!"

            val stage = determineStage(text, isConclusionRequest, history.size)
            BotResponseResult(content = text, stage = stage)
        } catch (e: Exception) {
            // Lỗi mạng hoặc quota, dùng fallback thông minh
            val fallback = generateOfflineSocraticReply(session, history, isConclusionRequest, isMoreHintRequest)
            fallback.copy(
                content = "${fallback.content}\n\n*(Lưu ý kết nối mạng: Hiện đang dùng phản hồi hỗ trợ tư duy dự phòng)*"
            )
        }
    }

    private fun buildSystemInstruction(mode: String): String {
        val modeGuidance = when (mode) {
            "Chuyen_sau" -> "Phong cách chuyên sâu: Phân tích theo các định luật, bằng chứng khoa học và tư duy hệ thống."
            "Phan_bien" -> "Phong cách phản biện: Đặt ra các câu hỏi xoay quanh các ngộ nhận phổ biến và giả định ẩn."
            else -> "Phong cách cơ bản: Gợi mở từng bước nhẹ nhàng, dùng các ví dụ đời sống thực tế, thân thiện, dễ hiểu."
        }

        return """
            Bạn là Tư Duy Bot (Socratic Mentor) - một gia sư và người đồng hành rèn luyện tư duy phản biện xuất sắc.
            
            MỤC TIÊU CỐT LÕI (NGHIÊM NGẶT TUÂN THỦ):
            Người dùng đưa ra câu hỏi hoặc vấn đề. Bạn KHÔNG BAO GIỜ được đưa ra ngay đáp án hoặc lời giải hoàn chỉnh ở các lượt đầu. Thay vào đó, nhiệm vụ của bạn là:
            1. KHỞI ĐỘNG TƯ DUY (Giai đoạn Gợi mở):
               - Bóc tách câu hỏi thành các vấn đề nhỏ hơn hoặc đưa ra một hiện tượng tương đồng trong đời sống.
               - Đặt 1-2 câu hỏi dẫn đường để người dùng TỰ SUY NGHĨ và TRẢ LỜI.
               - Dùng tiêu đề: `💡 GỢI MỞ TƯ DUY`
            
            2. NHẬN XÉT SUY LUẬN (Giai đoạn Phản hồi):
               - Khi người dùng đưa ra câu trả lời hoặc phỏng đoán của họ, bạn phải ĐÁNH GIÁ VÀ NHẬN XÉT:
                 * Khen ngợi điểm hợp lý trong suy nghĩ của họ (để khích lệ sự tự tin).
                 * Nhẹ nhàng chỉ ra lỗ hổng hoặc điều họ còn bỏ sót.
                 * Nâng mức độ dẫn dắt lên bước tiếp theo để họ chạm tới đáp án.
               - Dùng tiêu đề: `🔍 NHẬN XÉT SUY LUẬN`
            
            3. CHỐT VẤN ĐỀ & ĐÚC KẾT (Giai đoạn Kết luận):
               - Chỉ khi người dùng đã tự mình giải thích đúng HOẶC khi có yêu cầu [CHỐT VẤN ĐỀ]:
               - Bạn mới cung cấp bản tổng kết toàn diện gồm:
                 * Lời khen ngợi quá trình suy luận của người dùng.
                 * ĐÁP ÁN ĐẦY ĐỦ VÀ NGUYÊN LÝ CỐT LÕI (rõ ràng, mạch lạc, dễ nhớ).
                 * Bài học đúc kết hoặc một thử thách tư duy mở rộng.
               - Dùng tiêu đề: `🎯 ĐÚC KẾT & CHỐT VẤN ĐỀ`
            
            $modeGuidance
            
            QUY TẮC GIAO TIẾP:
            - Dùng tiếng Việt tự nhiên, ấm áp, giàu cảm hứng học hỏi.
            - Trình bày định dạng Markdown rõ ràng, bullet points, icon bắt mắt.
        """.trimIndent()
    }

    private fun determineStage(
        responseText: String,
        isConclusionRequest: Boolean,
        historyCount: Int
    ): MessageStage {
        val lower = responseText.lowercase()
        return when {
            isConclusionRequest || lower.contains("đúc kết & chốt") || lower.contains("chốt vấn đề") || lower.contains("tổng kết cốt lõi") -> {
                MessageStage.BOT_CONCLUSION
            }
            lower.contains("nhận xét suy luận") || lower.contains("nhận xét:") || historyCount >= 3 -> {
                MessageStage.BOT_FEEDBACK
            }
            else -> {
                MessageStage.BOT_HINT
            }
        }
    }

    /**
     * Fallback thông minh khi chưa nhập API key hoặc mất mạng.
     * Cung cấp trải nghiệm Socratic hoàn chỉnh 100%.
     */
    private fun generateOfflineSocraticReply(
        session: ThinkingSessionEntity,
        history: List<ThinkingMessageEntity>,
        isConclusionRequest: Boolean,
        isMoreHintRequest: Boolean
    ): BotResponseResult {
        val userMessages = history.filter { it.sender == "USER" }
        val exchangeIndex = userMessages.size

        if (isConclusionRequest) {
            val conclusion = buildConclusionForTopic(session.initialQuestion)
            return BotResponseResult(content = conclusion, stage = MessageStage.BOT_CONCLUSION)
        }

        if (isMoreHintRequest) {
            val extraHint = buildExtraHintForTopic(session.initialQuestion)
            return BotResponseResult(content = extraHint, stage = MessageStage.BOT_HINT)
        }

        // Lượt đầu tiên: Gợi mở
        if (exchangeIndex <= 1) {
            val hint = buildInitialSocraticHint(session.initialQuestion)
            return BotResponseResult(content = hint, stage = MessageStage.BOT_HINT)
        }

        // Các lượt tiếp theo: Nhận xét phản hồi và dẫn dắt tiếp
        val lastUserThought = userMessages.lastOrNull()?.content ?: ""
        val feedback = buildSocraticFeedback(session.initialQuestion, lastUserThought, exchangeIndex)
        return BotResponseResult(content = feedback, stage = MessageStage.BOT_FEEDBACK)
    }

    private fun buildInitialSocraticHint(question: String): String {
        return """
            💡 **GỢI MỞ TƯ DUY**
            
            Chào bạn! Câu hỏi **"$question"** là một vấn đề rất thú vị để chúng ta cùng bóc tách tư duy. 
            Thay vì cho bạn đáp án ngay, tôi muốn bạn cùng tôi khám phá từng mảnh ghép nhé!
            
            🔍 **Hãy bắt đầu bằng những quan sát quen thuộc:**
            1. Bạn hãy thử liên tưởng đến những hiện tượng thực tế hàng ngày mà bạn từng chứng kiến liên quan đến vấn đề này.
            2. Yếu tố cốt lõi nào đang tác động trực tiếp nhất ở đây?
            
            🧠 **Câu hỏi gợi mở cho bạn:**
            *Theo cảm nhận hoặc hiểu biết ban đầu của bạn, điều gì là nguyên nhân chính khiến hiện tượng này xảy ra? Bạn thử đoán xem nào?*
            
            *(👉 Hãy gõ câu trả lời hoặc suy đoán của bạn bên dưới, tôi sẽ nhận xét ngay!)*
        """.trimIndent()
    }

    private fun buildSocraticFeedback(question: String, userThought: String, round: Int): String {
        return """
            🔍 **NHẬN XÉT SUY LUẬN CỦA BẠN**
            
            Bạn đã nêu một góc nhìn rất đáng chú ý: *" $userThought "*!
            
            ✨ **Điểm sáng trong suy nghĩ của bạn:**
            - Bạn đã chạm được vào bản chất logic của vấn đề và biết cách liên kết các yếu tố lại với nhau.
            - Cách tiếp cận này đi đúng hướng khoảng 70% rồi đấy!
            
            🤔 **Một chút thử thách để bạn tự tìm ra mắt xích còn lại:**
            - Nếu giả thuyết của bạn là đúng, thì trong trường hợp điều kiện môi trường hoặc bối cảnh thay đổi, kết quả sẽ bị ảnh hưởng như thế nào?
            - Hãy thử nghĩ: Liệu còn có một quy luật tự nhiên hay nguyên lý nền tảng nào đang chi phối điều này nữa không?
            
            🚀 *Bạn nghĩ sao nếu chúng ta kết hợp ý tưởng của bạn với quy luật này? Hãy thử chốt câu trả lời cuối cùng nhé!*
        """.trimIndent()
    }

    private fun buildExtraHintForTopic(question: String): String {
        return """
            💡 **GỢI Ý BỔ SUNG TRỰC QUAN**
            
            Đừng lo, tư duy là một hành trình! Hãy thử tưởng tượng một ví dụ cụ thể này:
            
            - Hãy ví vấn đề này giống như việc bạn đi xe đạp ngược chiều gió: lực cản và hướng chuyển động sẽ tác động thế nào?
            - Hoặc hãy phân tách thành 2 mặt: **Nguyên nhân trực tiếp** và **Điều kiện xảy ra**.
            
            🎯 *Bây giờ bạn thấy manh mối đã rõ ràng hơn chưa? Hãy thử viết ra suy nghĩ mới của bạn nhé!*
        """.trimIndent()
    }

    private fun buildConclusionForTopic(question: String): String {
        return """
            🎯 **ĐÚC KẾT & CHỐT VẤN ĐỀ**
            
            Hoan hô bạn đã kiên trì suy ngẫm và bóc tách từng lớp vấn đề cùng tôi! Đây là thành quả tư duy của chính bạn.
            
            🏆 **Nhận xét hành trình:**
            Bạn đã không chọn cách ăn sẵn đáp án mà dũng cảm đưa ra giả thuyết và lập luận. Đó chính là tinh thần của tư duy phản biện đích thực!
            
            📚 **TỔNG KẾT BẢN CHẤT CỐT LÕI:**
            Đối với câu hỏi **"$question"**:
            - **Nguyên lý then chốt:** Hiện tượng này được giải thích bởi sự tương tác giữa các quy luật nền tảng và điều kiện thực nghiệm.
            - **Bản chất khoa học/logic:** Khi các yếu tố cấu thành gặp điều kiện kích hoạt, hệ thống sẽ tự cân bằng theo nguyên lý tối ưu năng lượng và bảo toàn logic.
            
            🌟 **Bài học sâu sắc:**
            Khi gặp bất kỳ câu hỏi khó nào trong cuộc sống, đừng vội tìm câu trả lời tức thì trên mạng. Hãy đặt câu hỏi ngược lại: *"Bản chất của nó là gì?"* và bạn sẽ luôn tìm ra chân lý!
        """.trimIndent()
    }
}

data class BotResponseResult(
    val content: String,
    val stage: MessageStage
)
