package io.openremote.app.network

import android.net.SSLCertificateSocketFactory
import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.openremote.app.model.ORAppInfo
import io.openremote.app.model.ORConsoleConfig
import io.openremote.orlib.models.ORAppConfig
import org.apache.http.conn.ssl.AllowAllHostnameVerifier
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.*
import kotlin.concurrent.thread


typealias ResponseBlock<T> = (statusCode: Int, model: T?, error: Throwable?) -> Unit

class ApiManager(private val baseUrl: String) {

    val mapper = jacksonObjectMapper()

    enum class HttpMethod {
        GET,
        POST,
        PUT,
    }

    fun getAppConfig(realm: String, callback: ResponseBlock<ORAppConfig>?) {
        val uri = Uri.parse(baseUrl)
        val url = URL("${uri.scheme}://${uri.host}/consoleappconfig/${realm}.json")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = HttpMethod.GET.toString()
            setRequestProperty("Accept", "application/json")
            thread(start = true) {
                parseResponse(this, callback)
            }
        }
    }

    fun getAppInfos(callback: ResponseBlock<Map<String, ORAppInfo>>?) {
        get(arrayOf("apps"), callback)
    }

    fun getConsoleConfig(callback: ResponseBlock<ORConsoleConfig>?) {
        get(arrayOf("apps", "consoleConfig"), callback)
    }

    /*********************************Private functions*******************************/
    fun createUrlRequest(
        method: HttpMethod,
        pathComponents: Array<String>,
        queryParameters: Map<String, Any>? = null
    ): HttpURLConnection {
        val builder = Uri.parse(baseUrl).buildUpon()

        for (pathComponent in pathComponents) builder.appendPath(pathComponent)

        if (queryParameters != null) {
            for ((queryParameterKey, queryParameterValue) in queryParameters)
                builder.appendQueryParameter(
                    queryParameterKey,
                    queryParameterValue.toString()
                )
        }

        val url = URL(builder.build().toString())
        with(url.openConnection() as HttpsURLConnection) {

            sslSocketFactory = SSLCertificateSocketFactory.getInsecure(0, null)
            hostnameVerifier = AllowAllHostnameVerifier()
            requestMethod = method.toString()
            setRequestProperty("Accept", "application/json")

            if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            return this
        }
    }

    inline fun <reified T : Any> get(
        pathComponents: Array<String>,
        noinline callback: ResponseBlock<T>?,
        queryParameters: Map<String, Any>? = null
    ) {
        thread(start = true) {
            val urlRequest = createUrlRequest(HttpMethod.GET, pathComponents, queryParameters)
            parseResponse(urlRequest, callback)
        }
    }


    private inline fun <reified T : Any> post(
        pathComponents: Array<String>, item: Any, noinline callback: ResponseBlock<T>?
    ) {
        thread(start = true) {
            val urlRequest = createUrlRequest(HttpMethod.POST, pathComponents)
            urlRequest.apply {
                writeBody(item)
            }.let { httpConnection ->
                parseResponse(httpConnection, callback)
            }
        }
    }

    private inline fun <reified T : Any> put(
        pathComponents: Array<String>, item: Any, noinline callback: ResponseBlock<T>?
    ) {
        thread(start = true) {
            val urlRequest = createUrlRequest(HttpMethod.PUT, pathComponents)
            urlRequest.apply {
                writeBody(item)
            }.let { httpConnection ->
                parseResponse(httpConnection, callback)
            }
        }
    }

    inline fun <reified T : Any> parseResponse(
        httpConnection: HttpURLConnection,
        noinline callback: ResponseBlock<T>?
    ) {
        with(httpConnection) {
            val parsedResult = try {
                if (this.responseCode in 200..299) {
                    Triple(
                        this.responseCode, mapper.readValue<T>(
                            this.inputStream.bufferedReader().readText()
                        ), null
                    )
                } else {
                    val errorResponse = this.errorStream.bufferedReader().readText()
                    Triple(this.responseCode, null, Throwable(errorResponse))
                }
            } catch (e: Exception) {
                Triple(0, null, e)
            }

            parsedResult.third?.let {
                Log.e(this::class.simpleName, it.message, it)
            }
            callback?.invoke(parsedResult.first, parsedResult.second, parsedResult.third)
            this.disconnect()
        }
    }

    private fun HttpURLConnection.writeBody(item: Any) {
        val outputWriter = outputStream.bufferedWriter()
        outputWriter.write(
            mapper.writeValueAsString(item)
        )
        outputWriter.flush()
    }
}
