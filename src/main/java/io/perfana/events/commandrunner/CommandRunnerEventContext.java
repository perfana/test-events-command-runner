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

    private final String onStartTest;
    private final String onBeforeTest;
    private final String onKeepAlive;
    private final String onAbort;
    private final String onAfterTest;
    private final String onScheduledEvent;
    private final boolean sendTestRunConfig;

    protected CommandRunnerEventContext(EventContext context, String onBeforeTest, String onStartTest, String onKeepAlive, String onAbort, String onAfterTest, String onCustomEvent, boolean sendTestRunConfig) {
        super(context, CommandRunnerEventFactory.class.getName());
        this.onStartTest = onStartTest;
        this.onBeforeTest = onBeforeTest;
        this.onKeepAlive = onKeepAlive;
        this.onAbort = onAbort;
        this.onAfterTest = onAfterTest;
        this.onScheduledEvent = onCustomEvent;
        this.sendTestRunConfig = sendTestRunConfig;
    }

    public String getOnStartTest() {
        return onStartTest;
    }

    public String getOnKeepAlive() {
        return onKeepAlive;
    }

    public String getOnAbort() {
        return onAbort;
    }

    public String getOnAfterTest() {
        return onAfterTest;
    }

    public String getOnBeforeTest() {
        return onBeforeTest;
    }

    public String getOnScheduledEvent() {
        return onScheduledEvent;
    }

    public boolean isSendTestRunConfig() {
        return sendTestRunConfig;
    }

    @Override
    public String toString() {
        return "CommandRunnerEventContext{" +
                "onStartTest='" + onStartTest + '\'' +
                ", onBeforeTest='" + onBeforeTest + '\'' +
                ", onKeepAlive='" + onKeepAlive + '\'' +
                ", onAbort='" + onAbort + '\'' +
                ", onAfterTest='" + onAfterTest + '\'' +
                ", onScheduledEvent='" + onScheduledEvent + '\'' +
                ", sendTestRunConfig=" + sendTestRunConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CommandRunnerEventContext that = (CommandRunnerEventContext) o;
        return sendTestRunConfig == that.sendTestRunConfig && Objects.equals(onStartTest, that.onStartTest) && Objects.equals(onBeforeTest, that.onBeforeTest) && Objects.equals(onKeepAlive, that.onKeepAlive) && Objects.equals(onAbort, that.onAbort) && Objects.equals(onAfterTest, that.onAfterTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), onStartTest, onBeforeTest, onKeepAlive, onAbort, onAfterTest, sendTestRunConfig);
    }
}
