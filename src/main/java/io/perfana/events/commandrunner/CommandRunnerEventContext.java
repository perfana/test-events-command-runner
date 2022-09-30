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

import io.perfana.eventscheduler.api.config.EventContext;
import net.jcip.annotations.Immutable;

import java.util.Objects;

@Immutable
public class CommandRunnerEventContext extends EventContext {

    private final String command;

    private final String pollingCommand;

    private final String abortCommand;

    private final String afterTestCommand;
    private final boolean sendTestRunConfig;

    private final boolean useCommandForPolling;

    protected CommandRunnerEventContext(EventContext context, String command, String pollingCommand, String abortCommand, String afterTestCommand, boolean sendTestRunConfig, boolean useCommandForPolling) {
        super(context, CommandRunnerEventFactory.class.getName());
        this.command = command;
        this.pollingCommand = pollingCommand;
        this.abortCommand = abortCommand;
        this.afterTestCommand = afterTestCommand;
        this.sendTestRunConfig = sendTestRunConfig;
        this.useCommandForPolling = useCommandForPolling;
    }

    public String getCommand() {
        return command;
    }

    public String getPollingCommand() {
        return pollingCommand;
    }

    public String getAbortCommand() {
        return abortCommand;
    }

    public String getAfterTestCommand() {
        return afterTestCommand;
    }

    public boolean isSendTestRunConfig() {
        return sendTestRunConfig;
    }

    public boolean isUseCommandForPolling() {
        return useCommandForPolling;
    }

    @Override
    public String toString() {
        return "CommandRunnerEventContext{" +
                "command='" + command + '\'' +
                ", pollingCommand='" + pollingCommand + '\'' +
                ", abortCommand='" + abortCommand + '\'' +
                ", afterTestCommand='" + afterTestCommand + '\'' +
                ", sendTestRunConfig=" + sendTestRunConfig +
                ", useCommandForPolling=" + useCommandForPolling +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CommandRunnerEventContext that = (CommandRunnerEventContext) o;
        return sendTestRunConfig == that.sendTestRunConfig && useCommandForPolling == that.useCommandForPolling && Objects.equals(command, that.command) && Objects.equals(pollingCommand, that.pollingCommand) && Objects.equals(abortCommand, that.abortCommand) && Objects.equals(afterTestCommand, that.afterTestCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), command, pollingCommand, abortCommand, afterTestCommand, sendTestRunConfig, useCommandForPolling);
    }
}
