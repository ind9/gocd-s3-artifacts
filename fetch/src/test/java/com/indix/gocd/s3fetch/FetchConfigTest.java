package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.mocks.MockTaskExecutionContext;
import com.indix.gocd.utils.utils.Maps;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionConfiguration;
import io.jmnarloch.cd.go.plugin.api.executor.ExecutionContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.indix.gocd.utils.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FetchConfigTest {
    private final String bucket = "gocd";
    Maps.MapBuilder<String, String> mockEnvironmentVariables;
    private ExecutionConfiguration config;
    private FetchConfig fetchConfig;
    private GoEnvironment goEnvironmentForTest;
    private final String secretKey = "secretKey";
    private final String accessId = "accessId";

    @Before
    public void setUp() throws Exception {
        config = mock(ExecutionConfiguration.class);
        when(config.getProperty(FetchConfigEnum.REPO.name())).thenReturn(bucket);
        when(config.getProperty(FetchConfigEnum.PACKAGE.name())).thenReturn("TestPublishS3Artifacts");
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, secretKey)
                .with(AWS_ACCESS_KEY_ID, accessId)
                .with(GO_ARTIFACTS_S3_BUCKET, bucket)
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_LABEL", "20.1")
                .with("GO_REPO_GOCD_TESTPUBLISHS3ARTIFACTS_S3_BUCKET", bucket)
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_PIPELINE_NAME", "TestPublish")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_STAGE_NAME", "defaultStage")
                .with("GO_PACKAGE_GOCD_TESTPUBLISHS3ARTIFACTS_JOB_NAME", "defaultJob");
        goEnvironmentForTest = new GoEnvironment(new HashMap<String,String>());

    }

    @Test
    public void shouldGetAWSSecretAccessKey() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        String awsSecretAccessKey = fetchConfig.getAWSSecretAccessKey();
        assertThat(awsSecretAccessKey, is(secretKey));
    }

    @Test
    public void shouldGetAWSAccessKeyId() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        String awsSecretAccessKey = fetchConfig.getAWSAccessKeyId();
        assertThat(awsSecretAccessKey, is(accessId));
    }

    @Test
    public void shouldGetHasAWSUseIamRoleFalseIfNotSet() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        Boolean result = fetchConfig.hasAWSUseIamRole();
        assertThat(result, is(Boolean.FALSE));
    }

    @Test
    public void shouldGetHasAWSUseIamRoleTrueIfSetToTrue() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"True")
                .build()),goEnvironmentForTest);

        Boolean result = fetchConfig.hasAWSUseIamRole();

        assertThat(result, is(Boolean.TRUE));
    }

    @Test
    public void shouldGetHasAWSUseIamRoleFalseIfSetToFalse() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"False")
                .build()),goEnvironmentForTest);

        Boolean result = fetchConfig.hasAWSUseIamRole();

        assertThat(result, is(Boolean.FALSE));
    }

    @Test
    public void shouldS3Bucket() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        String awsSecretAccessKey = fetchConfig.getS3Bucket();
        assertThat(awsSecretAccessKey, is(bucket));
    }

    @Test
    public void shouldGetArtifactLocation() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        String location = fetchConfig.getArtifactsLocationTemplate();
        assertThat(location, is("TestPublish/defaultStage/defaultJob/20.1"));
    }

    @Test
    public void shouldBeValid() {
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldNotBeValidIfAWSSecretAccessKeyNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables.with(AWS_SECRET_ACCESS_KEY, "").build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("AWS_SECRET_ACCESS_KEY environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfAWSAccessKeyIdNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables.with(AWS_ACCESS_KEY_ID, "").build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("AWS_ACCESS_KEY_ID environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldBeValidIfUsingIamRoleAndAWSKeysArePresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"True")
                .build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldBeValidIfUsingIamRoleAndAWSKeysNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"True")
                .with(AWS_ACCESS_KEY_ID, "")
                .with(AWS_SECRET_ACCESS_KEY, "")
                .build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldNotBeValidIfNotUsingIamRoleAndAWSSecretAccessKeyNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables
                .with(AWS_USE_IAM_ROLE,"False")
                .with(AWS_SECRET_ACCESS_KEY, "")
                .build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("AWS_SECRET_ACCESS_KEY environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfNotUsingIamRoleAndAWSAccessKeyIdNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables
                .with(AWS_ACCESS_KEY_ID, "")
                .with(AWS_USE_IAM_ROLE,"False")
                .build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("AWS_ACCESS_KEY_ID environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfS3BucketNotPresent() {
        fetchConfig = new FetchConfig(config, mockContext( mockEnvironmentVariables.with(GO_ARTIFACTS_S3_BUCKET, "").build()),
                goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("GO_ARTIFACTS_S3_BUCKET environment variable not present");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfRepoConfigIsNotValid() {
        when(config.getProperty(FetchConfigEnum.REPO.name())).thenReturn("Wrong");
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline.");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldNotBeValidIfPackageConfigIsNotValid() {
        when(config.getProperty(FetchConfigEnum.PACKAGE.name())).thenReturn("Wrong");
        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertFalse(validationResult.isSuccessful());
        ArrayList<String> messages = new ArrayList<String>();
        messages.add("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline.");
        assertThat(validationResult.getMessages(), Matchers.<List<String>>is(messages));
    }

    @Test
    public void shouldAllowFetchTaskVariablesWithDashesInTheName() throws Exception {
        config = mock(ExecutionConfiguration.class);
        when(config.getProperty(FetchConfigEnum.REPO.name())).thenReturn("repo-with-dash");
        when(config.getProperty(FetchConfigEnum.PACKAGE.name())).thenReturn("package-with-dash");
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, secretKey)
                .with(AWS_ACCESS_KEY_ID, accessId)
                .with(GO_ARTIFACTS_S3_BUCKET, bucket)
                .with("GO_PACKAGE_REPO_WITH_DASH_PACKAGE_WITH_DASH_LABEL", "20.1")
                .with("GO_REPO_REPO_WITH_DASH_PACKAGE_WITH_DASH_S3_BUCKET", bucket)
                .with("GO_PACKAGE_REPO_WITH_DASH_PACKAGE_WITH_DASH_PIPELINE_NAME", "TestPublish")
                .with("GO_PACKAGE_REPO_WITH_DASH_PACKAGE_WITH_DASH_STAGE_NAME", "defaultStage")
                .with("GO_PACKAGE_REPO_WITH_DASH_PACKAGE_WITH_DASH_JOB_NAME", "defaultJob");

        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()),goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldAllowFetchTaskVariablesWithPeriodsInTheName() throws Exception {
        config = mock(ExecutionConfiguration.class);
        when(config.getProperty(FetchConfigEnum.REPO.name())).thenReturn("repo-with.period");
        when(config.getProperty(FetchConfigEnum.PACKAGE.name())).thenReturn("package-with.period");
        mockEnvironmentVariables = Maps.<String, String>builder()
            .with(AWS_SECRET_ACCESS_KEY, secretKey)
            .with(AWS_ACCESS_KEY_ID, accessId)
            .with(GO_ARTIFACTS_S3_BUCKET, bucket)
            .with("GO_PACKAGE_REPO_WITH_PERIOD_PACKAGE_WITH_PERIOD_LABEL", "20.1")
            .with("GO_REPO_REPO_WITH_PERIOD_PACKAGE_WITH_PERIOD_S3_BUCKET", bucket)
            .with("GO_PACKAGE_REPO_WITH_PERIOD_PACKAGE_WITH_PERIOD_PIPELINE_NAME", "TestPublish")
            .with("GO_PACKAGE_REPO_WITH_PERIOD_PACKAGE_WITH_PERIOD_STAGE_NAME", "defaultStage")
            .with("GO_PACKAGE_REPO_WITH_PERIOD_PACKAGE_WITH_PERIOD_JOB_NAME", "defaultJob");

        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()), goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }


    @Test
    public void shouldAllowFetchTaskVariablesWithSpecialCharactersInTheName() throws Exception {
        config = mock(ExecutionConfiguration.class);
        when(config.getProperty(FetchConfigEnum.REPO.name())).thenReturn("repo-with`~!@#$%^&*()-+=[{]}\\|;:'\",<.>/?");
        when(config.getProperty(FetchConfigEnum.PACKAGE.name())).thenReturn("package-with`~!@#$%^&*()-+=[{]}\\|;:'\",<.>/?");
        mockEnvironmentVariables = Maps.<String, String>builder()
                .with(AWS_SECRET_ACCESS_KEY, secretKey)
                .with(AWS_ACCESS_KEY_ID, accessId)
                .with(GO_ARTIFACTS_S3_BUCKET, bucket)
                .with("GO_PACKAGE_REPO_WITH________________________________PACKAGE_WITH________________________________LABEL", "20.1")
                .with("GO_REPO_REPO_WITH________________________________PACKAGE_WITH________________________________S3_BUCKET", bucket)
                .with("GO_PACKAGE_REPO_WITH________________________________PACKAGE_WITH________________________________PIPELINE_NAME", "TestPublish")
                .with("GO_PACKAGE_REPO_WITH________________________________PACKAGE_WITH________________________________STAGE_NAME", "defaultStage")
                .with("GO_PACKAGE_REPO_WITH________________________________PACKAGE_WITH________________________________JOB_NAME", "defaultJob");

        fetchConfig = new FetchConfig(config, mockContext(mockEnvironmentVariables.build()), goEnvironmentForTest);
        ValidationResult validationResult = fetchConfig.validate();
        assertTrue(validationResult.isSuccessful());
    }

    private ExecutionContext mockContext(final Map<String, String> environmentMap) {
        return new MockTaskExecutionContext(environmentMap);
    }
}
