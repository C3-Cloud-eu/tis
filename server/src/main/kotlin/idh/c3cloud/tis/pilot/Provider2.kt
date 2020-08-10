package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.PatientRepository
import idh.c3cloud.tis.TaskExecutable
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant

@Component("Provider2-OpenServices-Import-v1")
class Provider2 @Autowired constructor(
        private val structureMap: StructureMap,
        private val c3dp: C3dp,
        private val patientRepository: PatientRepository,
        private val webClientBuilder: WebClient.Builder,
        private val clientCredentialsWebClientFilter: ServerOAuth2AuthorizedClientExchangeFilterFunction): TaskExecutable {

    private val webClient = webClientBuilder.clone()
            .baseUrl("https://{{API_URL}}/patients")
            .defaultHeader("Ocp-Apim-Subscription-Key", "{{secret-key}}")
            .filter(clientCredentialsWebClientFilter)
            .build()

    private val clientId = "open-services"

    // Example list of endpoints
    private val endpoints = listOf("/patient", "/diagnosis", "/journalnotes", "/chemistrylabreports", "/medications")// "/contacts")

    suspend fun get(apiUri: String, id: String) =
            webClient
                    .get()
                    .uri(apiUri)
                    .header("patient", id)
                    .attributes(clientRegistrationId(clientId))
                    .retrieve()
                    .bodyToMono<String>()
                    .awaitSingle()

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
                val fhirBundles = mutableListOf<String>()
                for (endpoint in endpoints) {
                    // call OpenServices
                    log["step"] = "OpenServices ${endpoint}"
                    val data = get(endpoint, patient.id)
                    // structure mapping
                    val mapperInstance = "provider2:api:0.1"
                    log["step"] = "Structure mapping - ${endpoint}"
                    val bundle = structureMap.transform(mapperInstance, MediaType.TEXT_XML_VALUE, data)
                    //println(bundle)
                    fhirBundles.add(bundle)
                }
                //save to C3DP FHIR repository
                log["step"] = "Saving to C3DP FHIR repository"
                val (transactionBundle, patientCreated) = c3dp.toTransactionBundle(fhirBundles, patient)
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

}
