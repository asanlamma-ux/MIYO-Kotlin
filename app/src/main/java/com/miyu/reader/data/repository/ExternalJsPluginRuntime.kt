package com.miyu.reader.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayInputStream
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
    val capabilities: Set<String>,
    val allowedHosts: Set<String>,
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
        require(definition.supportsRequest(requestType)) {
            "${definition.pluginId} does not support the $requestType JavaScript capability."
        }
        require(definition.isAllowedUrl(definition.startUrl)) {
            "${definition.pluginId} start URL is outside the package host allow-list."
        }
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
                                                "Verification is required before ${definition.pluginId} can run. " +
                                                    "Open the verifier and retry.",
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
                                        val messageId = message.optString("id")
                                        if (messageId == requestId || messageId == BOOTSTRAP_ERROR_ID || messageId.isBlank()) {
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
                        if (!definition.isAllowedUrl(url)) {
                            finish(Result.failure(IllegalStateException("Plugin attempted to navigate outside its allowed hosts.")))
                            return
                        }
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (finished.get()) return
                        if (!definition.isAllowedUrl(url)) {
                            finish(Result.failure(IllegalStateException("Plugin attempted to run outside its allowed hosts.")))
                            return
                        }
                        webView.evaluateJavascript(definition.bootstrapInjectionScript(), null)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString()
                        if (requestUrl != null && !definition.isAllowedUrl(requestUrl)) {
                            if (request?.isForMainFrame == true) {
                                finish(Result.failure(IllegalStateException("Plugin attempted to leave its allowed hosts.")))
                            }
                            return true
                        }
                        return false
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url != null && !definition.isAllowedUrl(url)) {
                            finish(Result.failure(IllegalStateException("Plugin attempted to leave its allowed hosts.")))
                            return true
                        }
                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString()
                        if (requestUrl != null && !definition.isAllowedUrl(requestUrl)) {
                            if (request?.isForMainFrame == true) {
                                webView.post {
                                    finish(Result.failure(IllegalStateException("Plugin attempted to load an untrusted host.")))
                                }
                            }
                            return blockedResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                        if (url != null && !definition.isAllowedUrl(url)) {
                            return blockedResponse()
                        }
                        return super.shouldInterceptRequest(view, url)
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
        const val BOOTSTRAP_ERROR_ID = "__bootstrap"
    }
}

private fun ExternalJsPluginDefinition.supportsRequest(requestType: String): Boolean =
    requestType.trim().lowercase() in capabilities

private fun ExternalJsPluginDefinition.isAllowedUrl(rawUrl: String?): Boolean {
    if (rawUrl.isNullOrBlank()) return false
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase().orEmpty()
    if (scheme == "about" || scheme == "data") return true
    if (scheme != "https") return false
    val host = uri.host?.lowercase()?.removePrefix("www.")?.trimEnd('.') ?: return false
    return allowedHosts.any { allowedHost ->
        val normalized = allowedHost.lowercase().removePrefix("www.").trimEnd('.')
        host == normalized || host.endsWith(".$normalized")
    }
}

private fun ExternalJsPluginDefinition.bootstrapInjectionScript(): String {
    val encodedScript = Base64.encodeToString(bootstrapScript.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val encodedScope = JSONObject.quote(providerScope)
    val encodedBridgeName = JSONObject.quote(androidBridgeName)
    val encodedErrorId = JSONObject.quote("__bootstrap")
    return """
        (function () {
          var bridgeName = $encodedBridgeName;
          try {
            var script = decodeURIComponent(escape(atob(${JSONObject.quote(encodedScript)})));
            (0, eval)(script);
          } catch (error) {
            var bridge = window[bridgeName];
            if (bridge && bridge.postMessage) {
              bridge.postMessage(JSON.stringify({
                type: "error",
                id: $encodedErrorId,
                scope: $encodedScope,
                providerId: $encodedScope,
                error: "Plugin bootstrap failed: " + (error && error.message ? error.message : String(error))
              }));
            }
          }
          return true;
        })();
    """.trimIndent()
}

private fun blockedResponse(): WebResourceResponse =
    WebResourceResponse(
        "text/plain",
        "utf-8",
        ByteArrayInputStream(ByteArray(0)),
    )
