package com.itheima.travelagent2.modules.tripcontext;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.tripcontext.entity.TripContext;
import com.itheima.travelagent2.modules.tripcontext.service.TripContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trip-context")
public class TripContextController {

    private final TripContextService tripContextService;

    public TripContextController(TripContextService tripContextService) {
        this.tripContextService = tripContextService;
    }

    @GetMapping("/current")
    public Result<TripContext> current(@RequestParam String sessionId) {
        return Result.success(tripContextService.getBySessionId(sessionId));
    }
}
