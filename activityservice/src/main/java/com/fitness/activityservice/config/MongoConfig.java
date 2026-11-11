package com.fitness.activityservice.config;

import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.outbox.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

@Slf4j
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    /** Enables MongoDB transactions (activity + outbox event are saved together). */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }

    @Bean
    public TransactionTemplate transactionTemplate(MongoTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prepareOutbox(ApplicationReadyEvent event) {
        MongoTemplate mongo = event.getApplicationContext().getBean(MongoTemplate.class);

        // Transactions only work on a replica set; say so clearly instead of
        // failing later with a cryptic error on the first saved activity.
        Document hello = mongo.getDb().runCommand(new Document("hello", 1));
        if (hello.getString("setName") == null) {
            log.error("MongoDB is not running as a replica set. Saving activities will fail because the "
                    + "outbox needs transactions. Start MongoDB with --replSet (see docker-compose.yml).");
        }

        // Every activity and analytics query filters by user, most by date too.
        mongo.indexOps(Activity.class).ensureIndex(
                new Index().on("userId", Sort.Direction.ASC).on("startTime", Sort.Direction.DESC));

        var indexes = mongo.indexOps(OutboxEvent.class);
        // The publisher's lookup: pending events that are due, oldest first.
        indexes.ensureIndex(new Index().on("status", Sort.Direction.ASC).on("nextAttemptAt", Sort.Direction.ASC));
        // Published events are deleted automatically after 7 days.
        indexes.ensureIndex(new Index().on("publishedAt", Sort.Direction.ASC).expire(Duration.ofDays(7)));
    }
}
