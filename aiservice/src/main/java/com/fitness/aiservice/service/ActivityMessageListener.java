package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService aiService;
    private final RecommendationRepository recommendationRepository;

    /**
     * Never throws: a thrown exception would put the message back on the queue
     * and retry it forever (burning Gemini quota). Failures are saved as a
     * FAILED report instead, so the UI can show what went wrong.
     */
    @RabbitListener(queues = "activity.queue")
    public void processActivity(Activity activity) {
        log.info("Received activity for processing: {}", activity.getId());

        // The outbox delivers at least once, so the same activity can arrive twice.
        // Don't pay for a second Gemini call if a report already exists.
        Optional<Recommendation> existing =
                recommendationRepository.findFirstByActivityIdOrderByCreatedAtDesc(activity.getId());
        if (existing.isPresent() && Recommendation.STATUS_COMPLETED.equals(existing.get().getStatus())) {
            log.info("Activity {} already has a report; ignoring duplicate message.", activity.getId());
            return;
        }

        Recommendation result;
        try {
            result = aiService.generateRecommendation(activity);
        } catch (WebClientResponseException e) {
            log.error("Gemini request failed for activity {}: {} {}", activity.getId(),
                    e.getStatusCode(), e.getResponseBodyAsString());
            result = aiService.failedRecommendation(activity, describe(e));
        } catch (Exception e) {
            log.error("Could not generate a report for activity {}", activity.getId(), e);
            result = aiService.failedRecommendation(activity, e.getMessage());
        }

        // One report per activity: replace an earlier FAILED one instead of adding another.
        Recommendation toSave = result;
        existing.ifPresent(previous -> toSave.setId(previous.getId()));
        recommendationRepository.save(toSave);
    }

    private String describe(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 400 || status == 401 || status == 403) {
            return "The Gemini API rejected the request. Check the API key and model name.";
        }
        if (status == 404) {
            return "The Gemini model in the configuration was not found.";
        }
        if (status == 429) {
            return "The Gemini API quota or rate limit was reached. Try again later.";
        }
        return "The Gemini API is unavailable right now (HTTP " + status + ").";
    }
}
