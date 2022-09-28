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

import io.perfana.eventscheduler.api.config.EventConfig;
import io.perfana.eventscheduler.api.config.TestContext;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class CommandRunnerEventConfig extends EventConfig {

    private String command;

    private String pollingCommand;

    private String abortCommand;

    private boolean sendTestRunConfig = false;

    @Override
    public CommandRunnerEventContext toContext() {
        return new CommandRunnerEventContext(super.toContext(), command, pollingCommand, abortCommand, sendTestRunConfig);
    }

    @Override
    public CommandRunnerEventContext toContext(TestContext override) {
        return new CommandRunnerEventContext(super.toContext(override), command, pollingCommand, abortCommand, sendTestRunConfig);
    }

    @Override
    public String toString() {
        return "CommandRunnerEventConfig{" +
                "command='" + command + '\'' +
                ", pollingCommand='" + pollingCommand + '\'' +
                ", abortCommand='" + abortCommand + '\'' +
                ", sendTestRunConfig=" + sendTestRunConfig +
                '}';
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public boolean isSendTestRunConfig() {
        return sendTestRunConfig;
    }

    public void setSendTestRunConfig(boolean sendTestRunConfig) {
        this.sendTestRunConfig = sendTestRunConfig;
    }

    public String getPollingCommand() {
        return pollingCommand;
    }

    public void setPollingCommand(String pollingCommand) {
        this.pollingCommand = pollingCommand;
    }

    public String getAbortCommand() {
        return abortCommand;
    }

    public void setAbortCommand(String abortCommand) {
        this.abortCommand = abortCommand;
    }
}
