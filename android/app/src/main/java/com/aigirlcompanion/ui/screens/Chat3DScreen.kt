package com.aigirlcompanion.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.aigirlcompanion.R
import com.aigirlcompanion.data.ChatMessage
import com.aigirlcompanion.data.ChatRepository
import com.aigirlcompanion.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ModelPathHandler(private val context: Context) : WebViewAssetLoader.PathHandler {
    override fun handle(path: String): WebResourceResponse? {
        val cleanPath = path.trimStart('/')
        // 1. Try external files dir: /sdcard/Android/data/com.aigirlcompanion/files/models/...
        val extFile = java.io.File(context.getExternalFilesDir(null), "models/$cleanPath")
        if (extFile.exists() && extFile.isFile) {
            return WebResourceResponse(
                if (cleanPath.endsWith(".glb")) "model/gltf-binary" else "application/octet-stream",
                null,
                extFile.inputStream()
            )
        }
        // 2. Try internal files dir: /data/user/0/com.aigirlcompanion/files/models/...
        val intFile = java.io.File(context.filesDir, "models/$cleanPath")
        if (intFile.exists() && intFile.isFile) {
            return WebResourceResponse(
                if (cleanPath.endsWith(".glb")) "model/gltf-binary" else "application/octet-stream",
                null,
                intFile.inputStream()
            )
        }
        return null
    }
}

class Android3DBridge(
    private val onSend: (String) -> Unit,
    private val onModelLoaded: (String, Int) -> Unit,
    private val onTouchMesh: (String) -> Unit,
    private val onDeleteMessage: (String) -> Unit,
    private val onPlayVoiceReply: (String, String) -> Unit,
    private val onStopVoice: () -> Unit,
) {
    @JavascriptInterface
    fun sendMessage(text: String) {
        onSend(text)
    }

    @JavascriptInterface
    fun onModelLoaded(label: String, morphCount: Int) {
        onModelLoaded(label, morphCount)
    }

    @JavascriptInterface
    fun onMeshTouched(partName: String) {
        onTouchMesh(partName)
    }

    @JavascriptInterface
    fun deleteMessage(messageId: String) {
        onDeleteMessage(messageId)
    }

    @JavascriptInterface
    fun playVoiceReply(messageId: String, text: String) {
        onPlayVoiceReply(messageId, text)
    }

    @JavascriptInterface
    fun stopVoice() {
        onStopVoice()
    }
}

@Composable
fun Chat3DScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    messages: List<ChatMessage>,
    sending: Boolean,
    lastEmotion: String,
    onSend: (String, (String) -> Unit) -> Unit,
    onSendDocument: (String, ByteArray, String, (String) -> Unit) -> Unit,
    onDeleteMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ChatRepository() }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var input by remember { mutableStateOf("") }
    var modelStatus by remember { mutableStateOf("Initializing 3D Stage...") }
    var isModelReady by remember { mutableStateOf(false) }
    var lastHandledMessageId by remember { mutableStateOf<String?>(null) }
    var activeCameraPresetIndex by remember { mutableIntStateOf(1) } // 0: closeup, 1: upper, 2: full
    var isChatDrawerOpen by remember { mutableStateOf(false) }
    var isTelemetryOpen by remember { mutableStateOf(false) }
    var isSubtitleVisible by remember { mutableStateOf(true) }

    val cameraPresets = listOf("closeup", "upper", "full")

    fun triggerHaptic(durationMs: Long = 40) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun executeJs(js: String) {
        webViewInstance?.post {
            webViewInstance?.evaluateJavascript(js) { res ->
                Log.d("Ana3D_ExecJs", "JS evaluated [res=$res]")
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                input = spoken
                onSend(spoken) {}
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                }
            }.getOrNull() ?: uri.lastPathSegment ?: "document.png"

            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()

            if (bytes != null && bytes.isNotEmpty()) {
                val resolvedName = fileName
                onSendDocument(resolvedName, bytes, "") {}
            }
        }
    }

    // Sync latest assistant message to 3D avatar (emotion + sweet voice lip-sync)
    val latestAssistant = messages.lastOrNull { it.role == "assistant" }
    LaunchedEffect(latestAssistant?.id, isModelReady) {
        if (!isModelReady || latestAssistant == null) return@LaunchedEffect
        if (latestAssistant.id == lastHandledMessageId) return@LaunchedEffect
        lastHandledMessageId = latestAssistant.id
        isSubtitleVisible = true

        val replyText = latestAssistant.content
        val emotion = latestAssistant.emotion.ifBlank { "loving" }

        // 1. Instantly trigger 3D visual expression, gesture, and procedural speech (< 50ms)
        val initialJs = buildString {
            append("window.Ana3D && window.Ana3D.receiveReply(")
            append(JSONObject.quote(replyText)).append(", ")
            append(JSONObject.quote(emotion)).append(", null, null);")
        }
        executeJs(initialJs)

        // 2. In parallel, fetch sweet neural voice audio stream without blocking visual reaction
        scope.launch(Dispatchers.IO) {
            val base64Audio = repository.synthesizeVoiceBase64(profile, replyText)
            if (base64Audio != null) {
                val audioJs = buildString {
                    append("window.Ana3D && window.Ana3D.playAudio(")
                    append(JSONObject.quote(base64Audio)).append(", ")
                    append(JSONObject.quote(replyText)).append(");")
                }
                executeJs(audioJs)
            }
        }
    }

    // Sync full conversation history to 3D WhatsApp drawer
    LaunchedEffect(messages, isModelReady) {
        if (!isModelReady) return@LaunchedEffect
        try {
            val jsonArray = JSONArray()
            messages.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("role", msg.role)
                    put("text", msg.content)
                    put("content", msg.content)
                    put("emotion", msg.emotion)
                    put("timestamp", msg.timestamp)
                }
                jsonArray.put(obj)
            }
            executeJs("window.Ana3D && window.Ana3D.syncConversation && window.Ana3D.syncConversation($jsonArray);")
        } catch (_: Exception) {}
    }

    // Sync sending state
    LaunchedEffect(sending) {
        if (isModelReady) {
            executeJs("window.Ana3D && window.Ana3D.setTyping(${if (sending) "true" else "false"});")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executeJs("window.Ana3D && window.Ana3D.stopVoice && window.Ana3D.stopVoice();")
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. Hardware-Accelerated 3D WebGL Canvas
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    // Enable Hardware Acceleration for 60 FPS WebGL
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(0x00000000)

                    val assetLoader = WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                        .addPathHandler("/models/", ModelPathHandler(ctx))
                        .build()

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            Log.d("Ana3D_WebConsole", "${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            val path = request.url.path.orEmpty()
                            if (path.endsWith(".glb", ignoreCase = true) || path.endsWith(".gltf", ignoreCase = true)) {
                                val fileName = request.url.lastPathSegment ?: "companion.glb"
                                val extFile = java.io.File(ctx.getExternalFilesDir(null), "models/$fileName")
                                if (extFile.exists() && extFile.isFile) {
                                    return WebResourceResponse("model/gltf-binary", null, extFile.inputStream())
                                }
                                val intFile = java.io.File(ctx.filesDir, "models/$fileName")
                                if (intFile.exists() && intFile.isFile) {
                                    return WebResourceResponse("model/gltf-binary", null, intFile.inputStream())
                                }
                            }
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isModelReady = true
                            executeJs("window.Ana3D && window.Ana3D.toggleChat && window.Ana3D.toggleChat(false);")
                            executeJs("window.Ana3D && window.Ana3D.toggleTelemetry && window.Ana3D.toggleTelemetry(false);")
                        }
                    }

                    val bridge = Android3DBridge(
                        onSend = { text ->
                            scope.launch { onSend(text) {} }
                        },
                        onModelLoaded = { label, morphCount ->
                            modelStatus = "$label ($morphCount blendshapes)"
                            isModelReady = true
                        },
                        onTouchMesh = { part ->
                            triggerHaptic(45)
                        },
                        onDeleteMessage = { messageId ->
                            scope.launch(Dispatchers.Main) {
                                onDeleteMessage(messageId)
                            }
                        },
                        onPlayVoiceReply = { msgId, text ->
                            scope.launch(Dispatchers.IO) {
                                val base64Audio = repository.synthesizeVoiceBase64(profile, text)
                                if (base64Audio != null) {
                                    val audioJs = buildString {
                                        append("window.Ana3D && window.Ana3D.playAudio(")
                                        append(JSONObject.quote(base64Audio)).append(", ")
                                        append(JSONObject.quote(text)).append(");")
                                    }
                                    executeJs(audioJs)
                                }
                            }
                        },
                        onStopVoice = {
                            executeJs("window.Ana3D && window.Ana3D.stopVoice && window.Ana3D.stopVoice();")
                        }
                    )
                    addJavascriptInterface(bridge, "AndroidHost")

                    loadUrl("https://appassets.androidplatform.net/assets/3d_stage/index.html")
                    webViewInstance = this
                }
            }
        )

        // 2. Single Unified Top Control HUD
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            color = Color(0xCC111827),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Online Status & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Ana 🌸",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (sending) "Thinking..." else "3D Live",
                            color = Color(0xBBFFFFFF),
                            fontSize = 9.sp
                        )
                    }
                }

                // Right: Clean quick action pills
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Hide / Unhide Left Chat Drawer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChatDrawerOpen) Color(0xFF25D366).copy(alpha = 0.25f) else Color(0x22FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChatDrawerOpen) Color(0xFF25D366) else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.clickable {
                            triggerHaptic(30)
                            val next = !isChatDrawerOpen
                            isChatDrawerOpen = next
                            executeJs("window.Ana3D && window.Ana3D.toggleChat && window.Ana3D.toggleChat($next);")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💬", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isChatDrawerOpen) "Hide" else "Chat",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 2. Hide / Unhide Right Live Expression Panel
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTelemetryOpen) Color(0xFFEC4899).copy(alpha = 0.25f) else Color(0x22FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTelemetryOpen) Color(0xFFEC4899) else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.clickable {
                            triggerHaptic(30)
                            val next = !isTelemetryOpen
                            isTelemetryOpen = next
                            executeJs("window.Ana3D && window.Ana3D.toggleTelemetry && window.Ana3D.toggleTelemetry($next);")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📊", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isTelemetryOpen) "Hide" else "Face",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 3. Dance Button
                    IconButton(
                        onClick = {
                            triggerHaptic(30)
                            executeJs("window.Ana3D && window.Ana3D.toggleDance && window.Ana3D.toggleDance();")
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Text("💃", fontSize = 14.sp)
                    }

                    // 4. Camera Zoom Button
                    IconButton(
                        onClick = {
                            triggerHaptic(30)
                            activeCameraPresetIndex = (activeCameraPresetIndex + 1) % cameraPresets.size
                            val preset = cameraPresets[activeCameraPresetIndex]
                            executeJs("window.Ana3D && window.Ana3D.setCameraPreset && window.Ana3D.setCameraPreset('$preset');")
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Text("🎥", fontSize = 14.sp)
                    }

                    // 5. Flip 180°
                    IconButton(
                        onClick = {
                            triggerHaptic(30)
                            executeJs("window.Ana3D && window.Ana3D.flip180 && window.Ana3D.flip180();")
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Text("🔄", fontSize = 14.sp)
                    }
                }
            }
        }

        // 3. Floating Bottom Chat & Subtitle Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Dismissible Spoken Subtitle Card
            if (latestAssistant != null && latestAssistant.content.isNotBlank() && isSubtitleVisible) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    color = Color(0xD0181E2A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33EC4899))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌸",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = latestAssistant.content,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { isSubtitleVisible = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("✕", color = Color(0x88FFFFFF), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xEE111827),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Document Attachment
                    IconButton(
                        onClick = { documentLauncher.launch("*/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_attach),
                            contentDescription = "Attach File",
                            tint = Color(0xCCFFFFFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Text Field
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Talk with Ana in 3D...", fontSize = 13.sp, color = Color(0x88FFFFFF)) },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        )
                    )

                    // Voice Input Mic Button
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Ana...")
                            }
                            speechLauncher.launch(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mic),
                            contentDescription = "Voice Input",
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            val trimmed = input.trim()
                            if (trimmed.isNotBlank()) {
                                onSend(trimmed) {}
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !sending,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_send),
                                contentDescription = "Send",
                                tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0x44FFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
