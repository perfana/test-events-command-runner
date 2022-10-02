# test-events-command-runner

Run a command in the process runner.

Use `onBeforeTest` for a command that is called on the `before-test` event. For instance, to initialise a 
test database. If the command is a `readyForStartParticipant`, the start test will be delayed until this
command is finished.

Use the `command` tag for the command to run during the test run, or as load test actually.
When the command ends and the command is `continueOnKeepAliveParticipant`, a stop test request is sent.
If the command runs for a long time (e.g. a non-self ending load test), it is cancelled at the end of the
test run in the `after-test` or `abort-test` events.

Use `onKeepAlive` for a command that is run every keep-alive event. You can use this
to poll the running of a remote load test process for instance. When this command does not
return success (exit code 0), a stop is requested.

Use `onAbort` for a command that is called on an `abort-test` event. For instance, to stop a 
remote running load test process.

Use `onAfterTest` for a command that is called on an `after-test` event. For instance, clean up
artifacts from a another command.

Only when _all_ polling commands that are indicated as `continueOnKeepAliveParticipant` request a stop,
the test-run is actually stopped.

Note: used executables in the commands should be available
on the `PATH` of the process that runs this (e.g. the CI server).

Use `sendTestRunConfig` to send the command to Perfana test config. Disabled by default.
Be careful not to send secrets via this option.

## use

In this example the first command has a simulated load test by sleeping 20 seconds, the second 24 seconds.
When both commands are done, a stop test is requested.

The test will also stop when both `/tmp/test-run-1.busy` and `/tmp/test-run-2.busy` are removed before the 
commands end, e.g. by running `rm -v /tmp/test-run-*.busy`.

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
                        <command>echo simulate a running load test; sleep 20; echo end load test simulation</command>
                        <onBeforeTest>touch /tmp/test-run-1.busy; echo command to start K6 runner 1 for ${testRunId};</onBeforeTest>
                        <onKeepAlive>ls /tmp/test-run-1.busy</onKeepAlive>
                        <onAbort>rm /tmp/test-run-1.busy</onAbort>
                        <onAfterTest>rm /tmp/test-run-1.busy</onAfterTest>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>K6Runner2</name>
                        <continueOnKeepAliveParticipant>true</continueOnKeepAliveParticipant>
                        <command>echo simulate a running load test; sleep 24; echo end load test simulation</command>
                        <onBeforeTest>touch /tmp/test-run-2.busy; \
                            echo command to start K6 runner 2</onBeforeTest>
                        <onKeepAlive>ls /tmp | grep -q test-run-2.busy</onKeepAlive>
                        <onAbort>rm /tmp/test-run-2.busy; \
                            echo abort K6 runner 2</onAbort>
                        <onAfterTest>rm /tmp/test-run-2.busy; echo end ${testRunId}</onAfterTest>
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
