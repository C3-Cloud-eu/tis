package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.TaskExecutionException
import idh.c3cloud.tis.func.getJsonFromString
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.NoSuchElementException

fun logError(log: MutableMap<String, Any>, e: Exception): MutableMap<String, Any> {
    log["error"] = e.toString()
    log["message"] = when (e) {
        is RestClientResponseException -> getJsonFromString(e.responseBodyAsString)
        is WebClientResponseException -> getJsonFromString(e.responseBodyAsString)
        is NoSuchElementException -> "Patient Not Registered"
        else -> e.message ?: ""
    }
    throw TaskExecutionException(log)
}