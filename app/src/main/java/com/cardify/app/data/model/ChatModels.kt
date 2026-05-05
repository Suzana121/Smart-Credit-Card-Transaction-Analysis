package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

data class Chat(
    @SerializedName("id")               val id: String = "",
    @SerializedName("participants")     val participants: List<String> = emptyList(),
    @SerializedName("participantNames") val participantNames: Map<String, String> = emptyMap(),
    /** displayNames — שמות תצוגה עם כינויים גלובליים (מגיע מהשרת) */
    @SerializedName("displayNames")     val displayNames: Map<String, String> = emptyMap(),
    @SerializedName("isGroup")          val isGroup: Boolean = false,
    @SerializedName("groupName")        val groupName: String = "",
    @SerializedName("lastMessage")      val lastMessage: String = "",
    @SerializedName("lastMessageAt")    val lastMessageAt: String = "",
    @SerializedName("unreadCount")      val unreadCount: Int = 0
)

data class ChatMessage(
    @SerializedName("id")             val id: String = "",
    @SerializedName("senderId")       val senderId: String = "",
    @SerializedName("senderName")     val senderName: String = "",
    @SerializedName("text")           val text: String = "",
    @SerializedName("deleted")        val deleted: Boolean = false,
    @SerializedName("forwarded")      val forwarded: Boolean = false,
    @SerializedName("transaction")    val transaction: ChatTransaction? = null,
    @SerializedName("replyToId")      val replyToId: String? = null,
    @SerializedName("replyToMessage") val replyToMessage: ReplySnapshot? = null,
    @SerializedName("audioUrl")       val audioUrl: String? = null,
    @SerializedName("audioDuration")  val audioDuration: Int = 0,
    @SerializedName("reactions")      val reactions: Map<String, String> = emptyMap(),
    @SerializedName("timestamp")      val timestamp: String = ""
)

data class ReplySnapshot(
    @SerializedName("senderName")   val senderName: String = "",
    @SerializedName("text")         val text: String = "",
    @SerializedName("businessName") val businessName: String = "",
    @SerializedName("isAudio")      val isAudio: Boolean = false
)

data class ChatTransaction(
    @SerializedName("businessName") val businessName: String = "",
    @SerializedName("amount")       val amount: Double = 0.0,
    @SerializedName("date")         val date: String = "",
    @SerializedName("category")     val category: String = "",
    @SerializedName("status")       val status: String = "REGULAR",
    @SerializedName("explanation")  val explanation: String = ""
)

data class CreateChatRequest(
    @SerializedName("participantIds") val participantIds: List<String>,
    @SerializedName("groupName")      val groupName: String = ""
)

data class SendMessageRequest(
    @SerializedName("text")           val text: String = "",
    @SerializedName("transaction")    val transaction: ChatTransaction? = null,
    @SerializedName("replyToId")      val replyToId: String? = null,
    @SerializedName("replyToMessage") val replyToMessage: ReplySnapshot? = null,
    @SerializedName("audioUrl")       val audioUrl: String? = null,
    @SerializedName("audioDuration")  val audioDuration: Int = 0,
    @SerializedName("forwarded")      val forwarded: Boolean = false
)

data class UnreadResponse(
    @SerializedName("unread") val unread: Int = 0
)

/** בקשה לשינוי כינוי גלובלי */
data class SetNicknameRequest(
    @SerializedName("nickname") val nickname: String
)

data class ReactRequest(
    @SerializedName("emoji") val emoji: String
)