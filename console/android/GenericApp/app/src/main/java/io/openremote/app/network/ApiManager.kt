package io.openremote.app.network

import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.openremote.app.models.ORAppConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors


typealias ResponseBlock<T> = (statusCode: Int, model: T?, error: Throwable?) -> Unit

class ApiManager(private val baseUrl: String) {

    companion object {
        var accessToken: String? = null
    }

    enum class HttpMethod {
        GET,
        POST,
        PUT,
    }

    fun getAppConfig(callback: ResponseBlock<ORAppConfig>?) {
        get(arrayOf("app", "config"), callback)
    }

    /*********************************Private functions*******************************/
    private fun createUrlRequest(
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

        return URL(builder.build().toString())
            .openConnection()
            .let {
                it as HttpURLConnection
            }.apply {
                requestMethod = method.toString()
                setRequestProperty("Accept", "application/json")

                if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
            }
    }

    private inline fun <reified T : Any> get(
        pathComponents: Array<String>,
        noinline callback: ResponseBlock<T>?,
        queryParameters: Map<String, Any>? = null
    ) {
        Executors.newCachedThreadPool().execute {
            val urlRequest = createUrlRequest(HttpMethod.GET, pathComponents, queryParameters)
            parseResponse<T>(urlRequest, callback)
        }
    }


    private inline fun <reified T : Any> post(
        pathComponents: Array<String>, item: Any, noinline callback: ResponseBlock<T>?
    ) {
        Executors.newCachedThreadPool().execute {

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
        Executors.newCachedThreadPool().execute {

            val urlRequest = createUrlRequest(HttpMethod.PUT, pathComponents)
            urlRequest.apply {
                writeBody(item)
            }.let { httpConnection ->
                parseResponse(httpConnection, callback)
            }
        }
    }

    private inline fun <reified T : Any> parseResponse(
        httpConnection: HttpURLConnection,
        noinline callback: ResponseBlock<T>?
    ) {
        val model = if (httpConnection.responseCode == 200) {
            try {
                jacksonObjectMapper().readValue(
                    httpConnection.inputStream.bufferedReader().readText(),
                    T::class.java
                )
            } catch (e: Exception) {
                Log.e(this::class.simpleName, e.message, e)
                null
            }
        } else {
            null
        }
        val error = try {

            if (httpConnection.responseCode !in 200..299) {
                val errorResponse = httpConnection.errorStream.bufferedReader().readText()
                Throwable(errorResponse)
            } else {
                null
            }
        } catch (e: Exception) {
            e
        }
        error?.let {
            Log.e(this::class.simpleName, it.message, it)
        }
        callback?.invoke(httpConnection.responseCode, model, error)
        httpConnection.disconnect()
    }

    private fun HttpURLConnection.writeBody(item: Any) {
        val outputWriter = outputStream.bufferedWriter()
        outputWriter.write(
            jacksonObjectMapper().writeValueAsString(item)
        )
        outputWriter.flush()
    }
}