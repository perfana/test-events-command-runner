# test-events-command-runner

Run a command in the process runner.

Use the `command` tag for the command to run. Used executables should be available
on the `PATH` of the process that runs this (e.g. the CI server).

When an abort happens, the process is killed.

At the end of the running time, the process is killed.

Use `sendTestRunConfig` to send the command to Perfana test config. Disabled by default.
Be careful not to send secrets via this option.

## use

```xml
<plugins>
    <plugin>
        <groupId>io.perfana</groupId>
        <artifactId>event-scheduler-maven-plugin</artifactId>
        <configuration>
            <eventSchedulerConfig>
                <debugEnabled>true</debugEnabled>
                <schedulerEnabled>true</schedulerEnabled>
                <failOnError>true</failOnError>
                <continueOnEventCheckFailure>true</continueOnEventCheckFailure>
                <eventScheduleScript>
                </eventScheduleScript>
                <testConfig>
                    <systemUnderTest>${systemUnderTest}</systemUnderTest>
                    <version>${version}</version>
                    <workload>${workload}</workload>
                    <testEnvironment>${testEnvironment}</testEnvironment>
                    <testRunId>${testRunId}</testRunId>
                    <buildResultsUrl>${buildResultsUrl}</buildResultsUrl>
                    <rampupTimeInSeconds>${rampupTimeInSeconds}</rampupTimeInSeconds>
                    <constantLoadTimeInSeconds>${constantLoadTimeInSeconds}</constantLoadTimeInSeconds>
                    <annotations>${annotations}</annotations>
                    <tags>${tags}</tags>
                </testConfig>
                <eventConfigs>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>CommandRunnerEventSleep1</name>
                        <command>sleep 10</command>
                        <sendTestRunConfig>true</sendTestRunConfig>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>CommandRunnerEventSleep2</name>
                        <command>sleep 20</command>
                    </eventConfig>
                </eventConfigs>
            </eventSchedulerConfig>
        </configuration>
        <dependencies>
            <dependency>
                <groupId>io.perfana</groupId>
                <artifactId>test-events-command-runner</artifactId>
                <version>${test-events-command-runner.version}</version>
            </dependency>
            <dependency>
                <groupId>io.perfana</groupId>
                <artifactId>perfana-java-client</artifactId>
                <version>${perfana-java-client.version}</version>
            </dependency>
        </dependencies>
    </plugin>
</plugins>
```

See also:
* https://github.com/perfana/event-scheduler-maven-plugin
* https://github.com/perfana/event-scheduler
* https://github.com/perfana/perfana-java-client
