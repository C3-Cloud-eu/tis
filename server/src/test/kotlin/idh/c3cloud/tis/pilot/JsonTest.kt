package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.func.getJsonFromString
import org.junit.Test

class JsonTest {

    @Test
    fun `get map`() {
        val s = "{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"severity\":\"error\",\"code\":\"invalid\",\"diagnostics\":\"Some of the requests are failed! Transaction can not be performed ...\"},{\"severity\":\"error\",\"code\":\"invalid\",\"diagnostics\":\"Identifier.system must be an absolute reference, not a local reference\",\"location\":[\"Patient.identifier[1]\"]}]}"
        println(getJsonFromString(s))
    }

    @Test
    fun `get string`() {
        println(getJsonFromString("html error"))
    }

}