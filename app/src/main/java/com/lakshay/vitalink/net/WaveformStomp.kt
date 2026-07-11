package com.lakshay.vitalink.net

import com.lakshay.vitalink.data.Backend
import com.lakshay.vitalink.data.WaveformFrame
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Live ECG over STOMP-on-WebSocket, hand-rolled on OkHttp. STOMP is a tiny text
 * protocol (CONNECT / SUBSCRIBE / MESSAGE frames terminated by NUL), so a client this
 * small beats pulling in a STOMP library and its version churn.
 * ponytail: minimal STOMP 1.2, enough for one broadcast subscription; swap in a real
 * library only if you need heart-beats, acks, or transactions.
 * The JWT rides on the WebSocket handshake via Backend.http's auth interceptor. The
 * server streams only while subscribed, so collecting starts the stream and cancelling
 * stops it.
 */
object WaveformStomp {
    private const val NUL = '\u0000'
    private val adapter = Backend.moshi.adapter(WaveformFrame::class.java)

    fun frames(encounterId: Long): Flow<WaveformFrame> = callbackFlow {
        val request = Request.Builder()
            .url(Backend.wsUrl())
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
        val ws = Backend.http.newWebSocket(request, listener)
        awaitClose { ws.close(1000, "bye") }
    }
}
