package idh.c3cloud.tis.pilot

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class OpenServices(webClientBuilder: WebClient.Builder,
                   clientCredentialsWebClientFilter: ServerOAuth2AuthorizedClientExchangeFilterFunction) {
    private val webClient =
            webClientBuilder.clone()
                    .baseUrl("https://{{API_URL}}")
                    .defaultHeader("Ocp-Apim-Subscription-Key", "{{secret-key}}")
                    .filter(clientCredentialsWebClientFilter)
                    .build()
    private val clientId = "open-services"

    suspend fun getPatient(id: String) =
            webClient
                    .get()
                    .uri("/patients/patient/")
                    .header("patient", id)
                    .attributes(clientRegistrationId(clientId))
                    .retrieve()
                    .bodyToMono<String>()
                    .awaitSingle()

}