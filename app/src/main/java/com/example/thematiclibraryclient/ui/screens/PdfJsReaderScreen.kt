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

class PdfJsBridge {
    @Volatile
    var onSelectionCallback: ((String?) -> Unit)? = null

    @Volatile
    var onPageCallback: ((Int) -> Unit)? = null

    @Volatile
    var isSelectionMode: Boolean = false

    @JavascriptInterface
    fun onSelectionChange(text: String, hasSelection: Boolean) {
        if (isSelectionMode) {
            onSelectionCallback?.invoke(if (hasSelection && text.isNotBlank()) text else null)
        } else {
            onSelectionCallback?.invoke(null)
        }
    }

    @JavascriptInterface
    fun onPageChanged(page: Int) {
        onPageCallback?.invoke(page - 1)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun PdfJsReaderScreen(
    filePath: String,
    currentPage: Int,
    requestedPage: Int?,
    isInSelectionMode: Boolean,
    onSelectionChanged: (String?) -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    val bridge = remember { PdfJsBridge() }

    SideEffect {
        bridge.onSelectionCallback = onSelectionChanged
        bridge.onPageCallback = onPageChanged
        bridge.isSelectionMode = isInSelectionMode
    }

    LaunchedEffect(requestedPage) {
        if (requestedPage != null && webView != null) {
            if (currentPage != requestedPage) {
                val targetPage = requestedPage + 1
                val js = """
                    if(window.PDFViewerApplication) { 
                        window.PDFViewerApplication.page = $targetPage; 
                    }
                """
                webView?.evaluateJavascript(js, null)
            }
        }
    }

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

                clearCache(true)
                android.webkit.WebStorage.getInstance().deleteAllData()

                addJavascriptInterface(bridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request!!.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        val cssInjection = """
                            var style = document.createElement('style');
                            style.innerHTML = `
                                #openFile, 
                                #print, 
                                #download, 
                                #viewBookmark, 
                                #editorFreeText, 
                                #editorInk, 
                                #secondaryToolbarToggle { 
                                    display: none !important; 
                                }
                                .toolbarViewerLeft { margin-left: 0 !important; }
                              
                              
                                body:not(.selection-mode-enabled) .textLayer {
                                    pointer-events: none !important;
                                    user-select: none !important;
                                    -webkit-user-select: none !important;
                                }
                            `;
                            document.head.appendChild(style);
                        """.trimIndent()
                        view?.evaluateJavascript(cssInjection, null)

                        val jsListeners = """
                            document.addEventListener("selectionchange", function() {
                                var selection = window.getSelection().toString();
                                var hasSelection = selection.length > 0;
                                AndroidBridge.onSelectionChange(selection, hasSelection);
                            });

                            var checkExist = setInterval(function() {
                               if (window.PDFViewerApplication && window.PDFViewerApplication.eventBus) {
                                  clearInterval(checkExist);
                                  window.PDFViewerApplication.eventBus.on('pagechanging', function(evt) {
                                      AndroidBridge.onPageChanged(evt.pageNumber);
                                  });
                               }
                            }, 100);
                        """.trimIndent()
                        view?.evaluateJavascript(jsListeners, null)
                    }
                }

                val relativePath = File(filePath).relativeTo(context.filesDir).path
                val pdfVirtualUrl = "https://appassets.androidplatform.net/app_files/$relativePath"
                val viewerVirtualUrl = "https://appassets.androidplatform.net/assets/pdfjs/web/viewer.html"

                val startPage = currentPage + 1
                val finalUrl = "$viewerVirtualUrl?file=${Uri.encode(pdfVirtualUrl)}#page=$startPage"

                loadUrl(finalUrl)
            }
        },
        update = { view ->
            webView = view

            if (isInSelectionMode) {
                view.evaluateJavascript("document.body.classList.add('selection-mode-enabled');", null)
            } else {
                view.evaluateJavascript("document.body.classList.remove('selection-mode-enabled');", null)
                view.evaluateJavascript("window.getSelection().removeAllRanges();", null)
            }
        }
    )
}