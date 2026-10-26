package tg.tontinepro.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        webView.webViewClient = WebViewClient()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        setContentView(webView)

        webView.loadUrl(
            "file:///android_asset/index.html"
        )
    }
}
