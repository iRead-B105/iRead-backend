package com.iread.backend.training.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.training.app.dto.res.DemoLearningDateResponse;
import com.iread.backend.training.app.service.DemoLearningCheatService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
@RequiredArgsConstructor
@RequestMapping("/api/admin/dev/students")
public class DemoLearningDateController {

    private final DemoLearningCheatService cheatService;

    @GetMapping("/{studentId}/date")
    public DemoLearningDateResponse currentDate(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return cheatService.currentDate(teacherId, studentId);
    }
}
