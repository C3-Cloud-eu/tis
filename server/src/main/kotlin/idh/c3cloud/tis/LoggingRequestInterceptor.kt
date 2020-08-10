package idh.c3cloud.tis

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class LoggingRequestInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        traceRequest(request, body)
        val response = execution.execute(request, body)
        traceResponse(response)
        return response
    }

    private fun traceRequest(request: HttpRequest, body: ByteArray) {
        log.debug("===========================request begin================================================")
        log.debug("URI         : {}", request.uri)
        log.debug("Method      : {}", request.method)
        log.debug("Headers     : {}", request.headers)
        log.debug("Request body: {}", String(body, StandardCharsets.UTF_8))
        log.debug("==========================request end================================================")
    }

    private fun traceResponse(response: ClientHttpResponse) {
        val inputStringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(response.body, "UTF-8"))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            inputStringBuilder.append(line)
            inputStringBuilder.append('\n')
            line = bufferedReader.readLine()
        }
        log.debug("============================response begin==========================================")
        log.debug("Status code  : {}", response.statusCode)
        log.debug("Status text  : {}", response.statusText)
        log.debug("Headers      : {}", response.headers)
        log.debug("Response body: {}", inputStringBuilder.toString())
        log.debug("=======================response end=================================================")
    }

    companion object {
        internal val log = LoggerFactory.getLogger(LoggingRequestInterceptor::class.java)
    }
}