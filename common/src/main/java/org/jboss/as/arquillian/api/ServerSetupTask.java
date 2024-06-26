/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.arquillian.api;

import java.io.IOException;
import java.util.function.Function;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.wildfly.plugin.tools.OperationExecutionException;

/**
 *
 * A task which is run before deployment that allows the client to customize the server config.
 *
 * @author Stuart Douglas
 */
@SuppressWarnings("unused")
public interface ServerSetupTask {

    /**
     * Execute any necessary setup work that needs to happen before the first deployment
     * to the given container.
     * <p>
     * <strong>Note on exception handling:</strong> If an implementation of this method
     * throws any exception, the implementation can assume the following:
     * <ol>
     * <li>Any subsequent {@code ServerSetupTask}s {@link ServerSetup associated with test class}
     * <strong>will not</strong> be executed.</li>
     * <li>The deployment event that triggered the call to this method will be skipped.</li>
     * <li>The {@link #tearDown(ManagementClient, String) tearDown} method of the instance
     * that threw the exception <strong>will not</strong> be invoked. Therefore, implementations
     * that throw {@code AssumptionViolatedException}, or any other exception, should do so before altering any
     * system state.</li>
     * <li>The {@link #tearDown(ManagementClient, String) tearDown} method for any
     * previously executed {@code ServerSetupTask}s {@link ServerSetup associated with test class}
     * <strong>will</strong> be invoked.</li>
     * </ol>
     * <p>
     * If any other exception is thrown, the {@link #tearDown(ManagementClient, String)} will be executed, including
     * this implementations {@code tearDown()}, re-throwing the original exception. The original exception will have
     * any other exceptions thrown in the {@code tearDown()} methods add as
     * {@linkplain Throwable#addSuppressed(Throwable) suppressed} messages.
     * </p>
     *
     * @param managementClient management client to use to interact with the container
     * @param containerId      id of the container to which the deployment will be deployed
     * @throws Exception if a failure occurs
     */
    void setup(ManagementClient managementClient, String containerId) throws Exception;

    /**
     * Execute any tear down work that needs to happen after the last deployment associated
     * with the given container has been undeployed.
     *
     * @param managementClient management client to use to interact with the container
     * @param containerId      id of the container to which the deployment will be deployed
     * @throws Exception if a failure occurs
     */
    void tearDown(ManagementClient managementClient, String containerId) throws Exception;

    /**
     * Executes an operation failing with a {@code RuntimeException} if the operation was not successful.
     *
     * @param client the client used to communicate with the server
     * @param op     the operation to execute
     *
     * @return the result from the operation
     *
     * @throws OperationExecutionException if the operation failed
     * @throws IOException                 if an error occurs communicating with the server
     */
    default ModelNode executeOperation(final ManagementClient client, final ModelNode op) throws IOException {
        return executeOperation(client, op, (result) -> String.format("Failed to execute operation '%s': %s", op
                .asString(),
                Operations.getFailureDescription(result).asString()));
    }

    /**
     * Executes an operation failing with a {@code RuntimeException} if the operation was not successful.
     *
     * @param client       the client used to communicate with the server
     * @param op           the operation to execute
     * @param errorMessage a function which accepts the result as the argument and returns an error message for an
     *                         unsuccessful operation
     *
     * @return the result from the operation
     *
     * @throws OperationExecutionException if the operation failed
     * @throws IOException                 if an error occurs communicating with the server
     */
    default ModelNode executeOperation(final ManagementClient client, final ModelNode op,
            final Function<ModelNode, String> errorMessage) throws IOException {
        return executeOperation(client, Operation.Factory.create(op), errorMessage);
    }

    /**
     * Executes an operation failing with a {@code RuntimeException} if the operation was not successful.
     *
     * @param client the client used to communicate with the server
     * @param op     the operation to execute
     *
     * @return the result from the operation
     *
     * @throws OperationExecutionException if the operation failed
     * @throws IOException                 if an error occurs communicating with the server
     */
    default ModelNode executeOperation(final ManagementClient client, final Operation op) throws IOException {
        return executeOperation(client, op, (result) -> String.format("Failed to execute operation '%s': %s", op.getOperation()
                .asString(),
                Operations.getFailureDescription(result).asString()));
    }

    /**
     * Executes an operation failing with a {@code RuntimeException} if the operation was not successful.
     *
     * @param client       the client used to communicate with the server
     * @param op           the operation to execute
     * @param errorMessage a function which accepts the result as the argument and returns an error message for an
     *                         unsuccessful operation
     *
     * @return the result from the operation
     *
     * @throws OperationExecutionException if the operation failed
     * @throws IOException                 if an error occurs communicating with the server
     */
    default ModelNode executeOperation(final ManagementClient client, final Operation op,
            final Function<ModelNode, String> errorMessage) throws IOException {
        final ModelNode result = client.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new OperationExecutionException(op, result);
        }
        return Operations.readResult(result);
    }
}
