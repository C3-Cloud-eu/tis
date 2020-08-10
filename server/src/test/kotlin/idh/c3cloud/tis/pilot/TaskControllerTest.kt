package idh.c3cloud.tis.pilot

import idh.c3cloud.tis.Task
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskControllerTest {
    @Autowired
    lateinit var testClient: WebTestClient

    @Test
    fun insertTask() {
        testClient.post().uri("/api/task")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(TestData.provider1()), Task::class.java)
                .exchange()
                .expectStatus().isOk()
    }

    @Test
    fun allImportTasks() {
        testClient.get().uri("/api/task/import")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk()
                //.expectBody().consumeWith { println(it.responseBody) }
    }

    @Test
    fun cancelTaskExecution() {
        testClient.post().uri("/api/task/cancel/5afea45e69b4522b10c953fd")
                //.contentType(MediaType.APPLICATION_JSON_UTF8)
                //.accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk()
    }

}