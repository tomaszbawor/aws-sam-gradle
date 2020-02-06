package com.github.prazmok.aws.sam;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;

import static java.util.Arrays.asList;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsSamPluginFunTest {
    private final static File projectDir = new File("example");

    @Test
    public void testDeploySamTask() {
        BuildResult result = GradleRunner.create().withProjectDir(projectDir.getAbsoluteFile())
            .withPluginClasspath()
            .withArguments(asList("deploySam", "-Penvironment=test", "-Pdry-run", "--stacktrace"))
            .forwardOutput()
            .build();

        assertEquals(SUCCESS, result.task(":build").getOutcome());
        assertEquals(SUCCESS, result.task(":shadowJar").getOutcome());
        assertEquals(SUCCESS, result.task(":validateSam").getOutcome());
        assertEquals(SUCCESS, result.task(":packageSam").getOutcome());
        assertEquals(SUCCESS, result.task(":deploySam").getOutcome());

        String rootDir = projectDir.getAbsolutePath();

        assertTrue(result.getOutput().contains("> Task :validateSam"));
        assertTrue(result.getOutput().contains("Dry run execution of command:"));
        assertTrue(result.getOutput().contains("sam validate --debug --template-file " + rootDir + "/template.yml --profile default --region eu-west-1"));
        assertTrue(result.getOutput().contains("AWS SAM template " + rootDir + "/template.yml is valid"));

        assertTrue(result.getOutput().contains("> Task :packageSam"));
        assertTrue(result.getOutput().contains("Dry run execution of command:"));
        assertTrue(result.getOutput().contains("sam package --debug --template-file " + rootDir + "/template.yml --output-template-file " + rootDir + "/packaged.yml --s3-bucket example-s3-bucket --s3-prefix example-s3-prefix --profile default --region eu-west-1 --kms-key-id example-kms-key-id"));
        assertTrue(result.getOutput().contains("Successfully created output SAM template: " + rootDir + "/packaged.yml"));

        assertTrue(result.getOutput().contains("> Task :deploySam"));
        assertTrue(result.getOutput().contains("Dry run execution of command:"));
        assertTrue(result.getOutput().contains("sam deploy --fail-on-empty-changeset --debug --template-file " + rootDir + "/packaged.yml --stack-name example-cloud-formation-stack --s3-bucket example-s3-bucket --s3-prefix example-s3-prefix --profile default --region eu-west-1 --kms-key-id example-kms-key-id --capabilities CAPABILITY_IAM,CAPABILITY_NAMED_IAM --notification-arns example-notification-arn1,example-notification-arn2 --tags example-tag1,example-tag2 --parameter-overrides ExampleParameter=ExtendedExampleValue"));
        assertTrue(result.getOutput().contains("Successfully finished AWS SAM deployment!"));

        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }
}
