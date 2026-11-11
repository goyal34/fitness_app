package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.common.event.ActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fitness.activityservice.outbox.OutboxEvent;
import com.fitness.activityservice.outbox.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;

    public ActivityResponse trackActivity(ActivityRequest request) {

        boolean isValidUser = userValidationService.validateUser(request.getUserId());
        if (!isValidUser) {
            throw new RuntimeException("Invalid User: " + request.getUserId());
        }

        validate(request);

        Activity activity = Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                // Analytics group workouts by this date; default to "now" when not given.
                .startTime(request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();

        // Transactional Outbox: the activity and the "activity created" event are
        // saved in one MongoDB transaction, so there is never an activity without
        // its event (or the reverse). OutboxPublisher sends the event to RabbitMQ.
        Activity savedActivity = transactionTemplate.execute(status -> {
            Activity saved = activityRepository.save(activity);
            outboxEventRepository.save(OutboxEvent.activityCreated(toEvent(saved)));
            return saved;
        });

        return mapToResponse(savedActivity);
    }

    /** The outbox stores the shared wire contract, not the MongoDB document. */
    private static ActivityEvent toEvent(Activity activity) {
        return ActivityEvent.builder()
                .id(activity.getId())
                .userId(activity.getUserId())
                .type(activity.getType() != null ? activity.getType().name() : null)
                .duration(activity.getDuration())
                .caloriesBurned(activity.getCaloriesBurned())
                .startTime(activity.getStartTime())
                .additionalMetrics(activity.getAdditionalMetrics())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    private void validate(ActivityRequest request) {
        if (request.getType() == null) {
            throw badRequest("type is required");
        }
        if (request.getDuration() == null || request.getDuration() <= 0 || request.getDuration() > 1440) {
            throw badRequest("duration must be between 1 and 1440 minutes");
        }
        if (request.getCaloriesBurned() == null || request.getCaloriesBurned() < 0 || request.getCaloriesBurned() > 20000) {
            throw badRequest("caloriesBurned must be between 0 and 20000");
        }
        // A few minutes of slack for clock differences between browser and server.
        if (request.getStartTime() != null && request.getStartTime().isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw badRequest("startTime cannot be in the future");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ActivityResponse mapToResponse(Activity activity){
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        return response;
    }

    public List<ActivityResponse> getUserActivities(String userId) {
        List<Activity> activities = activityRepository.findByUserId(userId);
        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ActivityResponse getActivityById(String activityId, String userId) {
        return mapToResponse(findOwnedActivity(activityId, userId));
    }

    public void deleteActivity(String activityId, String userId) {
        activityRepository.delete(findOwnedActivity(activityId, userId));
    }

    /**
     * Loads an activity only if it belongs to the given user. "Not found" and
     * "not yours" give the same 404, so other users' ids can't be probed.
     */
    private Activity findOwnedActivity(String activityId, String userId) {
        return activityRepository.findById(activityId)
                .filter(activity -> activity.getUserId() != null && activity.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Activity not found with id: " + activityId));
    }
}
