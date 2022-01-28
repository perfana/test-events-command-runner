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

import io.perfana.eventscheduler.api.EventGenerator;
import io.perfana.eventscheduler.api.EventGeneratorFactory;
import io.perfana.eventscheduler.api.EventGeneratorProperties;
import io.perfana.eventscheduler.api.EventLogger;

public class CommandRunnerEventGeneratorFactory implements EventGeneratorFactory {

    @Override
    public EventGenerator create(EventGeneratorProperties eventGeneratorProperties, EventLogger eventLogger) {
        return new CommandRunnerEventGenerator(eventGeneratorProperties, eventLogger);
    }
}
