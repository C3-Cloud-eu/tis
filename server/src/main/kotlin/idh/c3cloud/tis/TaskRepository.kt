package idh.c3cloud.tis

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TaskRepository : ReactiveMongoRepository<Task, String> {
    fun findByNameAndVersion(name: String, version: Int): Mono<Task>
    fun findByName(name: String): Flux<Task>
    fun findByType(type: Task.Type): Flux<Task>
}
