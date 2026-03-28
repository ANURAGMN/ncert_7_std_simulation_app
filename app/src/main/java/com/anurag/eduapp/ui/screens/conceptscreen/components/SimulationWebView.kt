package com.anurag.eduapp.ui.screens.conceptscreen.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** WebView component for rendering simulation HTML */
@Composable
fun SimulationWebView(
    url: String,
    modifier: Modifier = Modifier,
    onPageLoaded: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onPageLoaded()
                    }
                }
                loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
