package idh.c3cloud.tis.pilot

import com.fasterxml.jackson.databind.ObjectMapper
import idh.c3cloud.tis.TaskRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class TaskTest {
    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var mapper: ObjectMapper

    @Test
    fun provider1ToJson() {
        //mapper.enable(SerializationFeature.INDENT_OUTPUT)
        //mapper.registerModule(JavaTimeModule())
        //mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(TestData.provider1()))
    }

    @Test
    fun `insert HelloWorld task`() {
        println(taskRepository.insert(TestData.helloWorld()).block())
    }

    @Test
    fun `insert provider1 task`() {
        println(taskRepository.insert(TestData.provider1()).block())
    }

    @Test
    fun `insert provider2 task`() {
        println(taskRepository.insert(TestData.provider2()).block())
    }

    @Test
    fun `insert provider3 task`() {
        println(taskRepository.insert(TestData.provider3()).block())
    }

    @Test
    fun `insert provider3Id task`() {
        println(taskRepository.insert(TestData.provider3Id()).block())
    }

    @Test
    fun `all tasks`() {
        taskRepository.findAll().doOnEach { println(it.get()) }.blockLast()
    }

}