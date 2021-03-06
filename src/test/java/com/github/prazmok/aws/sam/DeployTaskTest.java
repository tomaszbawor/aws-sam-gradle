package com.github.prazmok.aws.sam;

import com.github.prazmok.aws.sam.config.AwsSamExtension;
import com.github.prazmok.aws.sam.config.Config;
import com.github.prazmok.aws.sam.config.Environment;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DeployTaskTest {
    private Project project;
    private NamedDomainObjectContainer<Environment> envs;
    private File samTemplate = new File("/tmp/packaged.yml");

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws IOException {
        if (!samTemplate.exists() || !samTemplate.isFile()) {
            assertTrue(samTemplate.createNewFile(), "Assert create temporary SAM template file");
        }
        project = ProjectBuilder.builder()
            .withProjectDir(samTemplate.getParentFile())
            .build();
        envs = Mockito.mock(NamedDomainObjectContainer.class);
        when(envs.getByName("default")).thenReturn(new Environment("default"));
    }

    @AfterEach
    void tearDown() {
        samTemplate.deleteOnExit();
    }

    @Test
    void testNotExistingTemplateFile() {
        AwsSamExtension extension = new AwsSamExtension(envs);
        extension.samTemplate = new File("/non/existing/sam/packaged.yml");
        Config config = new Config(project, extension);
        DeployTask task = (DeployTask) buildTask(config);
        assertThrows(FileNotFoundException.class, task::samPackagedTemplate);
        extension.samTemplate = new File("/wrong/path/to/directory");
        assertThrows(FileNotFoundException.class, task::samPackagedTemplate);
    }

    @Test
    void testBuildCommand() {
        Config config = new Config(project, getExtension());
        DeployTask task = (DeployTask) buildTask(config);
        String expected = "sam deploy --force-upload --use-json --no-execute-changeset --fail-on-empty-changeset --debug" +
            " --template-file /tmp/packaged.yml --stack-name example-cloud-formation-stack --s3-bucket " +
            "example-s3-bucket --s3-prefix example-s3-prefix --profile default --region eu-west-1 --kms-key-id " +
            "example-kms-key-id --capabilities CAPABILITY_IAM,CAPABILITY_NAMED_IAM --notification-arns " +
            "example-notification-arn1,example-notification-arn2 --tags example-tag1,example-tag2,example-tag3 " +
            "--parameter-overrides SomeParam1=ParamValue1 SomeParam2=ParamValue2";
        assertEquals(expected, String.join(" ", task.buildCommand()));
    }

    @Test
    void testReturnCodeOnError() {
        AwsSamExtension extension = new AwsSamExtension(envs);
        Config config = new Config(project, extension);
        DeployTask task = (DeployTask) buildTask(config);
        assertEquals("return 1", String.join(" ", task.buildCommand()));
    }

    @Test
    void testDryRunExecution() {
        Config config = new Config(project, getExtension());
        Config configMock = Mockito.spy(config);
        when(configMock.isDryRunOption()).thenReturn(true);
        DeployTask task = (DeployTask) buildTask(configMock);
        assertEquals("echo", String.join(" ", task.buildCommand()));
    }

    private Task buildTask(Config config) {
        Object[] constructorArgs = {config};
        Map<String, Object> taskParams = new HashMap<String, Object>() {{
            put("type", DeployTask.class);
            put("constructorArgs", constructorArgs);
        }};
        return project.task(taskParams, AwsSamPlugin.DEPLOY_TASK + "Test");
    }

    private AwsSamExtension getExtension() {
        AwsSamExtension extension = new AwsSamExtension(envs);
        extension.samTemplate = samTemplate;
        extension.awsRegion = "eu-west-1";
        extension.awsProfile = "default";
        extension.kmsKeyId = "example-kms-key-id";
        extension.s3Bucket = "example-s3-bucket";
        extension.s3Prefix = "example-s3-prefix";
        extension.stackName = "example-cloud-formation-stack";
        extension.roleArn = "example-cf-role-arn-assumed-when-executing-the-change-set";

        extension.forceUpload = true;
        extension.useJson = true;
        extension.noExecuteChangeset = true;
        extension.failOnEmptyChangeset = true;
        extension.noFailOnEmptyChangeset = true;
        extension.debug = true;

        extension.capabilities = new LinkedList<>();
        extension.capabilities.add("CAPABILITY_IAM");
        extension.capabilities.add("CAPABILITY_NAMED_IAM");

        extension.tags = new LinkedList<>();
        extension.tags.add("example-tag1");
        extension.tags.add("example-tag2");
        extension.tags.add("example-tag3");

        extension.notificationArns = new LinkedList<>();
        extension.notificationArns.add("example-notification-arn1");
        extension.notificationArns.add("example-notification-arn2");

        extension.parameterOverrides = new LinkedHashMap<>();
        extension.parameterOverrides.put("SomeParam1", "ParamValue1");
        extension.parameterOverrides.put("SomeParam2", "ParamValue2");

        return extension;
    }
}
