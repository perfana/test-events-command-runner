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
import io.perfana.eventscheduler.api.message.EventMessageBus;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandRunnerEventTest {

    @Test
    void beforeTest() {
        CommandRunnerEventConfig eventConfig = new CommandRunnerEventConfig();
        eventConfig.setEventFactory(CommandRunnerEventFactory.class.getSimpleName());
        eventConfig.setName("myEvent1");
        eventConfig.setEnabled(true);
        eventConfig.setSendTestRunConfig(true);
        eventConfig.setOnBeforeTest("echo hello there");
        eventConfig.setOnStartTest("echo something >&2; for (( ; ; )) do echo __testRunId__ $(date); sleep 0.01; done");
        eventConfig.setOnKeepAlive("sh -c \"echo this is a keep-alive command simulation; exit 0\"");
        eventConfig.setOnAbort("echo this is an echo abort command");

        EventMessageBus messageBus = new EventMessageBusSimple();

        CommandRunnerEvent event = new CommandRunnerEvent(eventConfig.toContext(), TestConfig.builder().build().toContext(), messageBus, EventLoggerStdOut.INSTANCE);
        event.beforeTest();
        event.startTest();
        event.keepAlive();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        event.abortTest();
        event.afterTest();


        // not much to assert really... just look at System.out and
        // check it does not blow with an Exception...

    }

    @Test
    void testCommandSplit() {
        List<String> commandList = CommandRunnerEvent.createCommandList("abc def 123  \"333\"");
        assertEquals(4, commandList.size());
        assertEquals("abc", commandList.get(0));
        assertEquals("\"333\"", commandList.get(3));
    }

}
