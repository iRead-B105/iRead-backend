package com.iread.backend.mypage.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.mypage.app.dto.res.CharacterResponse;
import com.iread.backend.mypage.app.service.MypageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "마이페이지", description = "훈련 앱 마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/mypage")
public class MypageController {
    private final MypageService mypageService;

    @Operation(summary = "학생이 보유한 캐릭터 목록 조회")
    @GetMapping("/character")
    public List<CharacterResponse> getCharacters(
            @CurrentTeacherId Long teacherId,
            @RequestParam Long studentId
    ) {
        return mypageService.getCharacters(teacherId, studentId);
    }
}
