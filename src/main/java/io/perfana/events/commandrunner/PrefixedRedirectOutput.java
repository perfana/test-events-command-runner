package io.perfana.events.commandrunner;

import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


/**
 * Output stream that inserts a prefix at each new line.
 *
 * Tries to avoid output interleaving.
 *
 * Might now work so well on windows (regarding new lines).
 */
public class PrefixedRedirectOutput extends OutputStream {

    private static final int BUFFER_SIZE = 1024;

    private final Object bufferLock = new Object();
    @GuardedBy("bufferLock")
    private final byte[] buffer = new byte[BUFFER_SIZE];
    @GuardedBy("bufferLock")
    private int pointer;

    private final byte[] prefix;

    private final OutputStream wrappedOS;

    private final String newLine = System.lineSeparator();
    private final int[] nl = newLine.chars().toArray();
    private final int chars = nl.length;

    private volatile boolean start = true;
    private volatile boolean thereIsMore = false;

    public PrefixedRedirectOutput(String prefix, OutputStream wrappedOS) {
        super();
        this.prefix = prefix.getBytes(StandardCharsets.UTF_8);
        this.wrappedOS = wrappedOS;
    }

    @Override
    public void write(int b) throws IOException {
        if (start) {
            start = false;
            wrappedOS.write(prefix);
        }
        if (thereIsMore) {
            wrappedOS.write(prefix);
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

    private void flushBuffer() throws IOException {
        // This might be a nasty thing to do: high-jack
        // the lock of a foreign object!
        // Can cause deadlocks?
        // Trying to reduce change of output interleaving...
        //synchronized (wrappedOS) {
            synchronized (bufferLock) {
                wrappedOS.write(buffer, 0, pointer);
                pointer = 0;
            }
        //}
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        wrappedOS.flush();
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        write(prefix);
        write(" END!".getBytes(StandardCharsets.UTF_8));
        write(newLine.getBytes(StandardCharsets.UTF_8));
        wrappedOS.close();
    }
}
