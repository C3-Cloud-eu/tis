package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Hapi
import idh.c3cloud.tis.Patient
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.dstu3.model.Bundle
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

@RunWith(SpringRunner::class)
@SpringBootTest
class Test {
    @Autowired
    private lateinit var c3dp: C3dp

    @Test
    fun C3dp_all_patients() {
        println(c3dp.patients())
    }

    @Test
    fun listFiles() {
        TestUtil.getFile("sis-output").listFiles().forEach {
            println("====================================================================\n")
            println(it.name)
            println(it.readText())
        }
    }

    @Test
    fun notify_PatientCreated_event() = runBlocking {
        val patientCreated = listOf(C3dp.PatientCreated("Patient/147", ""))
        println(c3dp.notify(patientCreated))
    }

    @Test
    fun loadFilesToC3DP() {
        TestUtil.getFile("sis-output").listFiles().forEach {
            loadOneFile(it)
        }
    }

    @Test
    fun loadOneFileToC3DP() {
        loadOneFile(TestUtil.getFile("sis-output-ok/provider1-1.json"))
    }

    private fun loadOneFile(fhirFile: File) = runBlocking {
        val c3dpBundle_id = fhirFile.name.dropLast(5)
        println("=================================== Loading ${c3dpBundle_id} ===================================")

        //printBundle(C3dp.doTransaction(fhirFile.readText()), "C3DP Request")

        //val loggingInterceptor = LoggingInterceptor()
        //loggingInterceptor.setLogRequestSummary(true)
        //loggingInterceptor.setLogRequestBody(true)
        //val fhirClient = Hapi.newRestfulGenericClient(c3dpUrl)
        //fhirClient.registerInterceptor(loggingInterceptor)
        //val c3dpResponse = fhirClient.transaction().withBundle(c3dpBundle).encodedJson().execute()
        //printBundle(c3dpResponse, "C3DP Response")

        val patient = Patient("123456", "1", layer3 = true)
        printBundle(c3dp.toTransactionBundle(listOf(fhirFile.readText()), patient).first, "C3DP Response")
    }

    private fun printBundle(bundle: Bundle, header: String) {
        println("=========================== $header =================================")
        println(Hapi.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle))
    }

    private fun printBundle(bundle: String, header: String) {
        println("=========================== $header =================================")
        println(bundle)
    }

}