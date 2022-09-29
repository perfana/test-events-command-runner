# test-events-command-runner

Run a command in the process runner.

Use the `command` tag for the command to run.
If the command runs for a long time (e.g. a non-self ending load test), it is cancelled at the end of the
test run in the `after-test` or `abort-test` events.

Use `pollingCommand` for a command that is run every keep-alive event. You can use this
to poll the running of a remote load test process for instance. When this command does not
return success (exit code 0), a stop is requested.

Only when _all_ polling commands that are indicated as `continueOnKeepAliveParticipant` request a stop,
the test-run is actually stopped.

Use `abortCommand` for a command that is called on an `abort-test` event. For instance, to stop a 
remote running load test process.

Note: used executables in the commands should be available
on the `PATH` of the process that runs this (e.g. the CI server).

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
                <testConfig>
                    <systemUnderTest>${systemUnderTest}</systemUnderTest>
                    <version>${version}</version>
                    <workload>${workload}</workload>
                    <testEnvironment>${testEnvironment}</testEnvironment>
                    <testRunId>${testRunId}</testRunId>
                    <buildResultsUrl>${CIBuildResultsUrl}</buildResultsUrl>
                    <rampupTimeInSeconds>${rampupTimeInSeconds}</rampupTimeInSeconds>
                    <constantLoadTimeInSeconds>${constantLoadTimeInSeconds}</constantLoadTimeInSeconds>
                    <annotations>${annotations}</annotations>
                    <tags>${tags}</tags>
                </testConfig>
                <eventConfigs>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>K6Runner1</name>
                        <continueOnKeepAliveParticipant>true</continueOnKeepAliveParticipant>
                        <command>sh -c "touch /tmp/test-run-1.busy; \
                            echo command to start K6 runner 1"</command>
                        <pollingCommand>ls /tmp/test-run-1.busy</pollingCommand>
                        <abortCommand>sh -c "rm /tmp/test-run-1.busy; \
                            echo abort K6 runner 1"</abortCommand>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>K6Runner2</name>
                        <continueOnKeepAliveParticipant>true</continueOnKeepAliveParticipant>
                        <command>sh -c "touch /tmp/test-run-2.busy; \
                            echo command to start K6 runner 2"</command>
                        <pollingCommand>ls /tmp/test-run-2.busy</pollingCommand>
                        <abortCommand>sh -c "rm /tmp//tmp/test-run-2.busy; \
                            echo abort K6 runner 2"</abortCommand>
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
