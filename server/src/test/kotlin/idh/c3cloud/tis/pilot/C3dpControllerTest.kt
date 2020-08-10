package idh.c3cloud.tis.pilot

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class C3dpControllerTest {
    @Autowired
    lateinit var testClient: WebTestClient

    @Test
    fun testControllerUrl() {
        val body = mapOf("patient" to "1", "source" to "provider1")
        testClient.post().uri("/api/c3cloud/import")
                .body(BodyInserters.fromObject(body))
                .exchange()
                .expectStatus().isAccepted
    }

}