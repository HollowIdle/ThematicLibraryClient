package com.example.thematiclibraryclient.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PdfJsReaderScreen(
    filePath: String,
    initialPage: Int,
    onQuoteCreated: (String) -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/app_files/", WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir))
            .build()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onTextSelected(text: String) {
                        if (text.isNotBlank()) onQuoteCreated(text)
                    }
                    @JavascriptInterface
                    fun onPageChanged(page: Int) {
                        onPageChanged(page - 1)
                    }

                    @JavascriptInterface
                    fun onError(message: String) {
                        android.util.Log.e("PDF_JS", "Error: $message")
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request!!.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        injectSelectionScript(view)
                        injectPageChangeListener(view)
                        injectErrorListener(view)
                    }
                }

                val relativePath = File(filePath).relativeTo(context.filesDir).path

                val pdfVirtualUrl = "https://appassets.androidplatform.net/app_files/$relativePath"

                val viewerVirtualUrl = "https://appassets.androidplatform.net/assets/pdfjs/web/viewer.html"

                val finalUrl = "$viewerVirtualUrl?file=${Uri.encode(pdfVirtualUrl)}#page=${initialPage + 1}"

                loadUrl(finalUrl)
            }
        },
        update = { view -> webView = view }
    )
}

private fun injectSelectionScript(webView: WebView?) {
    val js = """
        document.addEventListener("mouseup", function() {
            var selection = window.getSelection().toString();
            if (selection.length > 0) {
                AndroidBridge.onTextSelected(selection);
            }
        });
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

private fun injectPageChangeListener(webView: WebView?) {
    val js = """
        var checkExist = setInterval(function() {
           if (window.PDFViewerApplication && window.PDFViewerApplication.eventBus) {
              clearInterval(checkExist);
              window.PDFViewerApplication.eventBus.on('pagechanging', function(evt) {
                  AndroidBridge.onPageChanged(evt.pageNumber);
              });
           }
        }, 100);
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

private fun injectErrorListener(webView: WebView?) {
    val js = """
        window.onerror = function(message, source, lineno, colno, error) {
            AndroidBridge.onError(message);
        };
        
        var checkApp = setInterval(function() {
           if (window.PDFViewerApplication && window.PDFViewerApplication.eventBus) {
              clearInterval(checkApp);
              window.PDFViewerApplication.eventBus.on('error', function(evt) {
                  AndroidBridge.onError(evt.message);
              });
           }
        }, 100);
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}