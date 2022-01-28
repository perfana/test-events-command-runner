/*
 * Copyright (C) 2020-2022 Peter Paul Bakker - Perfana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.perfana.events.commandrunner;

import io.perfana.eventscheduler.api.EventAdapter;
import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.message.EventMessage;
import io.perfana.eventscheduler.api.message.EventMessageBus;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class CommandRunnerEvent extends EventAdapter<CommandRunnerEventContext> {

    Future<ProcessResult> future;

    public CommandRunnerEvent(CommandRunnerEventContext eventContext, EventMessageBus messageBus, EventLogger logger) {
        super(eventContext, messageBus, logger);
        this.eventMessageBus.addReceiver(m -> logger.info("Received message: " + m));
    }

    @Override
    public void beforeTest() {

        String pluginName = CommandRunnerEvent.class.getSimpleName() + "-" + eventContext.getName();

        String command = eventContext.getCommand();

        logger.info("About to run [" + command + "] for [" + eventContext.getTestContext().getTestRunId() + "]");

        List<String> commandList = Arrays.stream(command.split("\\s+")).collect(Collectors.toList());
        try {
            future = new ProcessExecutor()
                .command(commandList)
                .start().getFuture();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run command: " + command, e);
        }

        EventMessage.EventMessageBuilder builder = EventMessage.builder();

        this.eventMessageBus.send(builder.pluginName(pluginName).build());

        this.eventMessageBus.send(EventMessage.builder().pluginName(pluginName).message("Go!").build());
    }

    @Override
    public void abortTest() {
        cancelCommand();
    }

    private void cancelCommand() {
        String command = eventContext.getCommand();

        logger.info("About to cancel [" + command + "] for [" + eventContext.getTestContext().getTestRunId() + "]");

        if (future != null) {
            boolean cancel = future.cancel(true);
            logger.info("Cancel [" + cancel + "] for [" + eventContext.getTestContext().getTestRunId() + "]");
        }
    }

    @Override
    public void afterTest() {
        cancelCommand();
    }

}
