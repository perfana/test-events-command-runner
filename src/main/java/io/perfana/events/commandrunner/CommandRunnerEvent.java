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
import io.perfana.eventscheduler.exception.EventSchedulerRuntimeException;
import io.perfana.eventscheduler.exception.handler.StopTestRunException;
import io.perfana.eventscheduler.util.TestRunConfigUtil;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandRunnerEvent extends EventAdapter<CommandRunnerEventContext> {

    public static final Pattern REGEX_SPLIT_QUOTES = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private Future<ProcessResult> future;

    public CommandRunnerEvent(CommandRunnerEventContext eventContext, EventMessageBus messageBus, EventLogger logger) {
        super(eventContext, messageBus, logger);
        this.eventMessageBus.addReceiver(m -> logger.debug("Received message: " + m));
    }

    @Override
    public void beforeTest() {

        String pluginName = CommandRunnerEvent.class.getSimpleName() + "-" + eventContext.getName();

        String command = eventContext.getCommand();

        logger.info("About to run [" + command + "] for [" + eventContext.getTestContext().getTestRunId() + "]");

        List<String> commandList = splitCommand(command);

        try {
            future = new ProcessExecutor()
                .command(commandList)
                .redirectOutput(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.out))
                .redirectError(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.err))
                .start().getFuture();
        } catch (IOException e) {
            throw new EventSchedulerRuntimeException("Failed to run command: " + command, e);
        }

        // disable sending of command
        if (eventContext.isSendTestRunConfig()) {
            String tags = "command-runner";
            EventMessage message = TestRunConfigUtil.createTestRunConfigMessageKeys(pluginName, createTestRunConfigLines(), tags);
            this.eventMessageBus.send(message);
        }

        this.eventMessageBus.send(EventMessage.builder().pluginName(pluginName).message("Go!").build());
    }

    private Map<String, String> createTestRunConfigLines() {
        String prefix = "event." + eventContext.getName() + ".";
        Map<String, String> lines = new HashMap<>();
        lines.put(prefix + "command", eventContext.getCommand());
        return lines;
    }

    private List<String> splitCommand(String command) {
        // https://stackoverflow.com/questions/3366281/tokenizing-a-string-but-ignoring-delimiters-within-quotes
        Matcher m = REGEX_SPLIT_QUOTES.matcher(command);
        List<String> commandList = new ArrayList<>();
        while (m.find()) {
            if (m.group(1) != null) {
                // Quoted
                commandList.add(m.group(1));
            } else {
                // Plain
                commandList.add(m.group(2));
            }
        }
        return commandList;
    }

    @Override
    public void abortTest() {
        cancelCommand();
        abortCommand();
    }

    private void abortCommand() {
        String abortCommand = eventContext.getAbortCommand();
        if (abortCommand != null) {
            logger.info("Abort command [" + abortCommand + "] for [" + eventContext.getTestContext().getTestRunId() + "]");

            List<String> commandList = splitCommand(abortCommand);

            try{
                new ProcessExecutor()
                        .command(commandList)
                        .redirectOutput(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.out))
                        .redirectError(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.err))
                        .start();
            } catch (IOException e) {
                throw new EventSchedulerRuntimeException("Failed to run command: " + abortCommand, e);
            }
        }
    }

    private void cancelCommand() {
        String command = eventContext.getCommand();

        if (future != null) {
            if (!future.isDone()) {
                logger.info("About to cancel [" + command + "] for [" + eventContext.getTestContext().getTestRunId() + "]");
                boolean cancel = future.cancel(true);
                logger.info("Cancel [" + cancel + "] for [" + eventContext.getTestContext().getTestRunId() + "]");
            }
            else {
                logger.info("No cancel needed for finished command for [" + eventContext.getTestContext().getTestRunId() + "]");
            }
        }

    }

    @Override
    public void afterTest() {
        cancelCommand();
    }

    @Override
    public void keepAlive() {
        String pollingCommand = eventContext.getPollingCommand();
        if (pollingCommand != null) {
            logger.info("Polling command [" + pollingCommand + "] for [" + eventContext.getTestContext().getTestRunId() + "]");

            List<String> commandList = splitCommand(pollingCommand);

            try{
                Future<ProcessResult> resultFuture = new ProcessExecutor()
                        .command(commandList)
                        .redirectOutput(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.out))
                        .redirectError(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.err))
                        .start()
                        .getFuture();

                ProcessResult processResult;
                try {
                    processResult = resultFuture.get(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.warn("Polling command got interrupted! " + e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    logger.warn("Polling command cannot be executed! " + e.getMessage());
                    return;
                } catch (TimeoutException e) {
                    logger.warn("Polling command got timeout! " + e.getMessage());
                    return;
                }

                int exitValue = processResult.getExitValue();
                if (exitValue != 0) {
                    String message = "Received non-0 value for polling command : " + exitValue;
                    logger.info(message);
                    if (eventContext.isContinueOnKeepAliveParticipant()) {
                        throw new StopTestRunException(message);
                    }
                }
                else {
                    logger.info("Received 0 value for polling command, all is ok");
                }

            } catch (IOException e) {
                throw new EventSchedulerRuntimeException("Failed to run command: " + pollingCommand, e);
            }
        }
    }
}
