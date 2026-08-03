package com.iread.backend.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RealtimeEventHubTest {

    @Test
    void dropsFailedConnectionWithoutCompletingCommittedResponseAgain() throws Exception {
        RealtimeEventHub hub = spy(new RealtimeEventHub());
        SseEmitter emitter = mock(SseEmitter.class);
        doReturn(emitter).when(hub).createEmitter();
        doNothing()
                .doThrow(new IOException("connection closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        hub.subscribeTeacher(1001L);
        hub.publish(
                1001L,
                RealtimeEvent.create(
                        2001L,
                        RealtimeResource.TRAINING,
                        3001L,
                        "UPDATED"
                )
        );

        assertThat(hub.teacherConnectionCount(1001L)).isZero();
        verify(emitter, never()).complete();
    }
}