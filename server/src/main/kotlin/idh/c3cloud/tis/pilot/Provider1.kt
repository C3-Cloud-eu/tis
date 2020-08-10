package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.PatientRepository
import idh.c3cloud.tis.TaskExecutable
import idh.c3cloud.tis.func.SoapClient
import idh.c3cloud.tis.func.evalXpathExpr
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.Instant

@Component("Provider1-CDA-DBP-Import-v1")
class Provider1 @Autowired constructor(
        @Value("\${provider1.cda-uri}") private val cdaUrl: String,
        @Value("\${provider1.dbp-uri}") private val dbpUrl: String,
        private val structureMap: StructureMap,
        private val c3dp: C3dp,
        private val patientRepository: PatientRepository,
        private val soap: SoapClient
): TaskExecutable {
    // Example SOAP template
    private val cdaTemplate = """<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tem="http://tempuri.org/">
			            |  <soapenv:Header/>
                        |  <soapenv:Body>
                        |    <tem:getInformeHCRCDA3_ATC>
                        |      <!--Optional:-->
                        |      <tem:Cic>%s</tem:Cic>
                        |      <!--Optional:-->
                        |      <tem:IdIdioma>1</tem:IdIdioma>
                        |    </tem:getInformeHCRCDA3_ATC>
                        |  </soapenv:Body>
                        |</soapenv:Envelope>""".trimMargin()

    // Exmaple list of local test/vital sign codes
    private val dbpCodes = listOf(3, 4, 6, 7, 8, 9, 12, 13, 14, 48, 49, 50, 51, 52, 53, 60, 61, 62, 65, 69, 71, 74, 75, 82,
            104, 346, 863, 906, 907, 948, 2331, 2447, 2530, 11440, 21002, 21139, 21242, 33382, 38143, 39388, 39390)
    private val immunizationCodes = listOf(25, 34026, 32357)
    
    // Example SOAP template for DBP (tests and immunisations) endpoint
    private val dbpTemplate = """<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tem="http://tempuri.org/">
                        |  <soap:Header/>
                        |  <soap:Body>
                        |    <tem:ObtenerValor_V1>
                        |      <tem:CIC>%s</tem:CIC>
                        |      <tem:Dbps>
                        |        <tem:CodDbp>
                        |          %s
                        |        </tem:CodDbp>
                        |      </tem:Dbps>
                        |    </tem:ObtenerValor_V1>
                        |  </soap:Body>
                        |</soap:Envelope>""".trimMargin()

    private fun dbpCodesToString(codes: List<Int>) =
        codes.map {"<tem:string>$it</tem:string>"}.joinToString("\n          ")

    /**
     * Expects a list of Patient Id
     */
    override suspend fun invoke(parameters: Map<String, String>): Map<String, Any> {
        val log = mutableMapOf<String, Any>()
        val createdPatients = mutableListOf<String>()
        val processedPatients = mutableListOf<String>()
        try {
            val inputId = parameters["patient"]!!.split(",").map(String::trim)
            val importAll = if (inputId.size == 1 && inputId[0] == "ALL") true else false
            log["input"] = if (importAll) "All" else inputId
            log["step"] = "Finding patients in registry"
            val patientFlux = if (importAll) patientRepository.findAll()
                            else patientRepository.findAllById(inputId)
            val patients = patientFlux.collectList().awaitSingle()
            for (patient in patients) {
                // find patient in local database
                log["processing"] = patient.id
                // call CDA & DBP service
                log["step"] = "Invoking CDA and DBP services"
                val (cdaSoapResponse, dbpSoapResponse, immunizationDbpResponse) = invokeCdaDbpServices(patient.id)
                log["step"] = "Extracting CDA from soap response"
                val cda = evalXpathExpr(cdaSoapResponse, "//HCR_CDA/text()")
                // CDA structure mapping
                log["step"] = "CDA structure mapping"
                val cdaBundle = structureMap.transform("provider1:cda:0.1", MediaType.TEXT_XML_VALUE, cda)
                // DBP structure mapping
                log["step"] = "DBP structure mapping"
                val dbpBundle = structureMap.transform("provider1:dbp:0.1", MediaType.TEXT_XML_VALUE, dbpSoapResponse)
                //DBP Immunization structure mapping
                log["step"] = "Immunization structure mapping"
                val immunizationBundle = structureMap.transform("provider1:dbpi:0.1", MediaType.TEXT_XML_VALUE, immunizationDbpResponse)
                //save to C3DP FHIR repository
                log["step"] = "Saving to C3DP FHIR repository"
                val (transactionBundle, patientCreated) = c3dp.toTransactionBundle(listOf(cdaBundle, dbpBundle, immunizationBundle), patient)
                c3dp.commit(transactionBundle)
                if (patientCreated.isNotEmpty()) {
                    //notify C3DP of PatientCreated
                    log["step"] = "Notifying C3DP PatientCreated"
                    c3dp.notify(patientCreated)
                    createdPatients.add(patient.id)
                }
                // update import time in local database
                log["step"] = "Updating patient.lastImport"
                patient.lastImport = Instant.now()
                patientRepository.save(patient).awaitSingle()
                processedPatients.add(patient.id)
            }
            log.remove("processing")
            log.remove("step")
            return log
        }
        catch (e: Exception) {
            return logError(log, e)
        }
        finally {
            log["processed"] = processedPatients
            if (createdPatients.isNotEmpty()) log["PatientCreated"] = createdPatients
        }
    }

    private data class Provider1ServiceResult(val cda: String, val dbp: String, val immunization: String)

    private suspend fun invokeCdaDbpServices(patientId: String): Provider1ServiceResult = coroutineScope {
        val cda = async {
            val msg = cdaTemplate.format(patientId)
            soap(cdaUrl, msg)
        }
        val dbp = async {
            val msg = dbpTemplate.format(patientId, dbpCodesToString(dbpCodes))
            soap(dbpUrl, msg)
        }
        val immunization = async {
            val msg = dbpTemplate.format(patientId, dbpCodesToString(immunizationCodes))
            soap(dbpUrl, msg)
        }
        Provider1ServiceResult(cda.await(), dbp.await(), immunization.await())
    }

}
