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

import io.perfana.eventscheduler.EventMessageBusSimple;
import io.perfana.eventscheduler.api.config.TestConfig;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.jupiter.api.Test;

class ExecProcessTest {

    @Test
    void execProcessHangs() {
        CommandRunnerEventConfig eventConfig = new CommandRunnerEventConfig();
        eventConfig.setOnBeforeTestNoWait("sleep 1");
        CommandRunnerEvent event = new CommandRunnerEvent(eventConfig.toContext(), TestConfig.builder().build().toContext(), new EventMessageBusSimple(), EventLoggerStdOut.INSTANCE);
        event.beforeTest();
    }

}
