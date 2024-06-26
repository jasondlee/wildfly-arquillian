/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.arquillian.container.bootable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.arquillian.container.CommonManagedDeployableContainer;
import org.jboss.as.arquillian.container.ParameterUtils;
import org.jboss.logging.Logger;
import org.wildfly.core.launcher.BootableJarCommandBuilder;
import org.wildfly.core.launcher.CommandBuilder;

/**
 * The managed deployable container for a bootable JAR.
 *
 * @author jdenise@redhat.com
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @since 3.0.0
 */
public final class BootableDeployableContainer extends CommonManagedDeployableContainer<BootableContainerConfiguration> {

    private final Logger log = Logger.getLogger(BootableDeployableContainer.class.getName());

    @Override
    public Class<BootableContainerConfiguration> getConfigurationClass() {
        return BootableContainerConfiguration.class;
    }

    @Override
    @SuppressWarnings("FeatureEnvy")
    protected CommandBuilder createCommandBuilder(final BootableContainerConfiguration config) {
        final BootableJarCommandBuilder commandBuilder = BootableJarCommandBuilder.of(config.getJarFile());
        final String path = config.getInstallDir();
        if (path != null) {
            final Path installDir = Paths.get(path);
            // Workaround for https://issues.redhat.com/browse/WFCORE-5062
            if (Files.notExists(installDir)) {
                try {
                    Files.createDirectories(installDir);
                } catch (IOException e) {
                    throw new UncheckedIOException(String.format("Failed to create directory %s", installDir), e);
                }
            }
            commandBuilder.setInstallDir(installDir);
        }

        final String javaOpts = config.getJavaVmArguments();
        final String jbossArguments = config.getJbossArguments();

        commandBuilder.setJavaHome(config.getJavaHome());
        if (javaOpts != null && !javaOpts.trim().isEmpty()) {
            commandBuilder.setJavaOptions(ParameterUtils.splitParams(javaOpts));
        }

        if (config.isEnableAssertions()) {
            commandBuilder.addJavaOption("-ea");
        }

        if (jbossArguments != null && !jbossArguments.trim().isEmpty()) {
            commandBuilder.addServerArguments(ParameterUtils.splitParams(jbossArguments));
        }

        // Check if we should enable debug
        if (config.isDebug()) {
            commandBuilder.setDebug(config.isDebugSuspend(), config.getDebugPort());
        }

        return commandBuilder;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
