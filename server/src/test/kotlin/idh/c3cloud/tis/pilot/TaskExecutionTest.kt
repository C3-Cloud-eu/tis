package idh.c3cloud.tis.pilot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import idh.c3cloud.tis.TaskExecution
import idh.c3cloud.tis.TaskExecutionRepository
import org.bson.types.ObjectId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class TaskExecutionTest {
    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    lateinit var taskExecutionRepository: TaskExecutionRepository

    @Test
    fun toJson() {
        val task = TestData.provider1()
        val parameters = mapOf("patient" to "1")
        val execution = TaskExecution(task.id)
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(execution))
    }

    @Test
    fun fromJson() {
        val execution = mapper.readValue<TaskExecution>(TestUtil.getFile("task-execution-1.json"))
        assertEquals(execution.task, "Provider1-CDA-Service-v1")
        assertEquals(execution.id, ObjectId("5afe9e1869b4522d08fdb74f"))
    }

    @Test
    fun findAll() {
        taskExecutionRepository.findAll().doOnEach { println(it.get()) }.blockLast()
    }

}