package com.miyu.reader.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExternalJsPluginDefinition(
    val pluginId: String,
    val providerScope: String,
    val startUrl: String,
    val bootstrapScript: String,
    val bridgeObjectName: String,
    val androidBridgeName: String,
)

@Singleton
class ExternalJsPluginRuntime @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    // External sources run inside a hidden WebView so they can reuse browser cookies,
    // site-side JS, and challenge flows that plain HTTP parsers cannot satisfy.
    suspend fun execute(
        definition: ExternalJsPluginDefinition,
        requestType: String,
        payload: JSONObject = JSONObject(),
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        onProgress: ((JSONObject) -> Unit)? = null,
    ): JSONObject = withTimeout(timeoutMs) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val finished = AtomicBoolean(false)
                val requestId = UUID.randomUUID().toString()
                val request = JSONObject()
                    .put("id", requestId)
                    .put("type", requestType)
                    .put("payload", payload)

                @SuppressLint("SetJavaScriptEnabled")
                val webView = WebView(appContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadsImagesAutomatically = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setSupportMultipleWindows(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }
                }

                fun destroyWebView() {
                    webView.stopLoading()
                    webView.removeAllViews()
                    webView.destroy()
                }

                fun finish(result: Result<JSONObject>) {
                    if (!finished.compareAndSet(false, true)) return
                    destroyWebView()
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) },
                    )
                }

                val bridge = object {
                    private var requestSent = false

                    @JavascriptInterface
                    fun postMessage(rawMessage: String) {
                        if (finished.get()) return
                        runCatching { JSONObject(rawMessage) }
                            .onSuccess { message ->
                                val scope = message.optString("scope")
                                val providerId = message.optString("providerId")
                                val scopeMatches = scope.isBlank() || scope == definition.providerScope
                                val providerMatches = providerId.isBlank() || providerId == definition.providerScope
                                if (!scopeMatches || !providerMatches) {
                                    return@onSuccess
                                }
                                when (message.optString("type")) {
                                    "ready" -> dispatchRequest()
                                    "challenge" -> finish(
                                        Result.failure(
                                            IllegalStateException(
                                                "Verification is required before ${definition.pluginId} can run. Open the verifier and retry.",
                                            ),
                                        ),
                                    )

                                    "progress" -> {
                                        if (message.optString("id") == requestId) {
                                            onProgress?.invoke(message.optJSONObject("payload") ?: JSONObject())
                                        }
                                    }

                                    "result" -> {
                                        if (message.optString("id") == requestId) {
                                            finish(Result.success(message.optJSONObject("payload") ?: JSONObject()))
                                        }
                                    }

                                    "error" -> {
                                        if (message.optString("id") == requestId) {
                                            val errorMessage = message.optString("error")
                                                .ifBlank { "Plugin request failed." }
                                            finish(Result.failure(IllegalStateException(errorMessage)))
                                        }
                                    }
                                }
                            }
                            .onFailure {
                                finish(Result.failure(IllegalStateException("Plugin bridge returned malformed data.")))
                            }
                    }

                    private fun dispatchRequest() {
                        if (requestSent || finished.get()) return
                        requestSent = true
                        val encodedRequest = JSONObject.quote(request.toString())
                        val js = "window.${definition.bridgeObjectName}.run(JSON.parse($encodedRequest));true;"
                        webView.evaluateJavascript(js, null)
                    }
                }

                continuation.invokeOnCancellation {
                    if (finished.compareAndSet(false, true)) {
                        destroyWebView()
                    }
                }

                webView.addJavascriptInterface(bridge, definition.androidBridgeName)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (finished.get()) return
                        webView.evaluateJavascript(definition.bootstrapScript, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            finish(
                                Result.failure(
                                    IllegalStateException(
                                        error?.description?.toString().orEmpty().ifBlank {
                                            "Plugin runtime could not load ${definition.startUrl}."
                                        },
                                    ),
                                ),
                            )
                        }
                    }
                }
                webView.loadUrl(definition.startUrl)
            }
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 120_000L
    }
}
