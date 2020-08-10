package idh.c3cloud.tis

import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.reactive.consumeEach
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitSingle
import java.time.Duration
import java.time.Period
import java.time.temporal.TemporalAmount
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/task")
class TaskScheduler(private val taskScheduleRepository: TaskScheduleRepository,
                    private val taskExecutor: TaskExecutor,
                    private val patientRepository: PatientRepository): CoroutineScope {
    override val coroutineContext = newSingleThreadContext("TaskScheduler")

    private val scheduler = launch {
        while(true) {
            taskScheduleRepository.findAll().filter {it.state == TaskSchedule.State.Active}.consumeEach { schedule ->
                val now = Instant.now();
                if (schedule.until != null && schedule.until.plus(Duration.ofMinutes(1)).isBefore(now)) {   //expired
                    schedule.state = TaskSchedule.State.Complete
                    taskScheduleRepository.save(schedule).awaitSingle()
                }
                else if (schedule.lastExecution == null) {   //first execution
                    if (schedule.start.isBefore(now)) {
                        taskExecutor.execute(schedule.task, schedule.parameters)
                        schedule.lastExecution = now
                        schedule.executionCount++
                        if (schedule.repeat == TaskSchedule.RepeatOption.none) schedule.state = TaskSchedule.State.Complete
                        taskScheduleRepository.save(schedule).awaitSingle()
                    }
                }
                else if (schedule.repeat != TaskSchedule.RepeatOption.none){    //repeat execution
                    val delay: TemporalAmount = when(schedule.repeat) {
                        TaskSchedule.RepeatOption.`1-hour` -> Duration.ofHours(1)
                        TaskSchedule.RepeatOption.`1-day` -> Period.ofDays(1)
                        TaskSchedule.RepeatOption.`1-week` -> Period.ofWeeks(1)
                        TaskSchedule.RepeatOption.`1-month` -> Period.ofMonths(1)
                        else -> Duration.ZERO
                    }
                    if (schedule.lastExecution!!.plus(delay).isBefore(now)) {
                        taskExecutor.execute(schedule.task, schedule.parameters)
                        schedule.lastExecution = now
                        schedule.executionCount++
                        taskScheduleRepository.save(schedule).awaitSingle()
                    }
                }
            }
            delay(TimeUnit.SECONDS.toMillis(30))
        }
    }

    @PostMapping("/schedule")
    fun schedule(@RequestBody taskSchedule: TaskSchedule): Mono<ResponseEntity<String>> {
        val patientIds = taskSchedule.parameters["patient"]
        return checkPatientIdBeforeExecution(patientIds) {
            taskScheduleRepository.insert(taskSchedule).map { ResponseEntity.ok(it.id.toString()) }
        }
    }

    private data class ExecuteRequestBody(val task: String, val parameters: Map<String, String> = emptyMap())

    @PostMapping("/execute")
    private fun execute(@RequestBody request: ExecuteRequestBody): Mono<ResponseEntity<String>> {
        val patientIds = request.parameters["patient"]
        return checkPatientIdBeforeExecution(patientIds) {
            Mono.just(ResponseEntity.ok(taskExecutor.execute(request.task, request.parameters).toString()))
        }
    }

    private fun checkPatientIdBeforeExecution(patients: String?, f: () -> Mono<ResponseEntity<String>>): Mono<ResponseEntity<String>> {
        if (patients == null || patients == "ALL") return f()
        val queried = patients.split(",").map(String::trim)
        return patientRepository
                .findAllById(queried)
                .map(Patient::id)
                .collectList()
                .map { found -> queried - found }
                .flatMap { invalidPatientId ->
                    if (invalidPatientId.isNotEmpty())
                        Mono.just(ResponseEntity.badRequest().body("Patient ID ${invalidPatientId.joinToString()} not found"))
                    else
                        f()
                }
    }

    @PostMapping("/cancel/{id}")
    fun cancelExecution(@PathVariable("id") executionId: ObjectId) {
        println("cancel: " + executionId.date)
    }

}