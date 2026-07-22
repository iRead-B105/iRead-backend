package com.iread.backend.mypage.app.controller;

import com.iread.backend.auth.annotation.CurrentTeacherId;
import com.iread.backend.mypage.app.dto.res.CharacterResponse;
import com.iread.backend.mypage.app.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/mypage")
public class MypageController {
    private final MypageService mypageService;

    @GetMapping("/character")
    public List<CharacterResponse> getCharacters(
            @CurrentTeacherId Long teacherId,
            @RequestParam String studentCode
    ) {
        return mypageService.getCharacters(teacherId, studentCode);
    }
}
