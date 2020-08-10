package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api/c3cloud/import")
class C3dpController(private val taskExecutor: TaskExecutor,
                     private val taskRepository: TaskRepository,
                     private val taskExecutionRepository: TaskExecutionRepository,
                     private val patientRepository: PatientRepository,
                     private val c3dp: C3dp): CoroutineScope {

    private val logger = LoggerFactory.getLogger(C3dpController::class.java)

    override val coroutineContext = newSingleThreadContext("C3DP-Notification")

    private val notifier = launch {
        while(true) {
            val notified = mutableSetOf<ObjectId>()
            for (job in jobs) {
                val status = mutableMapOf<String, Any>()
                try {
                    val execution = taskExecutionRepository.findById(job).awaitSingle()
                    if (execution.state == TaskExecution.State.Success)
                        status["success"] = "Data synchronization complete"
                    else if (execution.state == TaskExecution.State.Error) {
                        status["error"] = execution.details["error"] ?: "None"
                        status["message"] = execution.details["message"] ?: "None"
                    }
                    else continue
                    status["timestamp"] = execution.endTime!!
                }
                catch (e : Exception) {
                    logger.error("Getting TaskExecution ${job} for C3DP notification", e)
                    status["error"] = e.toString()
                    status["message"] = "TaskExecution database error"
                }
                try {
                    //c3dp.notifyTest(job, status)
                    c3dp.notify(job, status)
                }
                catch(e: Exception) {
                    logger.error("Notifying C3DP of job ${job} completion", e)
                }
                notified.add(job)
            }
            jobs.removeAll(notified)
            delay(500)
        }
    }

    private val jobs = ConcurrentHashMap.newKeySet<ObjectId>()

    private val tasks = mapOf(
            "provider1" to "Provider1-CDA-DBP-Import-v1",
            "provider2" to "Provider2-OpenServices-Import-v1",
            "provider3" to "Provider3-Import-v1",
            "helloworld" to "HelloWorld-v1"
    )

    @GetMapping("/{site}/{patient}")
    fun importSinglePatient(@PathVariable site: String, @PathVariable patient: String): Mono<ResponseEntity<Map<String, String>>> {
        val taskId = tasks[site.toLowerCase()]
        if (taskId == null) return Mono.just(
                ResponseEntity.badRequest().body(mapOf("error" to "'${site}' not found"))
        )
        return patientRepository
                .existsById(patient)
                .filter { it == false }    // patient not found
                .map {
                    ResponseEntity.badRequest().body(
                            mapOf("error" to "Patient ID ${patient} not found")
                    )
                }.switchIfEmpty(
                        taskRepository.findById(taskId)     // patient found
                        .map { task ->
                            val taskParameters = task.parameters.toMutableMap()
                            taskParameters["patient"] = patient
                            val jobId = taskExecutor.execute(taskId, taskParameters)
                            ResponseEntity.accepted().body(
                                    mapOf(
                                            "success" to "Data synchronization triggered",
                                            "job id" to "${jobId}"
                                    )
                            )
                        })
    }

    @GetMapping("/{site}")
    fun importMultiplePatients(@PathVariable site: String, @RequestParam patient: String): Mono<ResponseEntity<Map<String, String>>> {
        val taskId = tasks[site.toLowerCase()]
        if (taskId == null) return Mono.just(
                ResponseEntity.badRequest().body(mapOf("error" to "'${site}' not found"))
        )
        val patientIds = patient.split(",").map(String::trim)
        return patientRepository
                .findAllById(patientIds)
                .map(Patient::id)
                .collectList()
                .map { found -> patientIds - found }
                .filter { it.isNotEmpty() }     // some patients not found
                .map {
                    ResponseEntity.badRequest().body(
                            mapOf("error" to "Patient ID ${it.joinToString()} not found"))
                }.switchIfEmpty(
                        taskRepository.findById(taskId)     // all patients found
                        .map { task ->
                            val taskParameters = task.parameters.toMutableMap()
                            taskParameters["patient"] = patientIds.joinToString()
                            val jobId = taskExecutor.execute(taskId, taskParameters)
                            ResponseEntity.accepted().body(
                                    mapOf(
                                            "success" to "Data synchronization triggered",
                                            "job id" to "${jobId}"
                                    )
                            )
                        })
    }

    @GetMapping("/job/subscription/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun subscribeJobCompletionNotification(@PathVariable id: ObjectId) {
        jobs.add(id)
    }

    @PostMapping("/job/event/api")
    @ResponseStatus(HttpStatus.OK)
    fun onJobCompletionNotification(@RequestBody body: Map<String, String>) {
        println(body)
    }

}