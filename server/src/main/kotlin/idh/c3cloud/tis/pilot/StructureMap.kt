package idh.c3cloud.tis.pilot

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
final class StructureMap(@Value("\${structure-map.uri}") val uri: String,
                         webClientBuilder: WebClient.Builder) {
    private val webClient = webClientBuilder.clone().baseUrl(uri).build()

    /**
     * Expects 3 parameters: mapperInstance, contentType, (payload) data, all as String.
     */
    suspend fun transform(mapperInstance: String, contentType: String, data: String): String {
        return webClient.post().uri { uriBuilder -> uriBuilder
                .queryParam("format", "json")
                .queryParam("mapperInstance", mapperInstance)
                .build() }
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .syncBody(data)
                .retrieve()
                .bodyToMono<String>()
                .awaitSingle()
    }

}





