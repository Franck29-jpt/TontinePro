package tg.tontinepro.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private var fileCallback:
        ValueCallback<Array<Uri>>? = null

    private var pendingDocument: ByteArray? = null

    private val chooserRequestCode = 1001
    private val saveRequestCode = 1002

    private inner class DocumentBridge {

        @JavascriptInterface
        fun enregistrerFichier(
            nom: String,
            type: String,
            base64: String
        ) {
            if (base64.length > 12_000_000) return

            val bytes = try {
                Base64.decode(base64, Base64.DEFAULT)
            } catch (_: Exception) {
                return
            }

            if (
                bytes.isEmpty() ||
                bytes.size > 8 * 1024 * 1024
            ) return

            runOnUiThread {

                if (pendingDocument != null) {
                    return@runOnUiThread
                }

                pendingDocument = bytes

                val mime = if (
                    type.matches(
                        Regex(
                            "image/(jpeg|png|webp)|application/pdf"
                        )
                    )
                ) {
                    type
                } else {
                    "application/octet-stream"
                }

                val intent = Intent(
                    Intent.ACTION_CREATE_DOCUMENT
                ).apply {
                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )
                    setType(mime)
                    putExtra(
                        Intent.EXTRA_TITLE,
                        nom.take(120).ifBlank {
                            "document"
                        }
                    )
                }

                try {
                    startActivityForResult(
                        intent,
                        saveRequestCode
                    )
                } catch (_: Exception) {
                    pendingDocument = null
                    Toast.makeText(
                        this@MainActivity,
                        "Impossible d'ouvrir l'enregistrement",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = true

        val assetLoader =
            WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    WebViewAssetLoader.AssetsPathHandler(
                        this
                    )
                )
                .build()

        webView.webViewClient =
            object : WebViewClientCompat() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url

                    return !(
                        url.scheme == "https" &&
                        url.host ==
                        "appassets.androidplatform.net"
                    )
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader
                        .shouldInterceptRequest(
                            request.url
                        )
                }
            }

        webView.addJavascriptInterface(
            DocumentBridge(),
            "TontineAndroid"
        )

        webView.webChromeClient =
            object : WebChromeClient() {

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback:
                        ValueCallback<Array<Uri>>?,
                    fileChooserParams:
                        FileChooserParams?
                ): Boolean {

                    fileCallback?.onReceiveValue(null)
                    fileCallback = filePathCallback

                    return try {

                        val intent =
                            fileChooserParams
                                ?.createIntent()
                                ?: Intent(
                                    Intent.ACTION_GET_CONTENT
                                ).apply {
                                    type = "*/*"
                                    addCategory(
                                        Intent.CATEGORY_OPENABLE
                                    )
                                }

                        startActivityForResult(
                            intent,
                            chooserRequestCode
                        )

                        true

                    } catch (_: Exception) {

                        fileCallback?.onReceiveValue(null)
                        fileCallback = null

                        true
                    }
                }
            }

        webView.loadUrl(
            "https://appassets.androidplatform.net/assets/index.html"
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == chooserRequestCode) {

            fileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams
                    .parseResult(
                        resultCode,
                        data
                    )
            )

            fileCallback = null

        } else if (
            requestCode == saveRequestCode
        ) {

            val bytes = pendingDocument
            pendingDocument = null

            if (
                resultCode == RESULT_OK &&
                data?.data != null &&
                bytes != null
            ) {
                try {

                    contentResolver
                        .openOutputStream(
                            data.data!!
                        )
                        ?.use {
                            it.write(bytes)
                        }
                        ?: throw IllegalStateException(
                            "Destination inaccessible"
                        )

                    Toast.makeText(
                        this,
                        "Document enregistré",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (_: Exception) {

                    Toast.makeText(
                        this,
                        "Échec de l'enregistrement",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
