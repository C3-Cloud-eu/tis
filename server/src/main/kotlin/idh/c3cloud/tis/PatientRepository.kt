package idh.c3cloud.tis

import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface PatientRepository : ReactiveMongoRepository<Patient, String>