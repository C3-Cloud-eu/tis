package idh.c3cloud.tis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

@Service
class TaskExecutor(@Value("\${task.pool.size}") private val poolSize: Int,
                   @Value("\${email.notification}") private val emailNotification: Boolean,
                   @Value("\${email.from}") private val emailFrom: String,
                   @Value("\${email.to}") private val emailTo: String,
                   private val emailSender: JavaMailSender): CoroutineScope {
    override val coroutineContext: CoroutineContext = newFixedThreadPoolContext(poolSize, "TaskExecutor")

    private val logger = LoggerFactory.getLogger(TaskExecutor::class.java)

    @Autowired
    private lateinit var springContex: ApplicationContext

    @Autowired
    private lateinit var executionRepository: TaskExecutionRepository

    val executables get() = springContex.getBeanNamesForType(TaskExecutable::class.java)

    private fun executable(id: String): TaskExecutable {
        return springContex.getBean(id, TaskExecutable::class.java)
    }

    private val executions = ConcurrentHashMap<ObjectId, TaskExecution>()

    fun execute(taskId: String, parameters: Map<String, String>) : ObjectId {
        val execution = TaskExecution(taskId)
        execution.job = launch {
            try {
                val executable = executable(taskId)
                log(execution)
                val result = executable(parameters)
                execution.state = TaskExecution.State.Success
                execution.details = result
            }
            catch (e: Exception) {
                execution.state = TaskExecution.State.Error
                execution.details = if (e is TaskExecutionException) e.details else mapOf("error" to e.toString())
                if (emailNotification) {
                    try {
                        sendErrorNotification(execution)
                    }
                    catch (ex: Exception) {
                        logger.error("Sending email error", ex)
                    }
                }
            }
            finally {
                executions.remove(execution.id)
                execution.endTime = Instant.now()
                log(execution)
            }
        }
        executions.put(execution.id, execution)
        return execution.id
    }

    private suspend fun log(execution: TaskExecution) {
        executionRepository.save(execution).awaitSingle()
    }

    private fun sendErrorNotification(execution: TaskExecution) {
        val message = SimpleMailMessage()
        message.setFrom(emailFrom)
        message.setTo(emailTo)
        message.setSubject("[C3-Cloud] Patient Data Synchronization Error")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
        val text = """
                    |This is an automated notification generated by C3-Cloud TIS.
                    |
                    |Task:  ${execution.task}
                    |ID:     ${execution.id}
                    |Start Time:  ${formatter.format(execution.startTime)}
                    |""".trimMargin()
        message.setText(text)
        emailSender.send(message);
    }

    /*@Autowired
    private lateinit var mapper: ObjectMapper

    private fun print(execution: TaskExecution) {
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(execution))
    }*/


}