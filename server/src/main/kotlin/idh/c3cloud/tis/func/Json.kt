package idh.c3cloud.tis.func

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

fun getJsonFromString(s: String): Any {
    val mapper = ObjectMapper()
    try {
        return mapper.readValue<Map<String, Any>>(s)
    }
    catch (e: JsonProcessingException) {
        return s
    }
}
