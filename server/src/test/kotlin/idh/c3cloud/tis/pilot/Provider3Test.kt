package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Patient
import idh.c3cloud.tis.PatientRepository
import idh.c3cloud.tis.TaskExecutable
import idh.c3cloud.tis.TaskExecutionException
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.NoSuchElementException

@RunWith(SpringRunner::class)
@SpringBootTest
class Provider3Test {
    @Autowired
    private lateinit var c3dp: C3dp

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var ctx: ApplicationContext

    private lateinit var ehrImportAll: TaskExecutable
    private lateinit var ehrImportById: TaskExecutable
    private lateinit var ehrIterator: Provider3CsvIterator
    private lateinit var gpIterator: Provider3CsvIterator
    private lateinit var structureMap: StructureMap

    private val ehrFileUri = "file:///C:/c3cloud/test/EHR.csv"
    private val gpFileUri = "file:///C:/c3cloud/test/GP.csv"

    @Before
    fun setup() {
        ehrImportAll = ctx.getBean(TestData.provider3().id, TaskExecutable::class.java)
        ehrImportById = ctx.getBean(TestData.provider3Id().id, TaskExecutable::class.java)
        ehrIterator = Provider3CsvIterator(ehrFileUri, EHR_CSV)
        gpIterator = Provider3CsvIterator(gpFileUri, GP_CSV)
        structureMap = ctx.getBean("structureMap", StructureMap::class.java)
    }

    @Test
    fun checkHeaders() {
        // List of headers in CSV file, example given below
        val h1 = "patient.nhs,patient.surname,patient.forename,patient.emis,patient.gender,patient.dob,patient.address,patient.organisation,appointment.date,appointment.slotetype,appointment.location,consultation.date,consultation.type,consultation.user.details,condition.date,condition.coding.term,condition.coding.code,condition.status,condition.text,allergie.status,allergie.coding.code,allergie.coding.term,allergie.coding.date,procedure.coding.code,procedure.coding.term,procedure.text,procedure.problem.status,procedure.problem.enddate,procedure.date,procedure.pratitioner.name,labres.coding.code,labres.coding.term,labres.date,labres.value,labres.unit,labres.normal.range,labres.problem.status,labres.practitioner.name,vs.coding.code,vs.coding.term,vs.text,vs.date,vs.value,vs.unit,vs.normal.range,vs.practitioner.name,medication.name,medication.status,medication.prescription.status,medication.drug.added.date,medication.linkedproblem.term,medication.linkedproblem.text,medication.practitioner.name,medication.quantity,medication.unit,medication.dosage,vaccination.date,vaccination.coding.term,vaccination.coding.code,vaccination.practitioner.name,socialh.date,socialh.coding.code,socialh.coding.term,socialh.text,socialh.problem.status,socialh.value,socialh.unit,familyh.coding.code,familyh.date,familyh.coding.term,familyh.text,familyh.problem.status,functional.coding.code,functional.date,functional.coding.term,functional.text,functional.value,functional.practitioner.name,mental.coding.code,mental.date,mental.coding.term,mental.text,mental.episode,mental.problem.status,mental.problem.enddate,mental.practitioner.name,risk.coding.code,risk.date,risk.coding.term,risk.text,risk.episode,risk.problem.status,risk.problem.enddate,risk.value,risk.practitioner.name,cbo.coding.code,cbo.date,cbo.coding.term,cbo.text,cbo.practitioner.name".split(",")
        assertEquals(EHR_CSV.header.size, h1.size)
        for ((header, h) in EHR_CSV.header.zip(h1)) {
            if (header != h) println("${header} \t ${h}")
        }
        assertTrue(EHR_CSV.header.contentEquals(h1.toTypedArray()))
    }

    @Test
    fun printEhrFirstPatient() {
        val (ehrNo, patientData) = ehrFirstPatient()
        println(ehrNo)
        println(patientData)
    }

    private fun ehrFirstPatient() = ehrIterator.use {
        ehrIterator.next()
    }

    @Test
    fun printGPFirstPatient() {
        val (ehrNo, patientData) = gpIterator.use { gpIterator.next() }
        println(ehrNo)
        println(patientData)
    }

    private fun gpPatient(id: String) = gpIterator.use {
        var pair: Pair<String, String>? = null
        while (gpIterator.hasNext()) {
            val p = gpIterator.next()
            if (p.first == id) {
                pair = p
                break
            }
        }
        pair!!
    }

    @Test
    fun allEhrPatientId() {
        var count = 0
        ehrIterator.use {
            while (ehrIterator.hasNext()) {
                println(ehrIterator.next().first)
                count++
            }
        }
        println("Total: " + count)
    }

    @Test
    fun allGPPatientId() {
        var count = 0
        gpIterator.use {
            while (gpIterator.hasNext()) {
                println(gpIterator.next().first)
                count++
            }
        }
        println("Total: " + count)
    }

    @Test
    fun createAllPatients() {
        var c3cloudId = 1
        ehrIterator.use {
            while (ehrIterator.hasNext()) {
                patientRepository.insert(Patient(ehrIterator.next().first, c3cloudId.toString())).block()
                c3cloudId++
            }
        }
        println("Total: " + (c3cloudId - 1))
    }

    @Test
    fun EHR_first_patient_StructureMap() {
        val (_, data) = ehrFirstPatient()
        val fhirBundle = runBlocking {
            structureMap.transform("provider3:ehr:0.2", MediaType.TEXT_PLAIN_VALUE, data)
        }
        println(fhirBundle)
    }

    @Test
    fun EHR_first_patient__StructrueMap_C3DP() {
        val (id, data) = ehrFirstPatient()
        val txn = EHR_TransactionBundle(id, data)
        val c3dpResponse = runBlocking {
            c3dp.commit(txn)
        }
        println(c3dpResponse)
    }

    @Test
    fun EHR_first_patient_StructrueMap_toTransation_SavetoFile() {
        val (id, data) = ehrFirstPatient()
        val txn = EHR_TransactionBundle(id, data)
        File("src/test/resources/ehr-json-example.json").writeText(txn)
    }

    private fun EHR_TransactionBundle(id: String, data: String, patient: Patient? = null) = runBlocking {
        val fhirBundle = structureMap.transform("provider3:ehr:0.2", MediaType.TEXT_PLAIN_VALUE, data)
        val p = if (patient == null) patientRepository.findById(id).awaitSingle() else patient
        c3dp.toTransactionBundle(listOf(fhirBundle), p).first
    }

    @Test
    fun GP_StructureMap() {
        val (_, data) = gpPatient("4715045106")
        val fhirBundle = runBlocking {
            structureMap.transform("provider3:gp:0.1", MediaType.TEXT_PLAIN_VALUE, data)
        }
        println(fhirBundle)
    }

    @Test
    fun GP_StructrueMap_C3DP() = runBlocking {
        val (id, data) = gpPatient("4725973863")
        val fhirBundle = structureMap.transform("provider3:gp:0.1", MediaType.TEXT_PLAIN_VALUE, data)
        val p = Patient(id, "1000")
        val fhirTxn = c3dp.toTransactionBundle(listOf(fhirBundle), p).first
        val c3dpResponse = c3dp.commit(fhirTxn)
        println(c3dpResponse)
    }

    @Test
    fun GP_StructrueMap_toTransation_SavetoFile() = runBlocking {
        val (id, data) = gpPatient("4875778355")
        val fhirBundle = structureMap.transform("provider3:gp:0.1", MediaType.TEXT_PLAIN_VALUE, data)
        val p = Patient(id, "1000")
        val fhirTxn = c3dp.toTransactionBundle(listOf(fhirBundle), p).first
        File("src/test/resources/GP-json-${id}.json").writeText(fhirTxn)
    }

    @Test
    fun EHR_StructrueMap_toTransation_SavetoFile() {
        ehrIterator.use {
            for (i in 1..3) {
                val (id, data) = ehrIterator.next()
                println("id: ${id}")
                val fhirBundle = EHR_TransactionBundle(id, data)
                File("src/test/resources/EHR-${id}.json").writeText(fhirBundle)
            }
        }
    }

    @Test
    fun EHR_first_patient_filter_all_data_new() = runBlocking {
        val (id, data) = ehrFirstPatient()
        val patient = patientRepository.findById(id).awaitSingle()
        patient.lastImport = null
        println(EHR_TransactionBundle(id, data))
    }

    @Test
    fun EHR_first_patient_filter_all_data_old() = runBlocking {
        val (id, data) = ehrFirstPatient()
        val patient = patientRepository.findById(id).awaitSingle()
        patient.lastImport = Instant.now()
        println(EHR_TransactionBundle(id, data, patient))
    }

    @Test
    fun EHR_first_patient_filter_all_data_after2018() = runBlocking {
        val (id, data) = ehrFirstPatient()
        val patient = patientRepository.findById(id).awaitSingle()
        patient.lastImport = LocalDate.of(2018, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        println(EHR_TransactionBundle(id, data, patient))
    }

    @Test(expected = NoSuchElementException::class)
    fun iteratorCloseTest() {
        println(ehrFirstPatient())
        println(ehrIterator.next())
    }

    @Test
    fun patient_A0001() {
        println("EHR No: A0001")
        runBlocking {
            try {
                println(ehrImportById(mapOf("ehr.file.uri" to ehrFileUri, "patient" to "A0001")))
            }
            catch (e: TaskExecutionException) {
                println(e.details)
            }
        }
    }
}