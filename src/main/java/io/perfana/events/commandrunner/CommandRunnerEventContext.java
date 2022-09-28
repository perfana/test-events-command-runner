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
    private final boolean sendTestRunConfig;

    protected CommandRunnerEventContext(EventContext context, String command, String pollingCommand, String abortCommand, boolean sendTestRunConfig) {
        super(context, CommandRunnerEventFactory.class.getName());
        this.command = command;
        this.pollingCommand = pollingCommand;
        this.abortCommand = abortCommand;
        this.sendTestRunConfig = sendTestRunConfig;
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

    public boolean isSendTestRunConfig() {
        return sendTestRunConfig;
    }

    @Override
    public String toString() {
        return "CommandRunnerEventContext{" +
                "command='" + command + '\'' +
                ", pollingCommand='" + pollingCommand + '\'' +
                ", abortCommand='" + abortCommand + '\'' +
                ", sendTestRunConfig=" + sendTestRunConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CommandRunnerEventContext that = (CommandRunnerEventContext) o;
        return sendTestRunConfig == that.sendTestRunConfig && Objects.equals(command, that.command) && Objects.equals(pollingCommand, that.pollingCommand) && Objects.equals(abortCommand, that.abortCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), command, pollingCommand, abortCommand, sendTestRunConfig);
    }
}
