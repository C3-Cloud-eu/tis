package idh.c3cloud.tis.pilot

import ca.uhn.fhir.rest.api.Constants
import idh.c3cloud.tis.Hapi
import idh.c3cloud.tis.Patient
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.hl7.fhir.dstu3.model.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.lang.RuntimeException
import java.time.Instant
import java.util.Date

@Component
final class C3dp(@Value("\${c3dp.fhir-uri}") private val FHIR_SERVER_URL: String,
                 @Value("\${c3dp.event-api-uri}") private val EVENT_API_URL: String,
                 @Value("\${c3dp.incremental-loading}") private val INCREMENTAL_LOADING: Boolean,
                 @Value("\${c3dp.missing-timestamp}") private val MISSING_TIMESTAMP: String,
                 private val webClientBuilder: WebClient.Builder,
                 private val clientCredentialsWebClientFilter: ServerOAuth2AuthorizedClientExchangeFilterFunction) {

    private val registrationId = "c3cloud-fhir"

    private val webClient = webClientBuilder.clone().filter(clientCredentialsWebClientFilter).build()

    fun patients() = runBlocking {
        webClient
                .get()
                .uri(FHIR_SERVER_URL + "Patient/")
                .attributes(clientRegistrationId(registrationId))
                .retrieve()
                .bodyToMono<String>()
                .awaitSingle()
    }

    data class PatientCreated(val reference: String, val display: String)

    /**
     * Expects a single parameter: "FHIR resource bundle" as String
     */
    suspend fun commit(transactionBundle: String) =
            webClient
                .post()
                .uri(FHIR_SERVER_URL)
                .header(HttpHeaders.CONTENT_TYPE, Constants.CT_FHIR_JSON_NEW + Constants.CHARSET_UTF8_CTSUFFIX)
                .attributes(clientRegistrationId(registrationId))
                .syncBody(transactionBundle)
                .retrieve()
                .bodyToMono<String>()
                .awaitSingle()

    fun toTransactionBundle(bundles: List<String>, patient: Patient): Pair<String, List<PatientCreated>> {
        val parser = Hapi.newJsonParser()
        val transactionBundle = Bundle()
        transactionBundle.setType(Bundle.BundleType.TRANSACTION)

        //Patient data sync audit
        val audit = AuditEvent()
        audit.type = Coding("http://dicom.nema.org/resources/ontology/DCM", "110107", "Import")
        audit.addSubtype(Coding("http://www.c3-cloud.eu/fhir/audit-subtype", "record-sync", "Patient record sync"))
        audit.action = AuditEvent.AuditEventAction.U
        audit.recorded = Date()
        audit.outcome = AuditEvent.AuditEventOutcome._0
        val source = AuditEvent.AuditEventAgentComponent()
        source.addRole(CodeableConcept().addCoding(Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Source Role ID")))
        source.userId = Identifier().setValue("tis.c3-cloud.eu")
        source.name = "C3-Cloud TIS"
        source.requestor = true
        audit.addAgent(source)
        val destination = AuditEvent.AuditEventAgentComponent()
        destination.addRole(CodeableConcept().addCoding(Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Destination Role ID")))
        destination.userId = Identifier().setValue("fhir.c3-cloud.eu")
        destination.name = "C3-Cloud FHIR Repository"
        destination.requestor = false
        destination.network = AuditEvent.AuditEventAgentNetworkComponent().setAddress(FHIR_SERVER_URL).setType(AuditEvent.AuditEventAgentNetworkType._2)
        audit.addAgent(destination)
        audit.source = AuditEvent.AuditEventSourceComponent()
        audit.source.site = "C3-Cloud TIS"
        audit.source.identifier = Identifier().setValue("tis.c3-cloud.eu")
        val entity = audit.addEntity()
        entity.type = Coding("http://hl7.org/fhir/audit-entity-type", "1", "Person")
        entity.role = Coding("http://hl7.org/fhir/object-role", "1", "Patient")

        val patientCreated = mutableListOf<PatientCreated>()
        for (bundle in bundles) {
            val inputBundle = parser.parseResource(Bundle::class.java, bundle)
            for (entry in inputBundle.entry) {
                val resource = entry.resource
                val transactionEntry = Bundle.BundleEntryComponent()
                val request = Bundle.BundleEntryRequestComponent()
                if (resource.resourceType == ResourceType.Patient) {
                    entity.reference = Reference(resource)
                    if (patient.lastImport != null) continue    // Patient resource already in repository, ignore
                    // Add patient resource
                    val patientResource = resource as org.hl7.fhir.dstu3.model.Patient
                    // c3cloud identifier
                    val c3cloudId = Identifier()
                    c3cloudId.system = "http://www.c3-cloud.eu/Identifier/pseudonym"
                    c3cloudId.value = patient.c3cloudId
                    patientResource.identifier.add(c3cloudId)
                    // email
                    if (patient.email != null) {
                        val email = ContactPoint()
                        email.system = ContactPoint.ContactPointSystem.EMAIL
                        email.value = patient.email
                        patientResource.addTelecom(email)
                    }
                    // evaluation group
                    if (patient.layer3) {
                        val ext = Extension()
                        ext.url = "http://www.c3-cloud.eu/fhir/StructureDefinition/evaluationLayer"
                        ext.setValue(StringType("layer-3"))
                        patientResource.addExtension(ext)
                    }
                    // use medical device
                    if (patient.useMedicalDevice) {
                        val ext = Extension()
                        ext.url = "http://www.c3-cloud.eu/fhir/StructureDefinition/evaluationLayer"
                        ext.setValue(StringType("med-device"))
                        patientResource.addExtension(ext)
                    }
                    //PUT patient resource
                    request.method = Bundle.HTTPVerb.PUT
                    request.url = resource.id
                    transactionEntry.setFullUrl(FHIR_SERVER_URL + resource.id)
                    //Add PatientCreated
                    val name = if (patientResource.name.isEmpty()) "" else patientResource.nameFirstRep.nameAsSingleString
                    patientCreated.add(PatientCreated(patientResource.id, name))
                }
                else {
                    val lastImport = patient.lastImport
                    if (lastImport != null && INCREMENTAL_LOADING) {   //incremental update
                        val hasTimestamp = hasTimestamp(resource)
                        if (!hasTimestamp) {
                            if (MISSING_TIMESTAMP == "ignore") continue
                            if (MISSING_TIMESTAMP == "error")
                                throw RuntimeException("Missing timestamp: " + parser.encodeResourceToString(resource))
                        }
                        if (hasTimestamp && isOldData(resource, lastImport)) continue   //old data, ignore
                    }
                    request.method = Bundle.HTTPVerb.POST
                    request.url = resource.fhirType()
                }
                transactionEntry.resource = resource
                transactionEntry.request = request
                transactionBundle.addEntry(transactionEntry)
            }
        }
        transactionBundle.addEntry().setResource(audit).setRequest(
                Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl(audit.fhirType()))
        return Pair(parser.setPrettyPrint(true).encodeResourceToString(transactionBundle), patientCreated)
    }

    private fun hasTimestamp(resource: Resource): Boolean {
        val timestampField = when (resource) {
            is Condition -> resource.onsetDateTimeType
            is MedicationStatement -> resource.effectivePeriod
            is Immunization -> resource.date
            is Observation -> resource.effectiveDateTimeType
            is AllergyIntolerance -> resource.onsetDateTimeType
            is FamilyMemberHistory -> resource.date
            is Procedure -> resource.performedDateTimeType
            is Encounter -> resource.period
            else -> null
        }
        if (timestampField is DateTimeType) return timestampField.value != null
        if (timestampField is Period) return timestampField.start != null
        return timestampField != null
    }

    private fun isOldData(resource: Resource, lastImport: Instant): Boolean {
        val resourceDate = when (resource) {
            is Condition -> resource.onsetDateTimeType.value
            is MedicationStatement -> resource.effectivePeriod.start
            is Immunization -> resource.date
            is Observation -> resource.effectiveDateTimeType.value
            is AllergyIntolerance -> resource.onsetDateTimeType.value
            is FamilyMemberHistory -> resource.date
            is Procedure -> resource.performedDateTimeType.value
            is Encounter -> resource.period.start
            else -> null
        }
        if (resourceDate == null) throw RuntimeException("Unsupported resource type: ${resource.resourceType}")
        else return resourceDate.toInstant().isBefore(lastImport)
    }

    private data class PatientCreatedEvent(val subject: PatientCreated) {
        val type = "PatientCreated"
        val category = "system-event"
        val timestamp = Instant.now()

        private data class RelatedResource(val reference: String, val relationship: String = "target")
        val relatedResources = arrayOf(RelatedResource(subject.reference))
    }

    suspend fun notify(patientCreated: List<PatientCreated>) = notify(patientCreated.map(C3dp::PatientCreatedEvent))

    suspend fun notify(jobId: ObjectId, status: Map<String, Any>) {
        val jobCompleted = status.toMutableMap()
        jobCompleted["type"] = "DataImportComplete"
        jobCompleted["category"] = "system-event"
        jobCompleted["job"] = jobId
        notify(jobCompleted)
    }

    private suspend fun notify(body: Any) {
        webClient
                .post()
                .uri(EVENT_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .attributes(clientRegistrationId(registrationId))
                .syncBody(body)
                .retrieve()
                .bodyToMono<Void>()
                .awaitFirstOrNull()
    }

    suspend fun notifyTest(jobId: ObjectId, status: Map<String, Any>) {
        val jobCompleted = status.toMutableMap()
        jobCompleted["type"] = "DataImportComplete"
        jobCompleted["category"] = "system-event"
        jobCompleted["job"] = jobId

        webClientBuilder.build()
                .post()
                .uri("http://localhost/api/c3cloud/import/job/event/api")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(jobCompleted)
                .retrieve()
                .bodyToMono<Void>()
                .awaitFirstOrNull()
    }

}