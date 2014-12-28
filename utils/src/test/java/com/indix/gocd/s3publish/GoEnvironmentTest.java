package com.indix.gocd.utils;

import org.junit.Test;
import static org.junit.Assert.assertThat;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

import com.indix.gocd.utils.utils.Maps;

public class GoEnvironmentTest {
    Map<String, String> mockEnvironment = Maps.<String, String>builder()
            .with("GO_SERVER_URL", "https://localhost:8154/go")
            .with("GO_PIPELINE_NAME", "s3-publish-test")
            .with("GO_PIPELINE_COUNTER", "20")
            .with("GO_STAGE_NAME", "build-and-publish")
            .with("GO_STAGE_COUNTER", "1")
            .with("GO_JOB_NAME", "publish")
            .with("GO_TRIGGER_USER", "Krishna")
            .build();
    GoEnvironment goEnvironment = new GoEnvironment().putAll(mockEnvironment);

    @Test
    public void shouldGenerateTracebackUrl() {
        assertThat(goEnvironment.traceBackUrl(), is("https://localhost:8154/go/tab/build/detail/s3-publish-test/20/build-and-publish/1/publish"));
    }

    @Test
    public void shouldReturnTriggeredUser() {
        assertThat(goEnvironment.triggeredUser(), is("Krishna"));
    }

    @Test
    public void shouldGenerateArtifactLocationTemplate() {
        assertThat(goEnvironment.artifactsLocationTemplate(), is("s3-publish-test/build-and-publish/publish/20.1"));
    }

}
