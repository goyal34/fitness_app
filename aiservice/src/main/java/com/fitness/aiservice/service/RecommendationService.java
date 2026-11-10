package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;

    public List<Recommendation> getUserRecommendation(String userId) {
        return recommendationRepository.findByUserId(userId);
    }

    /** Returns the recommendation only if it belongs to the caller; otherwise 404. */
    public Recommendation getActivityRecommendation(String activityId, String userId) {
        return recommendationRepository.findFirstByActivityIdOrderByCreatedAtDesc(activityId)
                .filter(rec -> rec.getUserId() != null && rec.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No recommendation found for this activity: " + activityId));
    }
}
