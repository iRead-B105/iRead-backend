package com.iread.backend.training.app.service;

import com.iread.backend.training.repository.TrainingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 아동의 학습 날짜를 계산한다. 치트가 꺼져 있으면 finishedAt 이 오늘을 앞설 수 없어
 * 달력상 오늘로 동작하므로, 치트 게이트와 무관하게 항상 빈으로 등록한다.
 * 치트 API 자체의 노출은 {@code iread.demo-cheat.enabled} 조건이 컨트롤러에서 막는다.
 */
@Component
public class DemoLearningClock {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final TrainingRepository trainingRepository;
    private final Clock clock;

    @Autowired
    public DemoLearningClock(TrainingRepository trainingRepository) {
        this(trainingRepository, Clock.system(SEOUL));
    }

    DemoLearningClock(TrainingRepository trainingRepository, Clock clock) {
        this.trainingRepository = trainingRepository;
        this.clock = clock;
    }

    public LocalDate currentDate(Long studentId) {
        LocalDate baseDate = baseDate();
        return trainingRepository.findLatestFinishedAtByStudentId(studentId)
                .map(LocalDateTime::toLocalDate)
                .filter(latestDate -> latestDate.isAfter(baseDate))
                .orElse(baseDate);
    }

    public LocalDate baseDate() {
        return LocalDate.now(clock);
    }

    public LocalDateTime currentDateTime(Long studentId) {
        LocalTime currentTime = LocalTime.now(clock);
        return LocalDateTime.of(currentDate(studentId), currentTime);
    }

    public LocalDateTime nextDateTime(Long studentId) {
        return currentDateTime(studentId).plusDays(1);
    }
}
