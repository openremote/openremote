package io.openremote.orlib.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.webkit.ConsoleMessage.MessageLevel
import android.webkit.WebView.WebViewTransport
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import io.openremote.orlib.ORConstants
import io.openremote.orlib.ORConstants.BASE_URL_KEY
import io.openremote.orlib.ORConstants.CLEAR_URL
import io.openremote.orlib.R
import io.openremote.orlib.databinding.ActivityOrMainBinding
import io.openremote.orlib.service.GeofenceProvider
import io.openremote.orlib.service.QrScannerProvider
import org.json.JSONException
import org.json.JSONObject
import java.util.logging.Level
import java.util.logging.Logger


open class OrMainActivity : Activity() {

    private val LOG = Logger.getLogger(
        OrMainActivity::class.java.name
    )

    private val locationResponseCode = 555
    private var locationCallback: GeolocationPermissions.Callback? = null;
    private  var locationOrigin: String?  = null;

    private lateinit var binding: ActivityOrMainBinding
    private lateinit var sharedPreferences: SharedPreferences


    private var mapper = jacksonObjectMapper()
    private val connectivityChangeReceiver: ConnectivityChangeReceiver =
        ConnectivityChangeReceiver()
    private var timeOutHandler: Handler? = null
    private var timeOutRunnable: Runnable? = null
    private var progressBar: ProgressBar? = null
    private var webViewTimeout = ORConstants.WEBVIEW_LOAD_TIMEOUT_DEFAULT
    private var webViewLoaded = false
    private var geofenceProvider: GeofenceProvider? = null
    private var qrScannerProvider: QrScannerProvider? = null
    private var consoleId: String? = null
    private var connectFailCount: Int = 0
    private var connectFailResetHandler: Handler? = null
    private var connectFailResetRunnable: Runnable? = null
    private var baseUrl: String? = null
    private var onDownloadCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                Toast.makeText(applicationContext, R.string.download_completed, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        try {
            val timeoutStr = getString(R.string.OR_CONSOLE_LOAD_TIMEOUT)
            webViewTimeout = timeoutStr.toInt()
        } catch (nfe: NumberFormatException) {
            println("Could not parse console load timeout value: $nfe")
        }

        // Enable remote debugging of WebView from Chrome Debugger tools
        if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
            LOG.info("Enabling remote debugging")
            WebView.setWebContentsDebuggingEnabled(true)
        }

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            initializeWebView()
        }

        progressBar = findViewById(R.id.webProgressBar)
        progressBar?.max = 100
        progressBar?.progress = 1


        if (intent.hasExtra(ORConstants.BASE_URL_KEY)) {
            baseUrl = intent.getStringExtra(ORConstants.BASE_URL_KEY)
        }

        if (intent.hasExtra(BASE_URL_KEY)) {
            baseUrl = intent.getStringExtra(BASE_URL_KEY)
        }

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            val host = sharedPreferences.getString(ORConstants.HOST_KEY, null)
            val realm = sharedPreferences.getString(ORConstants.REALM_KEY, null)


                    openIntentUrl(intent)


    }

    override fun onNewIntent(intent: Intent) {
        openIntentUrl(intent)
    }

    private fun openIntentUrl(intent: Intent) {
        when {
            intent.hasExtra("appUrl") -> {
                val url = intent.getStringExtra("appUrl")!!
                LOG.fine("Loading web view: $url")
                loadUrl(url)
            }
            else -> {
                var url = baseUrl
                val intentUrl = intent.getStringExtra("appUrl")
                if (intentUrl != null) {
                    url = if (intentUrl.startsWith("http") || intentUrl.startsWith("https")) {
                        intentUrl
                    } else {
                        url + intentUrl
                    }
                }
                LOG.fine("Loading web view: $url")
                loadUrl(url!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            connectivityChangeReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
        registerReceiver(
            onDownloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(connectivityChangeReceiver)
        unregisterReceiver(onDownloadCompleteReceiver)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.webView.saveState(outState)
    }

    private fun reloadWebView() {
        var url = binding.webView.url
        if ("about:blank" == url) {
            url = baseUrl
            LOG.fine("Reloading web view: $url")
            loadUrl(url!!)
        }
    }



    @SuppressLint("SetJavaScriptEnabled")
    fun initializeWebView() {
        LOG.fine("Initializing web view")
        val webAppInterface = WebAppInterface(this)
        binding.webView.apply {
            addJavascriptInterface(webAppInterface, "MobileInterface")
            settings.javaScriptEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportMultipleWindows(true)
            webViewClient = object : WebViewClient() {
                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse
                ) {
                    //When initialising Keycloak with an invalid offline refresh token (e.g. wrong nonce because
                    // server was reinstalled), we detect the failure and then don't show an error view. We clear the stored
                    // invalid token. The web app will then start a new login.
                    if (request.url.lastPathSegment != null && request.url.lastPathSegment == "token" && request.method == "POST" && errorResponse.statusCode == 400) {
                        storeData(getString(R.string.SHARED_PREF_REFRESH_TOKEN), null)
                        return
                    }

                    handleError(
                        errorResponse.statusCode,
                        errorResponse.reasonPhrase,
                        request.url.toString()
                    )
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    // Remote debugging session from Chrome wants to load about:blank and then fails with "ERROR_UNSUPPORTED_SCHEME", ignore
                    if ("net::ERR_CACHE_MISS".contentEquals(error.description)) {
                        return
                    }
                    if (request.url.toString() == "about:blank" && error.errorCode == ERROR_UNSUPPORTED_SCHEME) {
                        return
                    }
                    handleError(
                        error.errorCode,
                        error.description.toString(),
                        request.url.toString()
                    )
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    progressBar!!.visibility = View.VISIBLE
                    timeOutRunnable = Runnable {
                        if (!webViewLoaded) {
                            handleError(ERROR_TIMEOUT, "Connection timed out", url)
                        }
                    }
                    timeOutHandler = Looper.myLooper()?.let { Handler(it) }
                    timeOutHandler!!.postDelayed(timeOutRunnable!!, 5000)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    webViewLoaded = true
                    progressBar!!.visibility = View.GONE
                    timeOutRunnable?.let { timeOutHandler!!.removeCallbacks(it) }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    if (request.url.scheme.equals("webbrowser", ignoreCase = true)) {
                        val newUrl = request.url.buildUpon().scheme("https").build().toString()
                        val i = Intent(Intent.ACTION_VIEW)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.data = Uri.parse(newUrl)
                        startActivity(i)
                        return true
                    }
                    if (!request.url.isAbsolute && baseUrl?.isNotEmpty()!!) {
                        view.loadUrl("${baseUrl}/${request.url}")
                        return true
                    }

                    return super.shouldOverrideUrlLoading(view, request)
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {

                    if (view == binding.webView && detail?.didCrash() == true) {
                        onCreate(null)
                        return true
                    }

                    return super.onRenderProcessGone(view, detail)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    val msg =
                        "WebApp console (" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "): " + consoleMessage.message()
                    when (consoleMessage.messageLevel()) {
                        MessageLevel.DEBUG, MessageLevel.TIP -> LOG.fine(msg)
                        MessageLevel.LOG -> LOG.info(msg)
                        else -> LOG.severe(msg)
                    }
                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        callback?.invoke(origin, true, false)
                    } else {
                        locationCallback = callback
                        locationOrigin = origin
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ),
                            locationResponseCode
                        )
                    }
                }

                override fun onProgressChanged(view: WebView, progress: Int) {
                    progressBar!!.progress = progress
                }

                override fun onCreateWindow(
                    view: WebView,
                    dialog: Boolean,
                    userGesture: Boolean,
                    resultMsg: Message
                ): Boolean {
                    val newWebView = WebView(this@OrMainActivity)
                    view.addView(newWebView)
                    val transport = resultMsg.obj as WebViewTransport
                    transport.webView = newWebView
                    resultMsg.sendToTarget()
                    newWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val browserIntent = Intent(Intent.ACTION_VIEW)
                            browserIntent.data = request.url
                            startActivity(browserIntent)
                            return true
                        }
                    }
                    return true
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(
                        this@OrMainActivity,
                        writePermission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Write permission has not been granted yet, request it.
                    requestPermissions(
                        arrayOf(writePermission),
                        ORConstants.WRITE_PERMISSION_FOR_DOWNLOAD
                    )
                } else {
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setMimeType(mimetype)
                    //------------------------COOKIE!!------------------------
                    val cookies = CookieManager.getInstance().getCookie(url)
                    request.addRequestHeader("cookie", cookies)
                    //------------------------COOKIE!!------------------------
                    request.addRequestHeader("User-Agent", userAgent)
                    request.setDescription("Downloading file...")
                    request.setTitle(
                        URLUtil.guessFileName(
                            url,
                            contentDisposition,
                            mimetype
                        )
                    )
                    request.allowScanningByMediaScanner()
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimetype)
                    )
                    val dm =
                        getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    Toast.makeText(applicationContext, R.string.downloading_file, Toast.LENGTH_LONG)
                        .show()
                    dm.enqueue(request)
                }
            }
        }
    }

    private fun handleError(
        errorCode: Int,
        description: String,
        failingUrl: String?
    ) {
        LOG.warning("Error requesting '$failingUrl': $errorCode($description)")
        //TODO should we always ignore image errors and locale json files?
        if (failingUrl != null && (failingUrl.endsWith("png")
                    || failingUrl.endsWith("jpg")
                    || failingUrl.endsWith("ico")
                    || failingUrl.contains("locales")
                    || failingUrl.contains( "consoleappconfig"))
        ) {
            LOG.info("Ignoring error loading image resource")
            return
        }
        // This will be the URL loaded into the webview itself (false for images etc. of the main page)
        // Check page load error URL
        if (errorCode >= 500) {
            if (baseUrl != null && baseUrl != failingUrl && connectFailCount < 10) {
                loadUrl(baseUrl!!)
                Toast.makeText(this, description, Toast.LENGTH_SHORT).show()
                connectFailCount++
                connectFailResetRunnable?.let { connectFailResetHandler!!.removeCallbacks(it) }
                connectFailResetRunnable = Runnable {
                    connectFailCount = 0
                }
                connectFailResetHandler = Looper.myLooper()?.let { Handler(it) }
                connectFailResetHandler!!.postDelayed(connectFailResetRunnable!!, 5000)
            } else {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.putExtra(CLEAR_URL, baseUrl)
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(launchIntent)
                    finish()
                }
                Toast.makeText(applicationContext, "The main page couldn't be opened", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun loadUrl(url: String) {
        webViewLoaded = false
        val encodedUrl = url.replace(" ", "%20")
        binding.webView.loadUrl(encodedUrl)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == ORConstants.WRITE_PERMISSION_FOR_DOWNLOAD) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, R.string.downloading_file, Toast.LENGTH_LONG)
                    .show()
            }
        } else if (requestCode == GeofenceProvider.locationResponseCode) {
            geofenceProvider?.onRequestPermissionsResult(this)
        } else if (requestCode == locationResponseCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationCallback?.invoke(locationOrigin, true, false)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QrScannerProvider.REQUEST_SCAN_QR) {
            val qrResult = data?.getStringExtra("result")

            val response = hashMapOf(
                "action" to "SCAN_QR",
                "provider" to "qr",
                "data" to hashMapOf("result" to qrResult)
            )
            notifyClient(response)
        }
    }

    private inner class WebAppInterface(
        private val activity: Activity
    ) {
        @JavascriptInterface
        @Throws(JSONException::class)
        public fun postMessage(jsonMessage: String) {
            val reader = JSONObject(jsonMessage)
            val messageType = reader.getString("type")
            val data = reader.optJSONObject("data")
            LOG.info("Received WebApp message: $data")

            when (messageType) {
                "error" -> {
                    LOG.fine("Received WebApp message, error: " + data?.getString("error"))
                    Toast.makeText(
                        this@OrMainActivity,
                        "Error occurred ${data?.getString("error")}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                "provider" -> {
                    data?.let {
                        val action = it.getString("action")
                        if (!action.isNullOrEmpty()) {
                            val provider = it.getString("provider")
                            when {
                                provider.equals("geofence", ignoreCase = true) -> {
                                    handleGeofenceProviderMessage(it)
                                }
                                provider.equals("push", ignoreCase = true) -> {
                                    handlePushProviderMessage(it)
                                }
                                provider.equals("storage", ignoreCase = true) -> {
                                    handleStorageProviderMessage(it)
                                }
                                provider.equals("qr", ignoreCase = true) -> {
                                    handleQrScannerProviderMessage(it)
                                }
                            }
                        }
                    }
                }
            }
        }

        @Throws(JSONException::class)
        private fun handleGeofenceProviderMessage(data: JSONObject) {
            val action = data.getString("action")
            if (geofenceProvider == null) {
                geofenceProvider = GeofenceProvider(activity)
            }
            when {
                action.equals("PROVIDER_INIT", ignoreCase = true) -> {
                    val initData: Map<String, Any> = geofenceProvider!!.initialize()
                    notifyClient(initData)
                }
                action.equals("PROVIDER_ENABLE", ignoreCase = true) -> {
                    val consoleId = data.getString("consoleId")
                    (activity as OrMainActivity).consoleId = consoleId
                    geofenceProvider?.enable(this@OrMainActivity, baseUrl ?: "",
                        consoleId, object : GeofenceProvider.GeofenceCallback {
                            override fun accept(responseData: Map<String, Any>) {
                                notifyClient(responseData)
                            }
                        })
                }
                action.equals("PROVIDER_DISABLE", ignoreCase = true) -> {
                    geofenceProvider?.disable()
                    val response: MutableMap<String, Any> = HashMap()
                    response["action"] = "PROVIDER_DISABLE"
                    response["provider"] = "geofence"
                    notifyClient(response)
                }
                action.equals("GEOFENCE_REFRESH", ignoreCase = true) -> {
                    geofenceProvider?.refreshGeofences()
                }
                action.equals("GET_LOCATION", ignoreCase = true) -> {
                    geofenceProvider?.getLocation(
                        this@OrMainActivity,
                        object : GeofenceProvider.GeofenceCallback {
                            override fun accept(responseData: Map<String, Any>) {
                                notifyClient(responseData)
                            }
                        })
                }
            }
        }

        @Throws(JSONException::class)
        private fun handlePushProviderMessage(data: JSONObject) {
            val action = data.getString("action")
            when {
                action.equals("PROVIDER_INIT", ignoreCase = true) -> {
                    // Push permission is covered by the INTERNET permission and is not a runtime permission
                    val response: MutableMap<String, Any> = HashMap()
                    response["action"] = "PROVIDER_INIT"
                    response["provider"] = "push"
                    response["version"] = "fcm"
                    response["enabled"] = false
                    response["disabled"] = sharedPreferences.contains(ORConstants.PUSH_PROVIDER_DISABLED_KEY)
                    response["requiresPermission"] = false
                    response["hasPermission"] = true
                    response["success"] = true
                    notifyClient(response)
                }
                action.equals("PROVIDER_ENABLE", ignoreCase = true) -> {
                    val consoleId = data.getString("consoleId")
                    sharedPreferences.edit()
                        .putString(ORConstants.CONSOLE_ID_KEY, consoleId)
                        .remove(ORConstants.PUSH_PROVIDER_DISABLED_KEY)
                        .apply()
                    // TODO: Implement topic support
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        val response: MutableMap<String, Any?> =
                            HashMap()
                        response["action"] = "PROVIDER_ENABLE"
                        response["provider"] = "push"
                        response["hasPermission"] = true
                        response["success"] = true
                        val responseData: MutableMap<String, Any> =
                            HashMap()
                        if (task.isSuccessful) {
                            responseData["token"] = task.result
                        }
                        response["data"] = responseData
                        notifyClient(response)
                    }
                }
                action.equals("PROVIDER_DISABLE", ignoreCase = true) -> {
                    // Cannot disable push notifications
                    val response: MutableMap<String, Any?> = HashMap()
                    response["action"] = "PROVIDER_DISABLE"
                    response["provider"] = "push"
                    sharedPreferences.edit().putBoolean(ORConstants.PUSH_PROVIDER_DISABLED_KEY, true).apply()
                    notifyClient(response)
                }
            }
        }

        @Throws(JSONException::class)
        private fun handleStorageProviderMessage(data: JSONObject) {
            val action = data.getString("action")
            when {
                action.equals("PROVIDER_INIT", ignoreCase = true) -> {
                    val response: MutableMap<String, Any> = HashMap()
                    response["action"] = "PROVIDER_INIT"
                    response["provider"] = "storage"
                    response["version"] = "1.0.0"
                    response["enabled"] = true
                    response["requiresPermission"] = false
                    response["hasPermission"] = true
                    response["success"] = true
                    notifyClient(response)
                }
                action.equals("PROVIDER_ENABLE", ignoreCase = true) -> {
                    // Doesn't require enabling but just in case it gets called lets return a valid response
                    val response: MutableMap<String, Any> = HashMap()
                    response["action"] = "PROVIDER_ENABLE"
                    response["provider"] = "storage"
                    response["hasPermission"] = true
                    response["success"] = true
                    notifyClient(response)
                }
                action.equals("STORE", ignoreCase = true) -> {
                    try {
                        val key = data.getString("key")
                        val valueJson = data.getString("value")
                        storeData(key, valueJson)
                    } catch (e: JSONException) {
                        LOG.log(Level.SEVERE, "Failed to store data", e)
                    }
                }
                action.equals("RETRIEVE", ignoreCase = true) -> {
                    try {
                        val key = data.getString("key")
                        val dataJson = retrieveData(key)
                        val response: MutableMap<String, Any?> = HashMap()
                        response["action"] = "RETRIEVE"
                        response["provider"] = "storage"
                        response["key"] = key
                        response["value"] = dataJson
                        notifyClient(response)
                    } catch (e: JSONException) {
                        LOG.log(Level.SEVERE, "Failed to retrieve data", e)
                    }
                }
            }
        }

        @Throws(JSONException::class)
        private fun handleQrScannerProviderMessage(data: JSONObject) {
            val action = data.getString("action")
            if (qrScannerProvider == null) {
                qrScannerProvider = QrScannerProvider(activity)
            }
            when {
                action.equals("PROVIDER_INIT", ignoreCase = true) -> {
                    val initData: Map<String, Any> = qrScannerProvider!!.initialize()
                    notifyClient(initData)
                }
                action.equals("PROVIDER_ENABLE", ignoreCase = true) -> {

                    qrScannerProvider?.enable(object : QrScannerProvider.ScannerCallback {
                        override fun accept(responseData: Map<String, Any>) {
                            notifyClient(responseData)
                        }
                    })
                }
                action.equals("PROVIDER_DISABLE", ignoreCase = true) -> {
                    val response = qrScannerProvider?.disable()
                    notifyClient(response)
                }
                action.equals("SCAN_QR", ignoreCase = true) -> {
                    qrScannerProvider?.startScanner(this@OrMainActivity)
                }
            }
        }
    }

    private fun notifyClient(data: Map<String, Any?>?) {
        try {
            var jsonString = mapper.writeValueAsString(data)

            // Double escape quotes (this is needed for browsers to be able to parse the response)
            jsonString = jsonString.replace("\\\"", "\\\\\"")

            LOG.info("notifyClient with message: $jsonString")
            runOnUiThread {
                binding.webView.evaluateJavascript(
                    String.format(
                        "OpenRemoteConsole._handleProviderResponse('%s')",
                        jsonString
                    ), null
                )
            }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
    }

    private fun storeData(key: String?, data: String?) {
        val editor = sharedPreferences.edit()
        if (data == null) {
            editor.remove(key)
        } else {
            editor.putString(key, data)
        }
        editor.apply()
    }

    private fun retrieveData(key: String?): Any? {
        val str = sharedPreferences.getString(key, null) ?: return null
        // Parse data JSON
        return try {
            mapper.readTree(str)
        } catch (e: JsonProcessingException) {
            str
        }
    }

    private fun onConnectivityChanged(connectivity: Boolean) {
        LOG.info("Connectivity changed: $connectivity")
        if (connectivity) {
            reloadWebView()
        } else {
            Toast.makeText(this, "Check your connection", Toast.LENGTH_LONG).show()
        }
    }

    private inner class ConnectivityChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            onConnectivityChanged(activeNetwork != null && activeNetwork.isConnectedOrConnecting)
        }
    }
}
