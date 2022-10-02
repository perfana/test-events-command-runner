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

    private String onStartTest = "";

    private String onBeforeTest = "";
    private String onKeepAlive = "";

    private String onAbort = "";

    private String onAfterTest = "";

    private boolean sendTestRunConfig = false;

    @Override
    public CommandRunnerEventContext toContext() {
        return new CommandRunnerEventContext(super.toContext(), onBeforeTest, onStartTest, onKeepAlive, onAbort, onAfterTest, sendTestRunConfig);
    }

    @Override
    public CommandRunnerEventContext toContext(TestContext override) {
        return new CommandRunnerEventContext(super.toContext(override), onBeforeTest, onStartTest, onKeepAlive, onAbort, onAfterTest, sendTestRunConfig);
    }

    public String getOnBeforeTest() {
        return onBeforeTest;
    }

    public void setOnBeforeTest(String onBeforeTest) {
        this.onBeforeTest = onBeforeTest;
    }

    public void setOnStartTest(String onStartTest) {
        this.onStartTest = onStartTest;
    }

    public String getOnStartTest() {
        return onStartTest;
    }

    public boolean isSendTestRunConfig() {
        return sendTestRunConfig;
    }

    public void setSendTestRunConfig(boolean sendTestRunConfig) {
        this.sendTestRunConfig = sendTestRunConfig;
    }

    public String getOnKeepAlive() {
        return onKeepAlive;
    }

    public void setOnKeepAlive(String onKeepAlive) {
        this.onKeepAlive = onKeepAlive;
    }

    public String getOnAbort() {
        return onAbort;
    }

    public void setOnAbort(String onAbort) {
        this.onAbort = onAbort;
    }

    public String getOnAfterTest() {
        return onAfterTest;
    }

    public void setOnAfterTest(String onAfterTest) {
        this.onAfterTest = onAfterTest;
    }

    @Override
    public String toString() {
        return "CommandRunnerEventConfig{" +
                "onStartTest='" + onStartTest + '\'' +
                ", onBeforeTest='" + onBeforeTest + '\'' +
                ", onKeepAlive='" + onKeepAlive + '\'' +
                ", onAbort='" + onAbort + '\'' +
                ", onAfterTest='" + onAfterTest + '\'' +
                ", sendTestRunConfig=" + sendTestRunConfig +
                '}';
    }

}
