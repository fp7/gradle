/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.internal.provider;

import org.gradle.api.internal.tasks.userinput.UserInputReader;
import org.gradle.initialization.BuildRequestContext;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.dispatch.Dispatch;
import org.gradle.internal.invocation.BuildAction;
import org.gradle.internal.logging.console.GlobalUserInputReceiver;
import org.gradle.launcher.daemon.client.DaemonClientInputForwarder;
import org.gradle.launcher.daemon.protocol.CloseInput;
import org.gradle.launcher.daemon.protocol.ForwardInput;
import org.gradle.launcher.daemon.protocol.InputMessage;
import org.gradle.launcher.daemon.protocol.UserResponse;
import org.gradle.launcher.daemon.server.clientinput.ClientInputForwarder;
import org.gradle.launcher.exec.BuildActionExecuter;
import org.gradle.launcher.exec.BuildActionParameters;
import org.gradle.launcher.exec.BuildActionResult;

import java.io.InputStream;

/**
 * Used in tooling API embedded mode to forward client provided user input to this process's System.in and other relevant services.
 * Reuses the services used by the daemon client and daemon server to forward user input.
 */
class ForwardStdInToThisProcess implements BuildActionExecuter<BuildActionParameters, BuildRequestContext> {
    private final GlobalUserInputReceiver userInputReceiver;
    private final UserInputReader userInputReader;
    private final InputStream finalStandardInput;
    private final BuildActionExecuter<BuildActionParameters, BuildRequestContext> delegate;
    private final ExecutorFactory executorFactory;

    public ForwardStdInToThisProcess(
        GlobalUserInputReceiver userInputReceiver,
        UserInputReader userInputReader,
        InputStream finalStandardInput,
        BuildActionExecuter<BuildActionParameters, BuildRequestContext> delegate,
        ExecutorFactory executorFactory
    ) {
        this.userInputReceiver = userInputReceiver;
        this.userInputReader = userInputReader;
        this.finalStandardInput = finalStandardInput;
        this.delegate = delegate;
        this.executorFactory = executorFactory;
    }

    @Override
    public BuildActionResult execute(BuildAction action, BuildActionParameters actionParameters, BuildRequestContext buildRequestContext) {
        ClientInputForwarder forwarder = new ClientInputForwarder(userInputReader);
        return forwarder.forwardInput(stdinHandler -> {
            DaemonClientInputForwarder inputForwarder = new DaemonClientInputForwarder(finalStandardInput, new Dispatch<InputMessage>() {
                @Override
                public void dispatch(InputMessage message) {
                    if (message instanceof UserResponse) {
                        stdinHandler.onUserResponse((UserResponse) message);
                    } else if (message instanceof ForwardInput) {
                        stdinHandler.onInput((ForwardInput) message);
                    } else if (message instanceof CloseInput) {
                        stdinHandler.onEndOfInput();
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
            }, userInputReceiver, executorFactory);
            inputForwarder.start();
            try {
                return delegate.execute(action, actionParameters, buildRequestContext);
            } finally {
                inputForwarder.stop();
                stdinHandler.onEndOfInput();
            }
        });
    }
}
