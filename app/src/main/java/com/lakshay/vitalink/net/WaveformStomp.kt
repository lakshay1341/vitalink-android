package com.lakshay.vitalink.net

import com.lakshay.vitalink.data.SessionManager
import com.lakshay.vitalink.data.WaveformFrame
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaveformStomp @Inject constructor(
    private val client: OkHttpClient,
    moshi: Moshi,
) {
    private val adapter = moshi.adapter(WaveformFrame::class.java)

    fun frames(encounterId: Long): Flow<WaveformFrame> = callbackFlow {
        val request = Request.Builder()
            .url(SessionManager.wsUrl())
            .addHeader("Sec-WebSocket-Protocol", "v12.stomp")
            .build()
        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n$NUL")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val command = text.substringBefore('\n')
                when {
                    command.startsWith("CONNECTED") ->
                        ws.send("SUBSCRIBE\nid:sub-0\ndestination:/topic/waveform/$encounterId\n\n$NUL")
                    command.startsWith("MESSAGE") -> {
                        val body = text.substringAfter("\n\n").substringBefore(NUL)
                        runCatching { adapter.fromJson(body) }.getOrNull()?.let { trySend(it) }
                    }
                    command.startsWith("ERROR") -> close()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        }
        val ws = client.newWebSocket(request, listener)
        awaitClose { ws.close(1000, "bye") }
    }

    companion object {
        private const val NUL = '\u0000'
    }
}
