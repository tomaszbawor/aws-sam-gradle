package com.github.prazmok.aws.sam;

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin;
import com.github.prazmok.aws.sam.config.AwsSamExtension;
import com.github.prazmok.aws.sam.config.Config;
import com.github.prazmok.aws.sam.config.Environment;
import com.github.prazmok.aws.sam.task.GenerateTemplateTask;
import com.github.prazmok.aws.sam.task.PackageTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

import java.util.HashMap;
import java.util.Map;

public class AwsSamPlugin implements Plugin<Project> {
    public static final String SAM_DEPLOY_EXTENSION = "deployment";
    static final String SAM_DEPLOY_ENVIRONMENTS = "environments";
    static final String GENERATE_TEMPLATE_TASK_NAME = "generateSamTemplate";
    static final String SAM_PACKAGE_TASK_NAME = "packageSam";
    static final String SAM_DEPLOY_TASK_NAME = "deploySam";

    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;

        final NamedDomainObjectContainer<Environment> envs = project.container(Environment.class);
        final AwsSamExtension extension = project.getExtensions()
            .create(SAM_DEPLOY_EXTENSION, AwsSamExtension.class, envs);
        ((ExtensionAware) extension).getExtensions().add(SAM_DEPLOY_ENVIRONMENTS, envs);
        final String environment = project.hasProperty("environment")
            ? (String) project.getProperties().get("environment") : "test";
        Config config = new Config(project, extension, environment);
        generateTemplateTask(config);
        packageTask(config);
//        deployTask(config);
    }

    private void generateTemplateTask(Config config) {
        Object[] dependsOn = {"clean", "build", ShadowJavaPlugin.getSHADOW_JAR_TASK_NAME()};
        Object[] constructorArgs = {config};
        Map<String, Object> taskParams = new HashMap<String, Object>() {{
            put("type", GenerateTemplateTask.class);
            put("group", "AWS SAM");
            put("description", "Generate source SAM template with correct CodeUri JAR path");
            put("dependsOn", dependsOn);
            put("constructorArgs", constructorArgs);
        }};
        project.task(taskParams, GENERATE_TEMPLATE_TASK_NAME);
    }

    private void packageTask(Config config) {
        Object[] dependsOn = {GENERATE_TEMPLATE_TASK_NAME};
        Object[] constructorArgs = {config, project.getLogger()};
        Map<String, Object> taskParams = new HashMap<String, Object>() {{
            put("type", PackageTask.class);
            put("group", "AWS SAM");
            put("description", "Packages an AWS SAM application.");
            put("dependsOn", dependsOn);
            put("constructorArgs", constructorArgs);
        }};
        project.task(taskParams, SAM_PACKAGE_TASK_NAME);
    }

//    private void deployTask() {
//        Object[] dependsOn = {SAM_PACKAGE_TASK_NAME};
//        Object[] constructorArgs = {project};
//        Map<String, Object> taskParams = new HashMap<>() {{
//            put("type", DeployTask.class);
//            put("group", "AWS SAM");
//            put("description", "Deploys an AWS SAM application.");
//            put("dependsOn", dependsOn);
//            put("constructorArgs", constructorArgs);
//        }};
//
//        project.task(taskParams, SAM_DEPLOY_TASK_NAME);
//    }
}
