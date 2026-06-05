package com.itheima.travelagent2.modules.timeline;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.timeline.entity.TravelTimelineItem;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timeline")
public class TravelTimelineController {
    private final TravelTimelineService service;

    public TravelTimelineController(TravelTimelineService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public Result<List<TravelTimelineItem>> generate(@RequestParam(defaultValue = "test_user") String userId,
                                                     @RequestBody(required = false) StructuredTravelDemand demand) {
        return Result.success(service.generate(userId, null, demand));
    }

    @GetMapping("/user/{userId}")
    public Result<List<TravelTimelineItem>> listByUser(@PathVariable String userId) {
        return Result.success(service.listByUserId(userId));
    }
}
