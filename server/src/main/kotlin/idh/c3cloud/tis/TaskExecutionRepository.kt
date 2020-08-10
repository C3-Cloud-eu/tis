package idh.c3cloud.tis

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface TaskExecutionRepository : ReactiveMongoRepository<TaskExecution, ObjectId>