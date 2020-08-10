package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Patient
import idh.c3cloud.tis.func.SoapClient
import idh.c3cloud.tis.func.evalXpathExpr
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class Provider1Test {
    @Autowired
    private lateinit var structureMap: StructureMap

    @Autowired
    private lateinit var soap: SoapClient

    @Autowired
    private lateinit var provider1: Provider1

    @Autowired
    private lateinit var c3dp: C3dp

    @Test
    fun cdaServiceException() = runBlocking {
        var completedExceptionally = false
        val job = launch (CoroutineExceptionHandler { _, e ->
            println("Exception handler: " + e)
            completedExceptionally = true
            // don't crash, just print exception -> DON'T REALLY DO IT LIKE THIS
        }) {
            provider1(mapOf("patient" to "10574682"))
        }
        job.join()
        assertTrue(completedExceptionally)
    }

    @Test
    fun whole_job() = runBlocking {
        val job = launch {
            provider1(mapOf("patient" to "211313"))
        }
        job.join()
    }

    @Test
    fun CDA_123456() = getCDA("123456")

    @Test
    fun CDA() = getCDA("123456")

    private fun getCDA(cic: String) = runBlocking {
        val cdaUrl = TestUtil.getProperty(provider1, "cdaUrl") as String
        val cdaTemplate =  TestUtil.getProperty(provider1, "cdaTemplate") as String
        val msg = cdaTemplate.format(cic)
        val job = launch {
            val soapResponse = soap(cdaUrl, msg)
            val cda = evalXpathExpr(soapResponse, "//HCR_CDA/text()")
            //println(cda)
            val cdaBundle = structureMap.transform("provider1:cda:0.1", MediaType.TEXT_XML_VALUE, cda)
            //println(fhirBundle)
            val patient = Patient(cic, "1")
            val (transactionBundle, _) = c3dp.toTransactionBundle(listOf(cdaBundle), patient)
        }
        job.join()
    }

    @Test
    fun dbpCodes() {
        val dbpCodes = TestUtil.getProperty(provider1, "dbpCodes") as List<Int>
        val dbpTemplate = TestUtil.getProperty(provider1, "dbpTemplate") as String
        println(dbpTemplate.format("79390", dbpCodes.map {"<tem:string>$it</tem:string>"}.joinToString("\n          ")))
        //println(dbpCodes.sorted())
    }

    @Test
    fun DBP() = getDBP("123456")

    private fun getDBP(cic: String) = runBlocking {
        val dbpCodes = TestUtil.getProperty(provider1, "dbpCodes") as List<Int>
        val dbpTemplate = TestUtil.getProperty(provider1, "dbpTemplate") as String
        val msg = dbpTemplate.format(cic, dbpCodes.map {"<tem:string>$it</tem:string>"}.joinToString("\n          "))
        val dbpUrl = TestUtil.getProperty(provider1, "dbpUrl") as String
        val job = launch {
            val soapResponse = soap(dbpUrl, msg)
            val dbpBundle = structureMap.transform("provider1:dbp:0.1", MediaType.TEXT_XML_VALUE, soapResponse)
            //println(dbpBundle)
            val patient = Patient(cic, "1")
            val (transactionBundle, _) = c3dp.toTransactionBundle(listOf(dbpBundle), patient)
            //println(c3dp.commit(transactionBundle))
        }
        job.join()
    }

    @Test
    fun Immunization() = runBlocking {
        val cic = "123456"
        val immunizationCodes = TestUtil.getProperty(provider1, "immunizationCodes") as List<Int>
        val dbpTemplate = TestUtil.getProperty(provider1, "dbpTemplate") as String
        val msg = dbpTemplate.format(cic, immunizationCodes.map {"<tem:string>$it</tem:string>"}.joinToString("\n          "))
        val dbpUrl = TestUtil.getProperty(provider1, "dbpUrl") as String
        val job = launch {
            val soapResponse = soap(dbpUrl, msg)
            val immunizationBundle = structureMap.transform("provider1:dbpi:0.1", MediaType.TEXT_XML_VALUE, soapResponse)
            val patient = Patient(cic, "1")
            val (transactionBundle, _) = c3dp.toTransactionBundle(listOf(immunizationBundle), patient)
            //println(transactionBundle)
        }
        job.join()
    }

}