package com.indix.gocd.s3fetch;

import com.amazonaws.util.StringUtils;
import com.indix.gocd.utils.GoEnvironment;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.indix.gocd.utils.Constants.*;

public class FetchConfig {
    private final String materialLabel;
    private final String pipeline;
    private final String stage;
    private final String job;
    private GoEnvironment env;

    public FetchConfig(TaskConfig config, TaskExecutionContext context)
    {
        this(config, context, new GoEnvironment());
    }

    public FetchConfig(TaskConfig config, TaskExecutionContext context, GoEnvironment goEnvironment) {
        this.env = goEnvironment;
        env.putAll(context.environment().asMap());

        String repoName = config.getValue(FetchTask.REPO).toUpperCase().replaceAll("-", "_");
        String packageName = config.getValue(FetchTask.PACKAGE).toUpperCase().replaceAll("-", "_");
        this.materialLabel = env.get(String.format("GO_PACKAGE_%s_%s_LABEL", repoName, packageName));
        this.pipeline = env.get(String.format("GO_PACKAGE_%s_%s_PIPELINE_NAME", repoName, packageName));
        this.stage = env.get(String.format("GO_PACKAGE_%s_%s_STAGE_NAME", repoName, packageName));
        this.job = env.get(String.format("GO_PACKAGE_%s_%s_JOB_NAME", repoName, packageName));
    }

    public ValidationResult validate() {
        ValidationResult validationResult = new ValidationResult();
        if (!hasAWSUseIamRole()) {
            if (env.isAbsent(AWS_ACCESS_KEY_ID)) validationResult.addError(envNotFound(AWS_ACCESS_KEY_ID));
            if (env.isAbsent(AWS_SECRET_ACCESS_KEY)) validationResult.addError(envNotFound(AWS_SECRET_ACCESS_KEY));
        }
        if (env.isAbsent(GO_ARTIFACTS_S3_BUCKET)) validationResult.addError(envNotFound(GO_ARTIFACTS_S3_BUCKET));
        if (StringUtils.isNullOrEmpty(materialLabel))
            validationResult.addError(new ValidationError("Please check Repository name or Package name configuration. Also ensure that the appropriate S3 material is configured for the pipeline."));

        return validationResult;
    }

    public String getArtifactsLocationTemplate() {
        String[] counters = materialLabel.split("\\.");
        String pipelineCounter = counters[0];
        String stageCounter = counters[1];
        return env.artifactsLocationTemplate(pipeline, stage, job, pipelineCounter, stageCounter);
    }

    private static final List<String> validUseIamRoleValues = new ArrayList<String>(Arrays.asList("true", "false", "yes", "no", "on", "off"));
    public boolean hasAWSUseIamRole() {
        if (!env.has(AWS_USE_IAM_ROLE)) {
            return false;
        }

        String useIamRoleValue = env.get(AWS_USE_IAM_ROLE);
        Boolean result = BooleanUtils.toBooleanObject(useIamRoleValue);
        if (result == null) {
            throw new IllegalArgumentException(getEnvInvalidFormatMessage(AWS_USE_IAM_ROLE,
                    useIamRoleValue, validUseIamRoleValues.toString()));
        }
        else {
            return result.booleanValue();
        }
    }

    public String getAWSAccessKeyId() {
        return env.get(AWS_ACCESS_KEY_ID);
    }

    public String getAWSSecretAccessKey() {
        return env.get(AWS_SECRET_ACCESS_KEY);
    }

    public String getS3Bucket() {
        return env.get(GO_ARTIFACTS_S3_BUCKET);
    }

    private ValidationError envNotFound(String environmentVariable) {
        return new ValidationError(environmentVariable, String.format("%s environment variable not present", environmentVariable));
    }

    private String getEnvInvalidFormatMessage(String environmentVariable, String value, String expected){
        return String.format(
                "Unexpected value in %s environment variable; was %s, but expected one of the following %s",
                environmentVariable, value, expected);
    }
}
