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

import io.perfana.eventscheduler.api.CustomEvent;
import io.perfana.eventscheduler.api.EventAdapter;
import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.config.TestContext;
import io.perfana.eventscheduler.api.message.EventMessage;
import io.perfana.eventscheduler.api.message.EventMessageBus;
import io.perfana.eventscheduler.exception.EventSchedulerRuntimeException;
import io.perfana.eventscheduler.exception.handler.StopTestRunException;
import io.perfana.eventscheduler.util.TestRunConfigUtil;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.perfana.events.commandrunner.CommandRunnerEvent.AllowedCustomEvents.runcommand;
import static io.perfana.events.commandrunner.CommandRunnerEvent.AllowedCustomEvents.stream;
import static io.perfana.events.commandrunner.PrefixedRedirectOutput.RedirectType.STDERR;
import static io.perfana.events.commandrunner.PrefixedRedirectOutput.RedirectType.STDOUT;

public class CommandRunnerEvent extends EventAdapter<CommandRunnerEventContext> {

    private Map<String, Future<ProcessResult>> futures = new ConcurrentHashMap<>();

    private final boolean isWindows;

    enum AllowedCustomEvents {
        runcommand("run-command");

        private final String eventName;

        AllowedCustomEvents(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }

        public static Stream<AllowedCustomEvents> stream() {
            return Stream.of(values());
        }

        public boolean hasEventName(String name) {
            return this.eventName.equals(name);
        }
    }

    private final Set<String> allowedCustomEvents = setOf(stream()
            .map(AllowedCustomEvents::getEventName)
            .toArray(String[]::new));

    @Override
    public Collection<String> allowedCustomEvents() {
        return allowedCustomEvents;
    }

    public CommandRunnerEvent(CommandRunnerEventContext eventContext, TestContext testContext, EventMessageBus messageBus, EventLogger logger) {
        super(eventContext, testContext, messageBus, logger);
        isWindows = systemGetPropertyNullSafe("os.name", logger).startsWith("Windows");
        this.eventMessageBus.addReceiver(m -> logger.debug("Received message: " + m));
    }

    private static String systemGetPropertyNullSafe(String property, EventLogger logger) {
        String prop = System.getProperty(property);
        if (prop == null) {
            logger.warn("System property [" + property + "] is not set!");
        }
        return prop == null ? "" : prop;
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {
        String eventName = scheduleEvent.getName();
        try {
            if (runcommand.hasEventName(eventName)) {
                Map<String, String> parsedSettings = parseSettings(scheduleEvent.getSettings());

                // if name is set, only run the command if the name matches
                String name = parsedSettings.get("name");
                if (name != null && !eventContext.getName().equals(name)) {
                    logger.info("Ignoring event [" + eventName + "] for [" + name + "], this is [" + eventContext.getName() + "]");
                    return;
                }

                String command = eventContext.getOnScheduledEvent();
                command = parsedSettings.entrySet().stream()
                        .reduce(command, (k, v) -> k.replaceAll("__" + v.getKey() + "__", v.getValue()), String::concat);

                runCommand(command, "scheduledEvent");
            } else {
                logger.warn("ignoring unknown event [" + eventName + "]");
            }
        } catch (Exception e) {
            logger.error("Failed to run custom event: " + eventName, e);
        }
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

        runBeforeTestNoWait(pluginName);
        runBeforeTest(pluginName);

    }

    private void runBeforeTest(String pluginName) {
        String command = eventContext.getOnBeforeTest();

        if (command.isEmpty()) {
            logger.debug("No command to run for beforeTest");
        }
        else {
            Future<ProcessResult> beforeTestCommandFuture = runCommand(command, "beforeTest");

            if (beforeTestCommandFuture == null) {
                logger.debug("No result available for beforeTest command");
            } else {
                waitForCommandToFinishOrTimeout(beforeTestCommandFuture);
            }
        }

        if (eventContext.isReadyForStartParticipant()) {
            this.eventMessageBus.send(EventMessage.builder().pluginName(pluginName).message("Go!").build());
        }
    }

    private void runBeforeTestNoWait(String pluginName) {
        String command = eventContext.getOnBeforeTestNoWait();

        if (command.isEmpty()) {
            logger.debug("No command to run for beforeTest");
            return;
        }

        Future<ProcessResult> future = runCommand(command, "beforeTest");
        if (future != null) {
            futures.put("beforeTestNoWait", future);
        } else {
            logger.debug("No result available for beforeTest command");
        }

        if (future == null) {
            logger.debug("No result available for beforeTest command");
        }
    }

    private void waitForCommandToFinishOrTimeout(Future<ProcessResult> command) {
        if (command == null) {
            logger.warn("No command to wait for");
            return;
        }

        try {
            ProcessResult processResult = command.get(120, TimeUnit.SECONDS);
            if (processResult.getExitValue() != 0) {
                logger.warn("Command did not end successfully. Exit code: " + processResult.getExitValue());
            }
            else {
                logger.info("Command ended. Is done: " + command.isDone());
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
        String command = eventContext.getOnStartTest();
        Future<ProcessResult> future = runCommand(command, "startTest");
        if (future != null) {
            futures.put("startTest", future);
        }
    }

    @Override
    public void keepAlive() {

        Future<ProcessResult> future = futures.get("startTest");

        if (isContinueOnKeepAliveParticipant() && future != null && future.isDone()) {
            int exitCode = getExitCode(future);
            String message = "The command is done (exit code: " + exitCode + ") and this is a continueOnKeepAlive participant: will request to stop test run.";
            logger.info(message);
            throw new StopTestRunException(message);
        }

        String keepAliveCommand = eventContext.getOnKeepAlive();

        Future<ProcessResult> processResultFuture = runCommand(keepAliveCommand, "keepAlive");

        if (processResultFuture == null) {
            return;
        }

        ProcessResult processResult;
        try {
            processResult = processResultFuture.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Keep-alive command got interrupted! " + e.getMessage());
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            logger.warn("Keep-alive command cannot be executed! " + e.getMessage());
            return;
        } catch (TimeoutException e) {
            logger.warn("Keep-alive command got timeout! " + e.getMessage());
            return;
        }

        int exitValue = processResult.getExitValue();
        if (exitValue != 0) {
            String stopMessage = isContinueOnKeepAliveParticipant()
                    ? "Is continueOnKeepAliveParticipant: will request a stop test run."
                    : "Is no continueOnKeepAliveParticipant: will not request a stop test run.";
            String message = "Received failed (non-zero) exit value for keep-alive command (exit: " + exitValue + "). ";
            logger.info(message + stopMessage);
            if (isContinueOnKeepAliveParticipant()) {
                logger.info("This is a continueOnKeepAlive participant, will request to stop test run.");
                throw new StopTestRunException(message);
            }
        }
        else {
            logger.info("Received success (zero) exit value for keep-alive command, keep on running!");
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

        String onAfterTestCommand = eventContext.getOnAfterTest();
        if (onAfterTestCommand.isEmpty()) {
            logger.debug("No command to run for afterTest");
            return;
        }
        Future<ProcessResult> future = runCommand(onAfterTestCommand, "afterTest");
        waitForCommandToFinishOrTimeout(future);
    }

    private Future<ProcessResult> runCommand(String command, String commandType) {
        if (command.isEmpty()) {
            logger.debug("No command to run for " + commandType);
            return null;
        }
        logger.info("About to run " + commandType + " [" + command + "]");

        String newTestRunId = testContext.getTestRunId();
        command = command.replace("__testRunId__", newTestRunId);

        List<String> commandList;

        if (isWindows) {
            commandList = createCommandList(command);
        }
        else {
            commandList = createCommandListWithShWrapper(command);
        }

        commandList = injectTestRunIdInCommandList(commandList);

        Future<ProcessResult> myProcessResult;
        try {
            myProcessResult = new ProcessExecutor()
                .command(commandList)
                .redirectOutput(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.out, STDOUT))
                .redirectError(new PrefixedRedirectOutput(eventContext.getName() + ": ", System.err, STDERR))
                .start().getFuture();
        } catch (IOException e) {
            throw new EventSchedulerRuntimeException("Failed to run command: " + command, e);
        }
        return myProcessResult;
    }

    public static List<String> createCommandList(String command) {
        return Arrays.asList(command.split("\\s+"));
    }

    private List<String> injectTestRunIdInCommandList(List<String> commandList) {
        String testRunId = testContext.getTestRunId();
        return commandList.stream()
                .map(s -> s.replace("__testRunId__", testRunId))
                .collect(Collectors.toList());
    }

    private Map<String, String> createTestRunConfigLines() {
        String prefix = "event." + eventContext.getName() + ".";
        Map<String, String> lines = new HashMap<>();
        lines.put(prefix + "command", eventContext.getOnStartTest());
        return lines;
    }

    public static List<String> createCommandListWithShWrapper(String command) {
        String strippedCommand;
        if (command.startsWith("sh -c")) {
            strippedCommand = command.substring("sh -c".length()).trim();
            if ((strippedCommand.startsWith("'") && strippedCommand.endsWith("'"))
                    || (strippedCommand.startsWith("\"") && strippedCommand.endsWith("\""))) {
                strippedCommand = strippedCommand.substring(1, strippedCommand.length() - 1);
            }
        }
        else {
            strippedCommand = command;
        }

        List<String> commandList = new ArrayList<>();
        commandList.add("sh");
        commandList.add("-c");
        commandList.add(strippedCommand.trim());
        return commandList;
    }

    private void abortCommand() {
        String abortCommand = eventContext.getOnAbort();
        if (abortCommand.isEmpty()) {
            logger.debug("No command to run for abortCommand");
            return;
        }
        Future<ProcessResult> future = runCommand(abortCommand, "abortCommand");
        waitForCommandToFinishOrTimeout(future);
    }

    private void cancelCommand() {
        String command = eventContext.getOnStartTest();
        for (String key : futures.keySet()) {
            cancelCommand(command, futures.get(key));
        }
    }

    private void cancelCommand(String command, Future<ProcessResult> future) {
        if (future != null) {
            logger.debug("There is a future for [ " + command + "]");
            if (!future.isDone()) {
                logger.info("About to cancel [" + command + "] for [" + testContext.getTestRunId() + "]");
                boolean cancel = future.cancel(true);
                logger.debug("Cancel [" + cancel + "] for [" + testContext.getTestRunId() + "]");
            }
            else {
                logger.info("No cancel needed for finished command for [" + testContext.getTestRunId() + "]");
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

    static Map<String, String> parseSettings(String eventSettings) {
        if (eventSettings == null || eventSettings.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(eventSettings.split(";"))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(k -> k[0], v -> v.length == 2 ? v[1] : ""));
    }
}
