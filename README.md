# test-events-command-runner

Run a command in the process runner.

Use `onBeforeTest` for a command that is called on the `before-test` event. For instance, to initialise a 
test database. If the command is a `readyForStartParticipant`, the start test will be delayed until this
command is finished.

Use the `onStartTest` for the command to run during the test run (e.g. the actual load test).
When the command ends and `continueOnKeepAliveParticipant` is true, a stop test request is sent.
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

Use `onScheduledEvent` for a command that is called in an event scheduler script. 
Use `runcommand` event. Use `name` to match one specific command runner event to trigger.
Use key=value parameters with `;` separated key=value pairs. 
In the command surround the keys underscores to be replaced, like `__key__`.
Use `__testRunId__` to be replaced by the test run id from the current test context.

Example of a line in a schedule script is: 

    PT30S|run-command(scale to 3)|name=k8sCommand;app=myapp;namespace=mynamespace;replicas=3

The `PT30S` is the when the event should happen after start of test, so 30 seconds after the start.
The `run-command(scale to 3)` is the event identifier and a message to be included in the registered event
so you can see what its purpose is. The `name` matches a name of a command runner plugin config. 
If no specific name is given, all command runner configs that contain an `onScheduledEvent` will be triggered.
The `app`, `namespace` and `replicas` are variables used in the command itself. Use underscores in the command
so these are replaced before being run. Example:

    <onScheduledEvent>kubectl -n __namespace__ scale --replicas=__replicas__ --timeout=1m deployment __app__</onScheduledEvent>

Note: used executables in the commands should be available
on the `PATH` of the process that runs this (e.g. the CI server).

Use `sendTestRunConfig` to send the command to Perfana test config. Disabled by default.
Be careful not to send secrets via this option.

# test run id

In commands, use `__testRunId__` to be replaced by the test run id from the current test context.

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
                    <eventSchedulerScript>
                        PT30S|run-command(scale to 3)|name=k8sCommand;app=myapp;namespace=mynamespace;replicas=3
                        PT1M|run-command(scale to 1)|name=k8sCommand;app=myapp;namespace=mynamespace;replicas=1
                    </eventSchedulerScript>
                    <annotations>${annotations}</annotations>
                    <tags>${tags}</tags>
                </testConfig>
                <eventConfigs>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>K6Runner1</name>
                        <continueOnKeepAliveParticipant>true</continueOnKeepAliveParticipant>
                        <onStartTest>echo simulate a running load test; sleep 20; echo end load test simulation</onStartTest>
                        <onBeforeTest>touch /tmp/test-run-1.busy; echo command to start K6 runner 1 for ${testRunId};</onBeforeTest>
                        <onKeepAlive>ls /tmp/test-run-1.busy</onKeepAlive>
                        <onAbort>rm /tmp/test-run-1.busy</onAbort>
                        <onAfterTest>rm /tmp/test-run-1.busy</onAfterTest>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>K6Runner2</name>
                        <continueOnKeepAliveParticipant>true</continueOnKeepAliveParticipant>
                        <onStartTest>echo simulate a running load test; sleep 24; echo end load test simulation</onStartTest>
                        <onBeforeTest>touch /tmp/test-run-2.busy; \
                            echo command to start K6 runner 2</onBeforeTest>
                        <onKeepAlive>ls /tmp | grep -q test-run-2.busy</onKeepAlive>
                        <onAbort>rm /tmp/test-run-2.busy; \
                            echo abort K6 runner 2</onAbort>
                        <onAfterTest>rm /tmp/test-run-2.busy; echo end ${testRunId}</onAfterTest>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.events.commandrunner.CommandRunnerEventConfig">
                        <name>k8sCommand</name>
                        <onScheduledEvent>kubectl -n __namespace__ scale --replicas=__replicas__ --timeout=1m deployment __app__</onScheduledEvent>
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
