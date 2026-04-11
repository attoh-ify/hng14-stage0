package hng14.stage0.nameclassifier.controller;

import hng14.stage0.nameclassifier.dto.response.ClassifyDataResponse;
import hng14.stage0.nameclassifier.dto.response.SuccessResponse;
import hng14.stage0.nameclassifier.service.ClassifyService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/classify")
public class ClassifyController {
    private final ClassifyService classifyService;

    public ClassifyController(ClassifyService classifyService) {
        this.classifyService = classifyService;
    }

    @GetMapping
    public SuccessResponse classify(@RequestParam(name = "name") String name) {
        ClassifyDataResponse response = classifyService.classify(name);
        return new SuccessResponse("success", response);
    }
}
