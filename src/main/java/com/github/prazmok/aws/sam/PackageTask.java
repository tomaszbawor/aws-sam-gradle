package com.github.prazmok.aws.sam;

import com.github.prazmok.aws.sam.command.SamCommandBuilder;
import com.github.prazmok.aws.sam.config.Config;
import com.github.prazmok.aws.sam.config.exception.MissingConfigurationException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

public class PackageTask extends SamCliTask {
    private final Config config;
    private final SamCommandBuilder samCommandBuilder;

    @Inject
    public PackageTask(Config config) {
        this.config = config;
        this.samCommandBuilder = new SamCommandBuilder(logger, config.isDryRunOption());
    }

    @TaskAction
    protected void packageSam() throws Exception {
        ExecResult result = getProject().exec((action) -> {
            action.commandLine(buildCommand());
        });

        if (result.getExitValue() == 0) {
            if (!config.isDryRunOption() && !config.getPackagedTemplate().exists()) {
                throw new Exception("Couldn't generate output SAM template!");
            }

            logger.lifecycle("Successfully created output SAM template: " + config.getPackagedTemplate().getPath());
        }

        result.rethrowFailure();
    }

    public Set<String> buildCommand() {
        try {
            samCommandBuilder.task("package")
                .option("--force-upload", config.forceUpload())
                .option("--use-json", config.useJson())
                .option("--debug", config.debug())
                .argument("--template-file", samTemplatePath())
                .argument("--output-template-file", samPackagedTemplate())
                .argument("--s3-bucket", config.getS3Bucket())
                .argument("--s3-prefix", config.getS3Prefix())
                .argument("--profile", config.getAwsProfile())
                .argument("--region", config.getAwsRegion())
                .argument("--kms-key-id", config.getKmsKeyId());

            return samCommandBuilder.build();
        } catch (MissingConfigurationException | FileNotFoundException e) {
            logger.error(e.toString());
        }

        return returnCodeCommand(1);
    }

    String samTemplatePath() throws FileNotFoundException {
        File template = config.getSamTemplate();

        if (!template.exists() || !template.isFile()) {
            throw new FileNotFoundException("AWS SAM template couldn't been found in "
                + template.getAbsolutePath() + " location!");
        }

        return template.getAbsolutePath();
    }

    String samPackagedTemplate() throws FileNotFoundException {
        File packaged = config.getPackagedTemplate();

        if (!packaged.getParentFile().exists() || !packaged.getParentFile().isDirectory()) {
            throw new FileNotFoundException("Provided packaged template directory ("
                + packaged.getParentFile().getAbsolutePath() + ") is invalid!");
        }

        return packaged.getAbsolutePath();
    }
}
