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

import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Output stream that inserts a prefix at each new line.
 *
 * Tries to avoid output interleaving.
 *
 * Might now work so well on windows (regarding new lines).
 */
public class PrefixedRedirectOutput extends OutputStream {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final int BUFFER_SIZE = 1024;

    private final RedirectType redirectType;

    private final Object bufferLock = new Object();
    @GuardedBy("bufferLock")
    private final byte[] buffer = new byte[BUFFER_SIZE];

    @GuardedBy("bufferLock")
    private int pointer;

    private final String prefix;

    private final OutputStream wrappedOS;

    private final String newLine = System.lineSeparator();
    private final int[] nl = newLine.chars().toArray();
    private final int chars = nl.length;

    private volatile boolean start = true;
    private volatile boolean thereIsMore = false;

    public PrefixedRedirectOutput(String prefix, OutputStream wrappedOS, RedirectType redirectType) {
        super();
        this.prefix = prefix;
        this.wrappedOS = wrappedOS;
        this.redirectType = redirectType;
    }

    @Override
    public void write(int b) throws IOException {
        if (start) {
            start = false;
            wrappedOS.write(createPrefix());
        }
        if (thereIsMore) {
            wrappedOS.write(createPrefix());
            thereIsMore = false;
        }

        synchronized (bufferLock) {
            buffer[pointer++] = (byte) b;
            if (pointer == BUFFER_SIZE) {
                flushBuffer();
            }
        }

        if ((chars == 1 && nl[0] == b) || (chars == 2 && nl[1] == b)) {
            thereIsMore = true;
            flushBuffer();
        }
    }

    private byte[] createPrefix() {
        String fullPrefix = formatTime() + " " + redirectType + " " + prefix;
        return fullPrefix.getBytes(StandardCharsets.UTF_8);
    }

    private String formatTime() {
        return TIME_FORMATTER.format(LocalTime.now());
    }

    private void flushBuffer() throws IOException {
        // This might be a nasty thing to do: high-jack
        // the lock of a foreign object!
        // Can cause deadlocks?
        // Trying to reduce change of output interleaving...
        synchronized (wrappedOS) {
            synchronized (bufferLock) {
                wrappedOS.write(buffer, 0, pointer);
                pointer = 0;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        wrappedOS.flush();
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        write(createPrefix());
        write(" END!".getBytes(StandardCharsets.UTF_8));
        write(newLine.getBytes(StandardCharsets.UTF_8));
        wrappedOS.close();
    }

    public enum RedirectType {
        STDOUT,
        STDERR
    }
}
