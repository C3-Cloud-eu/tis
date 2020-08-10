package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.TaskExecutable
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

@Component("HelloWorld-v1")
class HelloWorld : TaskExecutable {

    override suspend fun invoke(parameters: Map<String, String>): Map<String, Any> {
        delay(30000)
        println("Hello world! ${parameters}")
        return emptyMap()
    }

}