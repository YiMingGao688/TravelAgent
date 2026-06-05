package com.itheima.travelagent2.modules.intent;

import com.itheima.travelagent2.common.Result;
import com.itheima.travelagent2.modules.intent.dto.StructuredTravelDemand;
import com.itheima.travelagent2.modules.intent.dto.TravelIntentDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intent")
public class IntentParseController {
    private final IntentParseService intentParseService;

    public IntentParseController(IntentParseService intentParseService) {
        this.intentParseService = intentParseService;
    }

    @PostMapping("/parse")
    public Result<StructuredTravelDemand> parse(@RequestBody TravelIntentDTO dto) {
        return Result.success(intentParseService.parse(dto));
    }
}
