# Release notes Perfana Test Events Command Runner

## 3.0.2 - February 2024

* use `onScheduledEvent` to run a command on as scheduled event

## 3.0.1 - October 2023

* use `__testRunId__` in commands to be replaced by test run id from the current text context

## 3.0.0 - April 2023

* uses event-scheduler 4.0.0: no more TestConfig at EventConfig level
* uses java 11 instead of java 8

## 3.0.3 - January 2025

* added `onBeforeTestNoWait` to run a command before the test starts without waiting for it to finish
* uses event-scheduler 4.0.5: improved executor handling on shutdown
* added timestamp and STDOUT/STDERR to event output