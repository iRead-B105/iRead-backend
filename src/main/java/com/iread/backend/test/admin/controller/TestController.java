package com.iread.backend.test.admin.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.test.admin.dto.res.TestCompareResponse;
import com.iread.backend.test.admin.dto.res.TestCurriculumDetailResponse;
import com.iread.backend.test.admin.dto.res.TestCurriculumListResponse;
import com.iread.backend.test.admin.dto.res.TestListResponse;
import com.iread.backend.test.admin.dto.res.TestListDataResponse;
import com.iread.backend.test.admin.service.TestService;
import com.iread.backend.test.admin.service.TestCurriculumAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "테스트", description = "관리자 앱 진단 테스트 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/test")
public class TestController {
    private final TestService testService;
    private final TestCurriculumAdminService testCurriculumAdminService;

    @Operation(summary = "학생의 실력도전 검사 목록 조회")
    @GetMapping("/{studentId}/curriculums")
    public TestCurriculumListResponse getTestCurriculums(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return testCurriculumAdminService.getCurriculums(teacherId, studentId);
    }

    @Operation(summary = "학생의 실력도전 검사 결과 상세 조회")
    @GetMapping("/{studentId}/curriculums/{testCurriculumId}")
    public TestCurriculumDetailResponse getTestCurriculum(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @PathVariable Long testCurriculumId
    ) {
        return testCurriculumAdminService.getCurriculum(
                teacherId,
                studentId,
                testCurriculumId
        );
    }

    @Operation(summary = "학생의 완료된 테스트 목록 조회")
    @GetMapping("/{studentId}/list")
    public TestListDataResponse getTestList(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId
    ) {
        return new TestListDataResponse(testService.getTestList(teacherId, studentId));
    }

    @Operation(summary = "현재 테스트와 이전 테스트 결과 비교")
    @GetMapping("/{studentId}/compare")
    public TestCompareResponse compareTests(
            @CurrentTeacherId Long teacherId,
            @PathVariable Long studentId,
            @RequestParam Long currentTestId,
            @RequestParam(required = false) List<Long> comparisonTestIds
    ) {
        return testService.compareTests(teacherId, studentId, currentTestId, comparisonTestIds);
    }

}
