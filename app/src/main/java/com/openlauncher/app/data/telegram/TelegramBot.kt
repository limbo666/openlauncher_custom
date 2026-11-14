package com.openlauncher.app.data.telegram

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** A message received from the bot (already authenticated by Telegram). */
data class IncomingTelegramMessage(
    val updateId: Long,
    val messageId: Long,
    val chatId: Long,
    val fromId: Long,
    val fromName: String,
    val fromUsername: String,
    val text: String,
    val dateSec: Long
)

/**
 * Telegram Bot-API client + long-poll loop. No Telegram app or phone number is
 * needed on the unit — this talks to Telegram over plain HTTPS using a bot token.
 *
 * Security is enforced by the listener (allowed-sender allow-list lives in the
 * caller): this class only delivers messages and exposes connectivity + a log.
 */
object TelegramBot {

    private interface Api {
        @GET("getMe")
        suspend fun getMe(): MeResponse

        @GET("getUpdates")
        suspend fun getUpdates(
            @Query("offset") offset: Long?,
            @Query("timeout") timeout: Int,
            @Query("limit") limit: Int
        ): UpdatesResponse

        @GET("sendMessage")
        suspend fun sendMessage(
            @Query("chat_id") chatId: Long,
            @Query("text") text: String
        ): SendResponse
    }

    private data class MeResponse(
        @SerializedName("ok") val ok: Boolean?,
        @SerializedName("result") val result: MeResult?
    )
    private data class MeResult(
        @SerializedName("id") val id: Long?,
        @SerializedName("username") val username: String?,
        @SerializedName("first_name") val firstName: String?
    )
    private data class UpdatesResponse(
        @SerializedName("ok") val ok: Boolean?,
        @SerializedName("result") val result: List<Update>?
    )
    private data class Update(
        @SerializedName("update_id") val updateId: Long?,
        @SerializedName("message") val message: Msg?
    )
    private data class Msg(
        @SerializedName("message_id") val messageId: Long?,
        @SerializedName("from") val from: From?,
        @SerializedName("chat") val chat: Chat?,
        @SerializedName("text") val text: String?,
        @SerializedName("date") val date: Long?
    )
    private data class From(
        @SerializedName("id") val id: Long?,
        @SerializedName("first_name") val firstName: String?,
        @SerializedName("username") val username: String?
    )
    private data class Chat(@SerializedName("id") val id: Long?)
    private data class SendResponse(@SerializedName("ok") val ok: Boolean?)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun api(token: String): Api = Retrofit.Builder()
        .baseUrl("https://api.telegram.org/bot$token/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Api::class.java)

    // ── Observable state (read by the settings page + header indicator) ─────
    private val _log = mutableListOf<String>()
    val log: List<String> get() = _log
    var logRevision by mutableStateOf(0)
        private set
    var connected by mutableStateOf(false)
        private set

    /** Called on every incoming message (already token-authenticated). */
    var listener: ((IncomingTelegramMessage) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null
    private var lastOffset: Long = 0

    fun log(line: String) {
        val ts = System.currentTimeMillis()
        _log.add("[%1\$tH:%1\$tM:%1\$tS] %2\$s".format(ts, line))
        while (_log.size > 120) _log.removeAt(0)
        logRevision++
    }

    fun start(token: String) {
        if (token.isBlank()) { connected = false; return }
        if (pollJob?.isActive == true) return
        lastOffset = 0
        log("Bot polling started")
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val resp = api(token).getUpdates(offset = if (lastOffset > 0) lastOffset else null, timeout = 25, limit = 10)
                    if (resp.ok == true) {
                        connected = true
                        val updates = resp.result ?: emptyList()
                        updates.forEach { u ->
                            val m = u.message ?: return@forEach
                            val chatId = m.chat?.id ?: return@forEach
                            val fromId = m.from?.id ?: return@forEach
                            val text = m.text ?: return@forEach
                            listener?.invoke(
                                IncomingTelegramMessage(
                                    updateId = u.updateId ?: 0L,
                                    messageId = m.messageId ?: 0L,
                                    chatId = chatId,
                                    fromId = fromId,
                                    fromName = m.from?.firstName ?: "",
                                    fromUsername = m.from?.username ?: "",
                                    text = text,
                                    dateSec = m.date ?: 0L
                                )
                            )
                            if ((u.updateId ?: 0L) >= lastOffset) lastOffset = (u.updateId ?: 0L) + 1
                        }
                    } else {
                        connected = false
                        delay(10_000)
                    }
                } catch (t: Throwable) {
                    connected = false
                    log("Poll error: ${t.message ?: t.javaClass.simpleName}")
                    delay(10_000)
                }
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        connected = false
        log("Bot polling stopped")
    }

    suspend fun send(token: String, chatId: Long, text: String): Boolean =
        runCatching { api(token).sendMessage(chatId, text).ok == true }.getOrDefault(false)

    /** Validates the token and returns a human-readable summary, or null on failure. */
    suspend fun test(token: String): String? {
        val me = runCatching { api(token).getMe() }.getOrNull() ?: return null
        if (me.ok != true || me.result == null) return null
        val r = me.result!!
        return "Bot ok — @${r.username ?: "?"} (${r.firstName ?: ""}) id=${r.id ?: "?"}"
    }
}
