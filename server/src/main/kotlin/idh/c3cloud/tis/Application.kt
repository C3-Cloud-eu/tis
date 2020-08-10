package idh.c3cloud.tis

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.mongodb.WriteConcern
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.bson.types.ObjectId
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.runApplication
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.WriteResultChecking
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient

@EnableReactiveMongoRepositories
@SpringBootApplication//(exclude = [ReactiveSecurityAutoConfiguration::class, ReactiveUserDetailsServiceAutoConfiguration::class])
class Application : CommandLineRunner {
    /*@Value("classpath:/static/index.html")
    private lateinit var indexHtml: Resource

    @Bean
    fun indexRouter() = router {
        GET("/") {
            ok().contentType(TEXT_HTML).syncBody(indexHtml)
        }
    }*/

    private val keepAliveTimeout = 60

    @Bean
    fun staticResourceFilter() =
        object : WebFilter {
            override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
                val path = exchange.getRequest().getURI().getPath()
                //println(path)
                if (path == "/" || (!path.startsWith("/api/") && !ClassPathResource("static" + path).exists())) {
                    return chain.filter(exchange.mutate().request(exchange.getRequest().mutate().path("/index.html").build()).build())
                }
                return chain.filter(exchange)
            }
        }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.csrf().disable()
                .authorizeExchange()
                .pathMatchers("/api/**", "/manifest.json", "/favicon.ico").permitAll()
                .anyExchange().authenticated()
                .and()
                .formLogin()
                //.authenticationSuccessHandler(ServerAuthenticationSuccessHandler {
                //    webFilterExchange, _ -> DefaultServerRedirectStrategy().sendRedirect(webFilterExchange.exchange, URI("/"))
                //})
        return http.build()
    }

    @Bean
    fun webConfig(): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/api/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedOrigins("*");
                        //.allowedOrigins("http://localhost:3000");
            }
        }
    }

    @Bean
    fun clientCredentialsOauth2ClientRepository() =
            UnAuthenticatedServerOAuth2AuthorizedClientRepository()

    @Bean
    fun clientCredentialsWebClientFilter(clientRegistrationRepository: ReactiveClientRegistrationRepository,
                                         clientRepository: UnAuthenticatedServerOAuth2AuthorizedClientRepository) =
            ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository, clientRepository)

    @Bean
    fun jacksonCustomizer() = object: Jackson2ObjectMapperBuilderCustomizer {
            override fun customize(jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder) {
                jacksonObjectMapperBuilder.serializerByType(ObjectId::class.java, ToStringSerializer())
            }
    }

    @Bean
    fun nettyClientCustomizer() = object: WebClientCustomizer {
        override fun customize(webClientBuilder: WebClient.Builder) {
            val httpClient = HttpClient.create().tcpConfiguration { client -> client
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                    .doOnConnected { conn -> conn
                            .addHandlerLast(ReadTimeoutHandler(keepAliveTimeout))
                            .addHandlerLast(WriteTimeoutHandler(keepAliveTimeout))
                    }
            }
            webClientBuilder.clientConnector(ReactorClientHttpConnector(httpClient))
        }
    }

    @Bean
    fun nettyServerCustomizer() = object: WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
        override fun customize(factory: NettyReactiveWebServerFactory) {
            factory.addServerCustomizers(NettyServerCustomizer { server -> server
                    .tcpConfiguration { tcp -> tcp
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                            .doOnConnection { conn -> conn
                                    .addHandlerLast(ReadTimeoutHandler(keepAliveTimeout))
                                    .addHandlerLast(WriteTimeoutHandler(keepAliveTimeout))
                            }
                    }
            })
        }
    }

    @Bean
    fun postProcessor() = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
            if (bean is MappingMongoConverter) {
                //println("Customize MappingMongoConverter")
                bean.setTypeMapper(DefaultMongoTypeMapper(null))
            }
            if (bean is ReactiveMongoTemplate) {
                //println("Customize ReactiveMongoTemplate")
                bean.setWriteResultChecking(WriteResultChecking.EXCEPTION);
                bean.setWriteConcern(WriteConcern.ACKNOWLEDGED);
            }
            return super.postProcessAfterInitialization(bean, beanName)
        }
    }

    override fun run(vararg args: String?) {}
}

fun main(args: Array<String>) {
    //println(System.getProperty("user.dir"))
    runApplication<Application>(*args)
}
