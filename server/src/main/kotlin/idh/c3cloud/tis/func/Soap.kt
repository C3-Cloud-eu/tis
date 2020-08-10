package idh.c3cloud.tis.func

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.toMono

@Component
class SoapClient(builder: WebClient.Builder) {
    private val webClient = builder.build()

    suspend operator fun invoke(url: String, msg: String): String {
        return webClient
                .post()
                .uri(url)
                .contentType(MediaType.TEXT_XML)
                .syncBody(msg)
                .retrieve()
                .bodyToMono<String>()
                .awaitSingle()

}


}

