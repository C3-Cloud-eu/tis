package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Patient
import idh.c3cloud.tis.PatientRepository
import idh.c3cloud.tis.TaskExecutable
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.lang.RuntimeException
import java.net.URI
import java.time.Instant
import java.util.NoSuchElementException

val PARAM_EHR_FILE = "EHR_file_uri"
val PARAM_GP_FILE = "GP_file_uri"

interface Provider3Csv {
    val header: Array<String>
    val PatientId_Column: Int
    val fileName: String

    fun validate(line: CSVRecord) {
        if (line.size() != header.size)
            throw IOException("${fileName} format error: ${header.size} columns expected, but ${line.size()} columns found")
    }
}

val EHR_CSV = object: Provider3Csv {
    // Example secondary care CSV headings
    override val header = arrayOf(
            "patient.nhs", "patient.surname", "patient.forename", "patient.emis", "patient.gender", "patient.dob", "patient.address", "patient.organisation",   //new version
            // "patient.emis", "patient.gender", "patient.organisation", //old version
            "appointment.date", "appointment.slotetype", "appointment.location",
            "consultation.date", "consultation.type", "consultation.user.details",
            "condition.date", "condition.coding.term", "condition.coding.code", "condition.status", "condition.text",
            "allergie.status", "allergie.coding.code", "allergie.coding.term", "allergie.coding.date",
            "procedure.coding.code", "procedure.coding.term", "procedure.text", "procedure.problem.status",
            "procedure.problem.enddate", "procedure.date", "procedure.pratitioner.name",
            "labres.coding.code", "labres.coding.term", "labres.date", "labres.value", "labres.unit", "labres.normal.range",
            "labres.problem.status", "labres.practitioner.name",
            "vs.coding.code", "vs.coding.term", "vs.text", "vs.date", "vs.value", "vs.unit", "vs.normal.range", "vs.practitioner.name",
            "medication.name", "medication.status", "medication.prescription.status", "medication.drug.added.date",
            "medication.linkedproblem.term", "medication.linkedproblem.text", "medication.practitioner.name",
            "medication.quantity", "medication.unit", "medication.dosage",
            "vaccination.date", "vaccination.coding.term", "vaccination.coding.code", "vaccination.practitioner.name",
            "socialh.date", "socialh.coding.code", "socialh.coding.term", "socialh.text", "socialh.problem.status", "socialh.value", "socialh.unit",
            "familyh.coding.code", "familyh.date", "familyh.coding.term", "familyh.text", "familyh.problem.status",
            "functional.coding.code", "functional.date", "functional.coding.term", "functional.text", "functional.value", "functional.practitioner.name",
            "mental.coding.code", "mental.date", "mental.coding.term", "mental.text", "mental.episode", "mental.problem.status", "mental.problem.enddate", "mental.practitioner.name",
            "risk.coding.code", "risk.date", "risk.coding.term", "risk.text", "risk.episode", "risk.problem.status", "risk.problem.enddate", "risk.value", "risk.practitioner.name",
            "cbo.coding.code", "cbo.date", "cbo.coding.term", "cbo.text", "cbo.practitioner.name")

    override val PatientId_Column = 0
    override val fileName = "EHR CSV"
}

val GP_CSV = object: Provider3Csv {
    // Example primary care CSV headings
    override val header = arrayOf(
            "patient.pod", "patient.nhs", "patient.identifier", "patient.id.ur", "patient.givenName", "patient.familyName",
            "patient.gender", "patient.birth", "patient.maritalStatus", "patient.physicalAddress", "patient.codeCurrent",
            "encounter.speciality", "encounter.startDate", "encounter.endDate", "encounter.diagnosis.code",
            "encounter.diagnosis.label", "encounter.procedure.code", "encounter.procedure.label")

    override val PatientId_Column = 1
    override val fileName = "GP CSV"
}

class Provider3CsvIterator(val fileUri: String, private val csv: Provider3Csv) : Iterator<Pair<String, String>>, Closeable {
    private val csvFile = File(URI(fileUri))
    private val csvParser = CSVParser.parse(csvFile, Charsets.UTF_8, CSVFormat.DEFAULT.withTrim())
    private val csvIterator = csvParser.iterator()
    private val block = mutableListOf<CSVRecord>()
    private var patientId = ""
    var lineNo = 2
        private set

    init {
        // drop the two header lines
        csvIterator.next()
        csvIterator.next()
        // get the first line of the first patient
        if (csvIterator.hasNext()) {
            val line = csvIterator.next()
            csv.validate(line)
            lineNo++
            block.add(line)
            patientId = line[csv.PatientId_Column]
        }
    }

    override fun hasNext(): Boolean {
        return block.isNotEmpty()
    }

    override fun next(): Pair<String, String> {
        if (!hasNext()) throw NoSuchElementException()
        var line: CSVRecord? = null
        while (csvIterator.hasNext()) {
            line = csvIterator.next()
            csv.validate(line)
            lineNo++
            if (line[csv.PatientId_Column].isEmpty() || line[csv.PatientId_Column] == patientId) {
                block.add(line)
                line = null
            }
            else break
        }
        //write the block to string
        val buffer = StringBuilder()
        //val csvPrinter = CSVFormat.DEFAULT.withHeader(*GP_EMIS_HEADER).print(buffer)
        val csvPrinter = CSVFormat.DEFAULT.print(buffer)
        csvPrinter.printRecord(*csv.header)
        for (record in block) csvPrinter.printRecord(record)
        csvPrinter.flush()
        csvPrinter.close()
        val output = Pair(patientId, buffer.toString())
        //start with the first line of next patient
        block.clear()
        if (line != null) {
            block.add(line)
            patientId = line[csv.PatientId_Column]
        }
        else patientId = ""
        return output
    }

    override fun close() {
        csvParser.close()
        block.clear()
    }
}

@Component("Provider3-Import-v1")
class Provider3CsvImport @Autowired constructor(
        private val structureMap: StructureMap,
        private val c3dp: C3dp,
        private val patientRepository: PatientRepository
) : TaskExecutable {

    override suspend fun invoke(parameters: Map<String, String>): Map<String, Any> {
        val log = mutableMapOf<String, Any>()
        val createdPatients = mutableListOf<String>()
        val processedPatients = mutableListOf<Patient>()
        var provider3CsvIterator: Provider3CsvIterator? = null
        try {
            val inputId = parameters["patient"]!!.split(",").map(String::trim)
            val importAll = if (inputId.size == 1 && inputId[0] == "ALL") true else false
            log["input"] = if (importAll) "All" else inputId
            val patients = if (importAll) patientRepository.findAll().map(Patient::id).collectList().awaitSingle()
                            else inputId
            //Iterate the GP_EMIS CSV file
            log["phase"] = "Iterating EHR CSV"
            provider3CsvIterator = Provider3CsvIterator(parameters[PARAM_EHR_FILE]!!, EHR_CSV)
            for ((patientId, data) in provider3CsvIterator) {
                if (patientId !in patients) continue
                log["processing"] = patientId
                log["step"] = "Finding patient ${patientId} in registry"
                val patient = patientRepository.findById(patientId).awaitSingle()
                //Structure mapping
                log["step"] = "Structure mapping"
                val fhirBundle = structureMap.transform("provider3:ehr:0.2", MediaType.TEXT_PLAIN_VALUE, data)
                //save to C3DP FHIR repository
                log["step"] = "Saving to C3DP FHIR repository"
                val (transactionBundle, patientCreated) = c3dp.toTransactionBundle(listOf(fhirBundle), patient)
                c3dp.commit(transactionBundle)
                if (patientCreated.isNotEmpty()) {
                    //notify C3DP of PatientCreated
                    log["step"] = "Notifying C3DP PatientCreated"
                    c3dp.notify(patientCreated)
                    createdPatients.add(patientId)
                }
                processedPatients.add(patient)
                log["step"] = "Iterator.next"
            }
            provider3CsvIterator.close()
            val notFound = patients - processedPatients.map(Patient::id)
            if (notFound.isNotEmpty()) throw RuntimeException("Patient ${notFound.joinToString()} not found in EHR")
            //Iterate the GP CSV file
            log["phase"] = "Iterating LGP CSV"
            provider3CsvIterator = Provider3CsvIterator(parameters[PARAM_GP_FILE]!!, GP_CSV)
            var gpProcessed = mutableListOf<String>()
            for ((patientId, data) in provider3CsvIterator) {
                if (patientId !in patients) continue
                log["processing"] = patientId
                log["step"] = "Finding patient ${patientId} in registry"
                val patient = patientRepository.findById(patientId).awaitSingle()
                //Structure mapping
                log["step"] = "Structure mapping"
                val fhirBundle = structureMap.transform("provider3:gp:0.1", MediaType.TEXT_PLAIN_VALUE, data)
                //save to C3DP FHIR repository
                log["step"] = "Saving to C3DP FHIR repository"
                val (transactionBundle, _) = c3dp.toTransactionBundle(listOf(fhirBundle), patient)
                c3dp.commit(transactionBundle)
                //No PatientCreated
                //No patient.lastImport update
                gpProcessed.add(patientId)
                log["step"] = "Iterator.next"
            }
            provider3CsvIterator.close()
            val gpNotFound = patients - gpProcessed
            if (gpNotFound.isNotEmpty())
                log["warning"] = "Patient ${gpNotFound.joinToString()} not found in GP, ignored"
            log.remove("step")
            log.remove("phase")
            log.remove("processing")
            return log
        }
        catch (e: Exception) {
            if (provider3CsvIterator != null) log["Line number"] = provider3CsvIterator.lineNo.toString()
            return logError(log, e)
        }
        finally {
            log["processed"] = processedPatients.map(Patient::id)
            if (createdPatients.isNotEmpty()) log["PatientCreated"] = createdPatients
            try {
                provider3CsvIterator?.close()
            }
            catch (e: Exception) {
                log["Provider3 CSV close error"] = e.toString()
            }
            if (processedPatients.isNotEmpty()) {
                try {
                    // update import time in local database
                    val now = Instant.now()
                    for (patient in processedPatients) patient.lastImport = now
                    patientRepository.saveAll(processedPatients).awaitLast()
                }
                catch (e: Exception) {
                    log["Updating patient.lastImport error"] = e.toString()
                }
            }
        }
    }
}

@Component("Provider3-Import-By-Id-v1")
class Provider3CsvImportById @Autowired constructor(
        private val structureMap: StructureMap,
        private val c3dp: C3dp,
        private val patientRepository: PatientRepository
) : TaskExecutable {

    override suspend fun invoke(parameters: Map<String, String>): Map<String, Any> {
        val log = mutableMapOf<String, Any>()
        var parser: CSVParser? = null
        try {
            //validate patientId
            val patients = parameters["patient"]!!.split(",").map(String::trim)
            log["patient"] = patients
            for (patientId in patients) {
                log["processing"] = patientId
                log["step"] = "Finding patient ${patientId}"
                val patient = patientRepository.findById(patientId).awaitSingle()
                //extract GP CSV
                log["step"] = "Parsing GP CSV"
                parser = CSVParser.parse(File(URI(parameters[PARAM_GP_FILE])), Charsets.UTF_8, CSVFormat.DEFAULT.withTrim())
                val lorenzoData = extract(parser, patientId, GP_CSV)
                if (lorenzoData.isNotEmpty()) {
                    log["step"] = "GP structure mapping"
                    val fhirBundle = structureMap.transform("provider3:gp:0.1", MediaType.TEXT_PLAIN_VALUE, lorenzoData)
                    //save to C3DP FHIR repository
                    log["step"] = "Saving GP to C3DP FHIR repository"
                    val (transactionBundle, _) = c3dp.toTransactionBundle(listOf(fhirBundle), patient)
                    c3dp.commit(transactionBundle)
                    //No PatientCreated
                    //No patient.lastImport update
                    log["step"] = "GP Done"
                }
                else {
                    log["info"] = "No data found in GP CSV"
                }
                log["step"] = "Parsing EHR CSV"
                parser = CSVParser.parse(File(URI(parameters[PARAM_EHR_FILE])), Charsets.UTF_8, CSVFormat.DEFAULT.withTrim())
                val gpEmisData = extract(parser, patientId, EHR_CSV)
                if (gpEmisData.isNotEmpty()) {
                    log["step"] = "EHR structure mapping"
                    val fhirBundle = structureMap.transform("provider3:ehr:0.2", MediaType.TEXT_PLAIN_VALUE, gpEmisData)
                    //save to C3DP FHIR repository
                    log["step"] = "Saving EHR to C3DP FHIR repository"
                    val (transactionBundle, patientCreated) = c3dp.toTransactionBundle(listOf(fhirBundle), patient)
                    c3dp.commit(transactionBundle)
                    if (patientCreated.isNotEmpty()) {
                        //notify C3DP of PatientCreated
                        log["step"] = "Notifying C3DP PatientCreated"
                        log["PatientCreated"] = patientCreated
                        c3dp.notify(patientCreated)
                    }
                    // update import time in local database
                    log["step"] = "Updating patient.lastImport"
                    patient.lastImport = Instant.now()
                    patientRepository.save(patient).awaitSingle()
                    log["step"] = "EHR Done"
                }
                else {
                    throw RuntimeException("No data found in EHR CSV")
                }
            }
            log.remove("processing")
            log.remove("step")
            return log
        }
        catch (e: Exception) {
            return logError(log, e)
        }
        finally {
            parser?.close()
        }
    }

    private suspend fun extract(csvParser: CSVParser, patientId: String, csv: Provider3Csv): String {
        val csvIterator = csvParser.iterator()
        // drop the two header lines
        csvIterator.next()
        csvIterator.next()
        var line: CSVRecord? = null
        while (csvIterator.hasNext()) {
            line = csvIterator.next()
            csv.validate(line)
            if (line[csv.PatientId_Column] == patientId) break
            line = null
        }
        if (line != null) { // found the patient id
            val buffer = StringBuilder()
            val csvPrinter = CSVFormat.DEFAULT.print(buffer)
            csvPrinter.printRecord(*csv.header)
            csvPrinter.printRecord(line)
            while (csvIterator.hasNext()) {
                line = csvIterator.next()
                csv.validate(line)
                if (line[csv.PatientId_Column].isEmpty() || line[csv.PatientId_Column] == patientId)
                    csvPrinter.printRecord(line)
                else break
            }
            csvPrinter.flush()
            csvPrinter.close()
            return buffer.toString()
        }
        else return ""
    }

}
