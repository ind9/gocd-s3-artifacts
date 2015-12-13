package com.indix.gocd.utils;

import com.amazonaws.auth.*;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.indix.gocd.utils.Constants.AWS_ACCESS_KEY_ID;
import static com.indix.gocd.utils.Constants.AWS_SECRET_ACCESS_KEY;
import static com.indix.gocd.utils.Constants.AWS_USE_INSTANCE_PROFILE;

public class AWSCredentialsFactory {
    private static final List<String> validUseInstanceProfileValues = new ArrayList<String>(Arrays.asList("true","false","yes","no","1","0"));
    private static final List<String> affirmativeUseInstanceProfileValues = new ArrayList<String>(Arrays.asList("true","yes","1"));
    private GoEnvironment env;
    private Logger log = Logger.getLoggerFor(AWSCredentialsFactory.class);

    public AWSCredentialsFactory(GoEnvironment goEnvironment) {
        this.env = goEnvironment;
    }

    public AWSCredentialsProvider getCredentialsProvider() {
        List<AWSCredentialsProvider> providers = new ArrayList<AWSCredentialsProvider>();
        if (!env.isAbsent(AWS_USE_INSTANCE_PROFILE)) {
            String useInstanceProfileCode = env.get(AWS_USE_INSTANCE_PROFILE);
            if (affirmativeUseInstanceProfileValues.contains(useInstanceProfileCode.toLowerCase())) {
                log.debug(String.format(
                        "AWS_USE_INSTANCE_PROFILE=%s;Initializing with InstanceProfileCredentialsProvider",
                        useInstanceProfileCode));
                providers.add(new InstanceProfileCredentialsProvider());
            }
            else if (!validUseInstanceProfileValues.contains(useInstanceProfileCode.toLowerCase())) {
                throwEnvInvalidFormat(AWS_USE_INSTANCE_PROFILE, useInstanceProfileCode,
                        validUseInstanceProfileValues.toString());
            }
        }

        if (providers.size() == 0) {
            if (env.isAbsent(AWS_ACCESS_KEY_ID))
                throwEnvNotFoundIllegalArgumentException(AWS_ACCESS_KEY_ID);
            if (env.isAbsent(AWS_SECRET_ACCESS_KEY))
                throwEnvNotFoundIllegalArgumentException(AWS_SECRET_ACCESS_KEY);

            // See AccessKeyCredentialsProvider as to why use it instead of built-in EnvironmentVariablesCredentialsProvider
            log.debug("AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY are present;Initializing with AccessKeyCredentialsProvider");
            providers.add(new AccessKeyCredentialsProvider(
                    env.get(AWS_ACCESS_KEY_ID),
                    env.get(AWS_SECRET_ACCESS_KEY)
            ));
        }

        return makeProvidersChain(providers);
    }

    /*
    public just to enable testing
     */
    public AWSCredentialsProvider makeProvidersChain(List<AWSCredentialsProvider> providers) {
        return new AWSCredentialsProviderChain(providers.toArray(new AWSCredentialsProvider[providers.size()]));
    }

    private void throwEnvNotFoundIllegalArgumentException(String environmentVariable) {
        String message = String.format("%s environment variable not present", environmentVariable);
        throw new IllegalArgumentException(message);
    }

    private void throwEnvInvalidFormat(String environmentVariable, String value, String expected) {
        String message = String.format(
                "Unexpected value in %s environment variable; was %s, but expected one of the following %s",
                environmentVariable, value, expected);
        throw new IllegalArgumentException(message);
    }
}
