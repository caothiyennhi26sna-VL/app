package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MessageStage
import com.example.data.db.ThinkingMessageEntity

@Composable
fun MessageItemCard(
    message: ThinkingMessageEntity,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == "USER"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Bot Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Bot Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 330.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                // User Message Bubble
                Surface(
                    shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 1.dp,
                    modifier = Modifier.testTag("user_message_${message.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (message.stage == MessageStage.USER_QUERY) "Câu hỏi của bạn" else "Suy luận của bạn",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            } else {
                // Bot Message Card with Stage Badge
                val (badgeBg, badgeText, badgeIcon) = when (message.stage) {
                    MessageStage.BOT_HINT -> Triple(
                        MaterialTheme.colorScheme.primaryContainer,
                        "💡 GỢI MỞ TƯ DUY",
                        Icons.Default.Lightbulb
                    )
                    MessageStage.BOT_FEEDBACK -> Triple(
                        MaterialTheme.colorScheme.secondaryContainer,
                        "🔍 NHẬN XÉT SUY LUẬN",
                        Icons.Default.RateReview
                    )
                    MessageStage.BOT_CONCLUSION -> Triple(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        "🎯 ĐÚC KẾT & CHỐT VẤN ĐỀ",
                        Icons.Default.CheckCircle
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant,
                        "💡 ĐỒNG HÀNH",
                        Icons.Default.Lightbulb
                    )
                }

                val badgeContentColor = when (message.stage) {
                    MessageStage.BOT_HINT -> MaterialTheme.colorScheme.primary
                    MessageStage.BOT_FEEDBACK -> MaterialTheme.colorScheme.secondary
                    MessageStage.BOT_CONCLUSION -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(
                        width = if (message.stage == MessageStage.BOT_CONCLUSION) 1.5.dp else 1.dp,
                        color = if (message.stage == MessageStage.BOT_CONCLUSION) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("bot_message_${message.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Badge Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = badgeContentColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeContentColor
                                )
                            }
                        }

                        // Message Text
                        FormattedMessageText(
                            text = message.content,
                            isConclusion = message.stage == MessageStage.BOT_CONCLUSION
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedMessageText(
    text: String,
    isConclusion: Boolean,
    modifier: Modifier = Modifier
) {
    // Renders text with readable paragraph structure
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 23.sp,
            fontSize = 15.sp,
            fontWeight = if (isConclusion) FontWeight.Normal else FontWeight.Normal
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
