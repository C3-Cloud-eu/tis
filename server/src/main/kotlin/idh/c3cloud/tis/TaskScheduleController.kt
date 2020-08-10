package idh.c3cloud.tis

import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/task/schedule")
class TaskScheduleController(private val taskScheduleRepository: TaskScheduleRepository) {

    @GetMapping
    fun findAll(): Flux<TaskSchedule> {
        return taskScheduleRepository.findAll()
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: ObjectId): Mono<Void> {
        return taskScheduleRepository.deleteById(id)
    }

    @PutMapping("/stop/{id}")
    fun stop(@PathVariable("id") id: ObjectId): Mono<TaskSchedule> {
        return taskScheduleRepository.findById(id).flatMap { schedule ->
            //println(schedule)
            if (schedule.state == TaskSchedule.State.Active) {
                schedule.state = TaskSchedule.State.Stopped
                taskScheduleRepository.save(schedule)
            }
            else Mono.just(schedule)
        }
    }

}