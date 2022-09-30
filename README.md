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

If `useCommandForPolling` is true (default is false) and `continueOnKeepAliveParticipant` is true, 
a check is made in each keep alive to see if the `command` is done/finished. 
If so, it will request a stop. If also a `pollingCommand` is
defined, that command will also be checked if the command is not done yet.

Use `abortCommand` for a command that is called on an `abort-test` event. For instance, to stop a 
remote running load test process.

Use `afterTestCommand` for a command that is called on an `after-test` event. For instance, clean up
artifacts from a another command.

Note: used executables in the commands should be available
on the `PATH` of the process that runs this (e.g. the CI server).

Use `sendTestRunConfig` to send the command to Perfana test config. Disabled by default.
Be careful not to send secrets via this option.

## use

In this example the first command has `useCommandForPolling` enabled, and will request a stop after 20 seconds of sleep.
But since the second command is also a `continueOnKeepAliveParticipant`, the `/tmp/test-run-2.busy` file should also
be removed to actually have the test stopped.

ALso when both `/tmp/test-run-1.busy` and `/tmp/test-run-2.busy` are removed the test will stop.

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
                        <useCommandForPolling>true</useCommandForPolling>
                        <command>touch /tmp/test-run-1.busy; \
                            echo command to start K6 runner 1 for ${testRunId}; \
                            sleep 20
                        </command>
                        <pollingCommand>ls /tmp/test-run-1.busy</pollingCommand>
                        <abortCommand>rm /tmp/test-run-1.busy; \
                            echo abort K6 runner 1</abortCommand>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>K6Runner2</name>
                        <continueOnKeepAliveParticipant>true</continueOnKeepAliveParticipant>
                        <command>touch /tmp/test-run-2.busy; \
                            echo command to start K6 runner 2</command>
                        <pollingCommand>ls /tmp | grep -q test-run-2.busy</pollingCommand>
                        <abortCommand>rm /tmp/test-run-2.busy; \
                            echo abort K6 runner 2</abortCommand>
                        <afterTestCommand>echo end ${testRunId}</afterTestCommand>
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
