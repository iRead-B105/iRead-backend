package com.iread.backend.story.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.story.domain.*;
import com.iread.backend.story.repository.*;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock StoryTemplateRepository storyTemplateRepository;
    @Mock StoryRepository storyRepository;
    @Mock StorySceneRepository storySceneRepository;
    @Mock StoryLineRepository storyLineRepository;
    @Mock StoryChoiceRepository storyChoiceRepository;
    @Mock AiClient aiClient;
    @Mock StoryAudioStorage storyAudioStorage;
    @InjectMocks StoryService storyService;

    private StudentEntity student;
    private StoryTemplateEntity template;

    @BeforeEach
    void setUp() {
        student = mock(StudentEntity.class);
        template = mock(StoryTemplateEntity.class);
        lenient().when(student.getId()).thenReturn(20L);
        lenient().when(template.getId()).thenReturn(30L);
        lenient().when(template.getTitle()).thenReturn("신비한 숲");
        lenient().when(template.getContent()).thenReturn("숲에서 친구를 만나는 이야기");
    }

    @Test
    void 책장은_삭제되지_않은_학생_스토리와_모든_템플릿을_반환한다() {
        StoryEntity story = story(100L);
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyRepository.findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(20L, StoryStatus.DELETED))
                .thenReturn(List.of(story));
        when(storyTemplateRepository.findAllByOrderByIdAsc()).thenReturn(List.of(template));

        var response = storyService.getStoryShelf(1L, 20L);

        assertThat(response.stories()).hasSize(1);
        assertThat(response.stories().getFirst().storyId()).isEqualTo(100L);
        assertThat(response.storyTemplates()).hasSize(1);
        assertThat(response.storyTemplates().getFirst().storyTemplateId()).isEqualTo(30L);
    }

    @Test
    void 신규_세션은_첫_선택지까지_생성하여_순서대로_저장한다() {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyTemplateRepository.findById(30L)).thenReturn(Optional.of(template));
        when(storyRepository.saveAndFlush(any(StoryEntity.class))).thenAnswer(invocation -> {
            StoryEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 7, 22, 10, 0));
            return saved;
        });
        when(aiClient.generateStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored-by-service-mock",
                1,
                50,
                false,
                List.of(
                        new GeneratedStoryLine("숲에 도착했어요.", false),
                        new GeneratedStoryLine("어디로 갈까요?", true)
                )
        ));
        mockSceneSave(200L);
        mockLineSave(1000L);

        var response = storyService.startStory(1L, 20L, 30L);

        assertThat(response.storyId()).isEqualTo(100L);
        assertThat(response.storyStatus()).isEqualTo(StoryStatus.IN_PROGRESS);
        ArgumentCaptor<List<StoryLineEntity>> linesCaptor = listCaptor();
        verify(storyLineRepository).saveAllAndFlush(linesCaptor.capture());
        List<StoryLineEntity> lines = linesCaptor.getValue();
        assertThat(lines).extracting(StoryLineEntity::getSequenceNo).containsExactly(1, 2);
        assertThat(lines).extracting(StoryLineEntity::getContent)
                .containsExactly("숲에 도착했어요.", "어디로 갈까요?");
        assertThat(lines.get(1).getPreviousStoryLine()).isSameAs(lines.getFirst());
        assertThat(lines.getLast().isRequiresBranchInput()).isTrue();
    }

    @Test
    void 장면을_조회하면_최초_조회_시각을_읽은_일자로_저장한다() {
        StoryEntity story = story(100L);
        StoryLineEntity line = line(1000L, story, null, false, "첫 대사", 1, null);
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryId(1000L, 100L)).thenReturn(Optional.of(line));

        var response = storyService.getStoryLine(1L, 20L, 100L, 1000L);

        assertThat(response.readAt()).isNotNull();
        assertThat(line.getReadAt()).isEqualTo(response.readAt());
    }

    @Test
    void 읽지_않은_대사가_없으면_답하지_않은_마지막_선택지에서_재개한다() {
        StoryEntity story = story(100L);
        StoryLineEntity choiceLine = line(1001L, story, null, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        ownedStory(story);
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(choiceLine));
        var response = storyService.resumeStory(1L, 20L, 100L);

        assertThat(response.storyLines().getFirst().lineId()).isEqualTo(1001L);
    }

    @Test
    void 자연어_선택지를_저장하고_다음_대사를_생성한_뒤_완료한다() {
        StoryEntity story = story(100L);
        StoryLineEntity firstLine = line(1000L, story, null, false, "숲에 도착했어요.", 1,
                LocalDateTime.of(2026, 7, 22, 10, 5));
        StoryLineEntity choiceLine = line(1001L, story, firstLine, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L)).thenReturn(Optional.of(choiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.empty());
        when(storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(100L))
                .thenReturn(Optional.of(choiceLine));
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(firstLine, choiceLine));
        when(aiClient.continueStory(any())).thenReturn(new GenerateStoryResponse(
                "ignored-by-service-mock",
                1,
                100,
                true,
                List.of(new GeneratedStoryLine("모두와 인사하고 집으로 돌아왔어요.", false))
        ));
        when(aiClient.transcribeSpeech(anyString(), eq(20L), isNull(), any()))
                .thenReturn(new SpeechTranscriptionResponse(
                        "ignored-by-service-mock",
                        "이야기를 끝내고 집으로 간다",
                        1.0,
                        1_000
                ));
        mockSceneSave(201L);
        mockLineSave(1002L);
        when(storyChoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            StoryChoiceEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 300L);
            return saved;
        });

        var response = storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        );

        assertThat(response.choiceId()).isEqualTo(300L);
        assertThat(response.transcript()).isEqualTo("이야기를 끝내고 집으로 간다");
        assertThat(response.nextSceneId()).isEqualTo(201L);
        assertThat(response.nextLineId()).isEqualTo(1002L);
        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.replayed()).isFalse();

        ArgumentCaptor<ContinueStoryRequest> requestCaptor = ArgumentCaptor.forClass(ContinueStoryRequest.class);
        verify(aiClient).continueStory(requestCaptor.capture());
        assertThat(requestCaptor.getValue().branchIntent()).isEqualTo("이야기를 끝내고 집으로 간다");
        assertThat(requestCaptor.getValue().currentStoryLineId()).isEqualTo(1001L);
    }

    @Test
    void 저장된_분기를_재요청하면_음성과_AI를_다시_처리하지_않는다() {
        StoryEntity story = story(100L);
        StoryLineEntity choiceLine = line(1001L, story, null, true, "어떻게 할까요?", 2,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        StoryLineEntity nextLine = line(1002L, story, choiceLine, false, "친구를 만났어요.", 1,
                null);
        StoryChoiceEntity choice = new StoryChoiceEntity(choiceLine, "친구를 따라간다");
        ReflectionTestUtils.setField(choice, "id", 300L);
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L))
                .thenReturn(Optional.of(choiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.of(choice));
        when(storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(100L))
                .thenReturn(List.of(choiceLine, nextLine));

        var response = storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        );

        assertThat(response.choiceId()).isEqualTo(300L);
        assertThat(response.nextLineId()).isEqualTo(1002L);
        assertThat(response.replayed()).isTrue();
        verifyNoInteractions(aiClient, storyAudioStorage);
    }

    @Test
    void 마지막_대사가_아닌_분기에는_답할_수_없다() {
        StoryEntity story = story(100L);
        StoryLineEntity oldChoiceLine = line(1001L, story, null, true, "예전 선택", 1,
                LocalDateTime.of(2026, 7, 22, 10, 10));
        StoryLineEntity currentChoiceLine = line(1002L, story, oldChoiceLine, true, "현재 선택", 2,
                LocalDateTime.of(2026, 7, 22, 10, 11));
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L))
                .thenReturn(Optional.of(oldChoiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.empty());
        when(storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(100L))
                .thenReturn(Optional.of(currentChoiceLine));

        assertThatThrownBy(() -> storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("현재 마지막 분기 장면에만 답할 수 있습니다.");

        verifyNoInteractions(aiClient, storyAudioStorage);
    }

    @Test
    void 읽지_않은_분기에는_답할_수_없다() {
        StoryEntity story = story(100L);
        StoryLineEntity unreadChoiceLine = line(1001L, story, null, true, "어떻게 할까요?", 1, null);
        ownedStory(story);
        when(storyLineRepository.findByIdAndStoryIdForUpdate(1001L, 100L))
                .thenReturn(Optional.of(unreadChoiceLine));
        when(storyChoiceRepository.findByStoryLineId(1001L)).thenReturn(Optional.empty());
        when(storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(100L))
                .thenReturn(Optional.of(unreadChoiceLine));

        assertThatThrownBy(() -> storyService.chooseStoryDirection(
                1L, 20L, 100L, 1001L,
                new MockMultipartFile("audioFile", "answer.webm", "audio/webm", new byte[]{1})
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("장면을 읽은 후 선택지를 제출할 수 있습니다.");

        verifyNoInteractions(aiClient, storyAudioStorage);
    }

    private void ownedStory(StoryEntity story) {
        when(studentRepository.findByIdAndTeacherId(20L, 1L)).thenReturn(Optional.of(student));
        when(storyRepository.findByIdAndStudentId(100L, 20L)).thenReturn(Optional.of(story));
    }

    private StoryEntity story(Long id) {
        StoryEntity story = new StoryEntity(student, template);
        ReflectionTestUtils.setField(story, "id", id);
        ReflectionTestUtils.setField(story, "createdAt", LocalDateTime.of(2026, 7, 22, 10, 0));
        return story;
    }

    private StoryLineEntity line(Long id, StoryEntity story, StoryLineEntity previous, boolean requiresBranchInput,
                                 String content, int sequenceNo, LocalDateTime readAt) {
        StorySceneEntity scene = new StorySceneEntity(story, null, 1);
        ReflectionTestUtils.setField(scene, "id", 200L);
        StoryLineEntity line = new StoryLineEntity(
                previous, scene, requiresBranchInput, content, sequenceNo
        );
        ReflectionTestUtils.setField(line, "id", id);
        ReflectionTestUtils.setField(line, "createdAt", LocalDateTime.of(2026, 7, 22, 10, sequenceNo));
        ReflectionTestUtils.setField(line, "readAt", readAt);
        return line;
    }

    private void mockLineSave(long firstId) {
        when(storyLineRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<StoryLineEntity> lines = new ArrayList<>(invocation.getArgument(0));
            AtomicLong nextId = new AtomicLong(firstId);
            lines.forEach(line -> {
                ReflectionTestUtils.setField(line, "id", nextId.getAndIncrement());
                ReflectionTestUtils.setField(line, "createdAt", LocalDateTime.of(2026, 7, 22, 11, line.getSequenceNo()));
            });
            return lines;
        });
    }

    private void mockSceneSave(long id) {
        when(storySceneRepository.countByStoryId(anyLong())).thenReturn(id == 200L ? 0L : 1L);
        when(storySceneRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            StorySceneEntity scene = invocation.getArgument(0);
            ReflectionTestUtils.setField(scene, "id", id);
            return scene;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<StoryLineEntity>> listCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
