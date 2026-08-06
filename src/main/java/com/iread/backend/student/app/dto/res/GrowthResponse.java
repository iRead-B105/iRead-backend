package com.iread.backend.student.app.dto.res;

import java.util.List;

public record GrowthResponse(
        /** 훈련을 완료한 서로 다른 날짜 수. 아동 앱의 "학습 N일차" 표시에 쓴다. */
        long studyDayCount,
        List<TrainingProgressResponse> trainingProgress,
        List<GrowthAreaResponse> growthAreas
) {
}
