# Simple test case

Try out the command runner with the `event-scheduler-maven-plugin`.

Run with:

```bash
../mvnw event-scheduler:test
```

or with debug:

```bash
../mvnw -X event-scheduler:test
```

# Test keep alive

For the `TestCommand-Two` replace `true` with `false` in the `onKeepAlive` section of the `event-scheduler-maven-plugin` configuration.
Now the run will stop after the `onKeepAlive` was called. Set `<continueOnKeepAliveParticipant>` to `false` to disable the stop behaviour.

```xml
<onKeepAlive>echo keep alive 2; sleep 1; echo end keep alive 2; true</onKeepAlive>
```