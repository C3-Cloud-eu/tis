package idh.c3cloud.tis

import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/task")
class TaskController(private val taskRepository: TaskRepository,
                     private val taskExecutor: TaskExecutor) {

    @PostMapping
    fun create(@RequestBody task: Task): Mono<Void> {
        return taskRepository.insert(task).then()
    }

    @GetMapping
    fun findAll(): Flux<Task> {
        return taskRepository.findAll()
    }

    @GetMapping("/import")
    fun findAllImportTasks(): Flux<Task> {
        return taskRepository.findByType(Task.Type.Import)
    }

}