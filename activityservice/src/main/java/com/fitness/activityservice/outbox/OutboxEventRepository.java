package com.fitness.activityservice.outbox;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    long countByStatus(OutboxStatus status);
}
