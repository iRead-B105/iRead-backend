package com.iread.backend.test.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.test.admin.dto.res.TestCompareResponse;
import com.iread.backend.test.admin.dto.res.TestListResponse;
import com.iread.backend.test.admin.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/test")
public class TestController {
    private final TestService testService;

    @GetMapping("/{studentId}/list")
    public List<TestListResponse> getTestList(@CurrentTeacherId Long teacherId, @PathVariable Long studentId) {
        return testService.getTestList(teacherId, studentId);
    }

    @GetMapping("/{studentId}/compare")
    public TestCompareResponse compareTests(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @RequestParam Long currentTestId,
            @RequestParam List<Long> comparisonTestIds
    ) {
        return testService.compareTests(teacherId, studentId, currentTestId, comparisonTestIds);
    }

}
