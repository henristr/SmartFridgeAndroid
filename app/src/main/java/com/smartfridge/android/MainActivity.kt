package com.smartfridge.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.EditText

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: android.content.SharedPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            webView.reload()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    request.grant(request.resources)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        checkDomain()
    }

    private fun checkDomain() {
        val savedDomain = prefs.getString("domain", null)

        if (savedDomain == null) {
            askForDomain()
        } else {
            webView.loadUrl(savedDomain)
        }
    }

    private fun askForDomain() {
        val input = EditText(this)
        input.hint = "https://example.com"

        AlertDialog.Builder(this)
            .setTitle("Domain eingeben")
            .setMessage("Bitte gib die Domain deiner SmartFridge Website ein:")
            .setView(input)
            .setCancelable(false)

            .setPositiveButton("Speichern") { _, _ ->
                var domain = input.text.toString().trim()

                if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                    domain = "https://$domain"
                }

                prefs.edit().putString("domain", domain).apply()
                webView.loadUrl(domain)
            }

            .setNeutralButton("Ich habe noch keine laufende Installation") { _, _ ->
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://smartfridge.henristr.de")
                )
                startActivity(intent)
            }

            .show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}