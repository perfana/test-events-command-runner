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

        // default sending of command is disabled: might contain secrets
        if (eventContext.isSendTestRunConfig()) {
            String tags = "command-runner";
            EventMessage message = TestRunConfigUtil.createTestRunConfigMessageKeys(pluginName, createTestRunConfigLines(), tags);
            this.eventMessageBus.send(message);
        }

        String command = eventContext.getOnBeforeTest();

        Future<ProcessResult> beforeTestCommandFuture = runCommand(command, "beforeTest");

        if (beforeTestCommandFuture == null) {
            logger.debug("No result available for beforeTest command");
        }
        else {
            waitForCommandToFinishOrTimeout(beforeTestCommandFuture);
        }

        if (eventContext.isReadyForStartParticipant()) {
            this.eventMessageBus.send(EventMessage.builder().pluginName(pluginName).message("Go!").build());
        }

    }

    private void waitForCommandToFinishOrTimeout(Future<ProcessResult> command) {
        try {
            ProcessResult processResult = command.get(120, TimeUnit.SECONDS);

            if (processResult.getExitValue() != 0) {
                logger.warn("Command did not end successfully. Exit code: " + processResult.getExitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Command got interrupted");
        } catch (ExecutionException e) {
            throw new EventSchedulerRuntimeException("Command failed.", e);
        } catch (TimeoutException e) {
            command.cancel(true);
            throw new EventSchedulerRuntimeException("Command timed out, cancelled command.", e);
        }
    }

    @Override
    public void startTest() {
        String command = eventContext.getOnTestStart();
        future = runCommand(command, "startTest");
    }

    @Override
    public void keepAlive() {

        if (isContinueOnKeepAliveParticipant() && future != null && future.isDone()) {
            int exitCode = getExitCode(future);
            String message = "The command is done (exit code: " + exitCode + ") and this is a continueOnKeepAlive participant: will request to stop test run.";
            logger.info(message);
            throw new StopTestRunException(message);
        }

        String pollingCommand = eventContext.getOnKeepAlive();

        Future<ProcessResult> processResultFuture = runCommand(pollingCommand, "keepAlive");

        if (processResultFuture == null) {
            return;
        }

        ProcessResult processResult;
        try {
            processResult = processResultFuture.get(30, TimeUnit.SECONDS);
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
            String message = "Received failed (non-zero) exit value for polling command (" + exitValue + ")." +
                    " Request to stop test run.";
            logger.info(message);
            if (isContinueOnKeepAliveParticipant()) {
                throw new StopTestRunException(message);
            }
        }
        else {
            logger.info("Received success (zero) exit value for polling command, keep on running!");
        }
    }

    @Override
    public void abortTest() {
        cancelCommand();
        abortCommand();
    }

    @Override
    public void afterTest() {
        cancelCommand();
        runCommand(eventContext.getOnAfterTest(), "afterTest");
    }

    private Future<ProcessResult> runCommand(String command, String commandType) {
        if (command.isEmpty()) {
            logger.debug("No command to run for " + commandType);
            return null;
        }
        logger.info("About to run " + commandType + " [" + command + "]");

        String wrappedCommand;
        if (command.startsWith("sh -c")) {
            wrappedCommand = command;
        }
        else {
            wrappedCommand = "sh -c \"" + escapeDoubleQuotes(command) + "\"";
        }

        List<String> commandList = splitCommand(wrappedCommand);

        Future<ProcessResult> myProcessResult;
        try {
            myProcessResult = new ProcessExecutor()
                .command(commandList)
                .redirectOutput(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.out))
                .redirectError(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.err))
                .start().getFuture();
        } catch (IOException e) {
            throw new EventSchedulerRuntimeException("Failed to run command: " + command, e);
        }
        return myProcessResult;
    }

    private String escapeDoubleQuotes(String command) {
        return command.replace("\"", "\\\"");
    }

    private Map<String, String> createTestRunConfigLines() {
        String prefix = "event." + eventContext.getName() + ".";
        Map<String, String> lines = new HashMap<>();
        lines.put(prefix + "command", eventContext.getOnTestStart());
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

    private void abortCommand() {
        String abortCommand = eventContext.getOnAbort();
        runCommand(abortCommand, "abortCommand");
    }

    private void cancelCommand() {
        String command = eventContext.getOnTestStart();
        if (future != null) {
            logger.debug("There is a future for [ " + command + "]");
            if (!future.isDone()) {
                logger.info("About to cancel [" + command + "] for [" + eventContext.getTestContext().getTestRunId() + "]");
                boolean cancel = future.cancel(true);
                logger.debug("Cancel [" + cancel + "] for [" + eventContext.getTestContext().getTestRunId() + "]");
            }
            else {
                logger.info("No cancel needed for finished command for [" + eventContext.getTestContext().getTestRunId() + "]");
            }
        }
    }

    private static int getExitCode(Future<ProcessResult> future) {
        ProcessResult processResult;
        try {
            processResult = future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e ) {
            Thread.currentThread().interrupt();
            return 8888;
        } catch (Exception e) {
            // unexpected, give "unexpected" error code
            return 9999;
        }
        return processResult.getExitValue();
    }
}
