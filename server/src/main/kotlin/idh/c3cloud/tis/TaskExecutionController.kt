package idh.c3cloud.tis

import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/task/execution")
class TaskExecutionController(private val taskExecutionRepository: TaskExecutionRepository) {

    @GetMapping
    fun findAll(): Flux<TaskExecution> {
        return taskExecutionRepository.findAll()
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: ObjectId): Mono<Void> {
        return taskExecutionRepository.deleteById(id)
    }

    @DeleteMapping
    fun delete(): Mono<Void> {
        return taskExecutionRepository.deleteAll()
    }

}