package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.TaskExecution
import idh.c3cloud.tis.TaskExecutor
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class TaskExecutorTest {
    @Autowired
    private lateinit var taskExecutor: TaskExecutor

    private lateinit var executions: Map<ObjectId, TaskExecution>

    @Before
    fun getExecutions() {
        executions = TestUtil.getProperty(taskExecutor, "executions") as Map<ObjectId, TaskExecution>
    }

    /*@Test
    fun emptyExecutions() {
        assertEquals(executions, emptyMap<ObjectId, TaskExecution>())
    }*/

    /*@Test
    fun poolSize() {
        //println(taskExecutor.poolSize)
        assertEquals(taskExecutor.poolSize, 20)
    }*/

    @Test
    fun taskExecutables() {
        //println(taskExecutor.definedTaskFunctions.contentToString())
        assertTrue("provider1CdaService" in taskExecutor.executables)
    }

    @Test
    fun `provider1CdaTask - 123456`() = provider1CdaTask("123456")

    private fun provider1CdaTask(patientId: String) = runBlocking {
        val task = TestData.provider1()
        val parameters = mapOf("patient" to patientId)
        val id = taskExecutor.execute(task.id, parameters)
        val execution = executions[id]
        execution!!.job!!.join()
        assertTrue(executions.isEmpty())
    }

    @Test
    fun `Provider3 by id 147`() {
        val task = TestData.provider3Id()
        val parameters = task.parameters.mapValues { (k, v) -> if (k == "patient") "147" else v }
        val id = taskExecutor.execute(task.id, parameters)
        val execution = executions[id]
        runBlocking {
            execution?.job?.join()
        }
    }

    @Test
    fun `Provider3 import all`() {
        val task = TestData.provider3()
        val id = taskExecutor.execute(task.id, task.parameters)
        val execution = executions[id]
        runBlocking { execution?.job?.join() }
    }

}

