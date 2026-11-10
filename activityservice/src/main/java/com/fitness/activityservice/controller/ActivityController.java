package com.fitness.activityservice.controller;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.service.ActivityService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The caller is always the "sub" of their verified token. */
@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {

    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> trackActivity(@RequestBody ActivityRequest request,
                                                          @AuthenticationPrincipal Jwt jwt){
        // Never trust a userId sent in the body.
        request.setUserId(jwt.getSubject());
        return ResponseEntity.ok(activityService.trackActivity(request));
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivities(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(activityService.getUserActivities(jwt.getSubject()));
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> getActivity(@PathVariable String activityId,
                                                        @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(activityService.getActivityById(activityId, jwt.getSubject()));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable String activityId,
                                               @AuthenticationPrincipal Jwt jwt){
        activityService.deleteActivity(activityId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
