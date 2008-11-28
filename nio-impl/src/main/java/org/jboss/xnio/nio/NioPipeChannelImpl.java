/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.xnio.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import org.jboss.xnio.IoHandler;
import org.jboss.xnio.IoUtils;
import org.jboss.xnio.channels.ChannelOption;
import org.jboss.xnio.channels.Configurable;
import org.jboss.xnio.channels.StreamChannel;
import org.jboss.xnio.channels.UnsupportedOptionException;
import org.jboss.xnio.log.Logger;

/**
 *
 */
public final class NioPipeChannelImpl implements StreamChannel {
    private static final Logger log = Logger.getLogger("org.jboss.xnio.nio.pipe.channel");

    private final Pipe.SourceChannel sourceChannel;
    private final Pipe.SinkChannel sinkChannel;
    private final IoHandler<? super StreamChannel> handler;
    private final NioHandle sourceHandle;
    private final NioHandle sinkHandle;
    private final AtomicBoolean callFlag = new AtomicBoolean(false);
    private final NioXnio nioXnio;

    NioPipeChannelImpl(final Pipe.SourceChannel sourceChannel, final Pipe.SinkChannel sinkChannel, final IoHandler<? super StreamChannel> handler, final NioXnio nioXnio) throws IOException {
        this.sourceChannel = sourceChannel;
        this.sinkChannel = sinkChannel;
        this.handler = handler;
        this.nioXnio = nioXnio;
        // todo leaking [this]
        sourceHandle = nioXnio.addReadHandler(sourceChannel, new ReadHandler());
        sinkHandle = nioXnio.addWriteHandler(sinkChannel, new WriteHandler());
    }

    static NioPipeChannelImpl create(final Pipe.SourceChannel sourceChannel, final Pipe.SinkChannel sinkChannel, final IoHandler<? super StreamChannel> handler, final NioXnio nioXnio) throws IOException {
        final NioPipeChannelImpl channel = new NioPipeChannelImpl(sourceChannel, sinkChannel, handler, nioXnio);
        return channel;
    }

    public long write(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
        return sinkChannel.write(srcs, offset, length);
    }

    public long write(final ByteBuffer[] srcs) throws IOException {
        return sinkChannel.write(srcs);
    }

    public int write(final ByteBuffer src) throws IOException {
        return sinkChannel.write(src);
    }

    public boolean isOpen() {
        return sourceChannel.isOpen() && sinkChannel.isOpen();
    }

    public void close() throws IOException {
        // since we've got two channels, only rethrow a failure on the WRITE side, since that's the side that stands to lose data
        IoUtils.safeClose(sourceChannel);
        try {
            sinkChannel.close();
        } finally {
            nioXnio.removeManaged(this);
            if (callFlag.getAndSet(true) == false) {
                HandlerUtils.<StreamChannel>handleClosed(handler, this);
            }
            sinkHandle.cancelKey();
            sourceHandle.cancelKey();
        }
    }

    public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        return sourceChannel.read(dsts, offset, length);
    }

    public long read(final ByteBuffer[] dsts) throws IOException {
        return sourceChannel.read(dsts);
    }

    public int read(final ByteBuffer dst) throws IOException {
        return sourceChannel.read(dst);
    }

    public void suspendReads() {
        try {
            sourceHandle.suspend();
        } catch (CancelledKeyException ex) {
            // ignore
        }
    }

    public void suspendWrites() {
        try {
            sinkHandle.suspend();
        } catch (CancelledKeyException ex) {
            // ignore
        }
    }

    public void resumeReads() {
        try {
            sourceHandle.resume(SelectionKey.OP_READ);
        } catch (CancelledKeyException ex) {
            // ignore
        }
    }

    public void resumeWrites() {
        try {
            sinkHandle.resume(SelectionKey.OP_WRITE);
        } catch (CancelledKeyException ex) {
            // ignore
        }
    }

    public void shutdownReads() throws IOException {
        sourceChannel.close();
    }

    public void shutdownWrites() throws IOException {
        sinkChannel.close();
    }

    public void awaitReadable() throws IOException {
        SelectorUtils.await(SelectionKey.OP_READ, sourceChannel);
    }

    public void awaitReadable(final long time, final TimeUnit timeUnit) throws IOException {
        SelectorUtils.await(SelectionKey.OP_READ, sourceChannel, time, timeUnit);
    }

    public void awaitWritable() throws IOException {
        SelectorUtils.await(SelectionKey.OP_WRITE, sinkChannel);
    }

    public void awaitWritable(final long time, final TimeUnit timeUnit) throws IOException {
        SelectorUtils.await(SelectionKey.OP_WRITE, sinkChannel, time, timeUnit);
    }

    public <T> T getOption(final ChannelOption<T> option) throws UnsupportedOptionException, IOException {
        throw new UnsupportedOptionException("No options supported");
    }

    public Set<ChannelOption<?>> getOptions() {
        return Collections.emptySet();
    }

    public <T> Configurable setOption(final ChannelOption<T> option, final T value) throws IllegalArgumentException, IOException {
        throw new UnsupportedOptionException("No options supported");
    }

    private class ReadHandler implements Runnable {
        public void run() {
            IoHandler<? super StreamChannel> handler = NioPipeChannelImpl.this.handler;
            try {
                handler.handleReadable(NioPipeChannelImpl.this);
            } catch (Throwable t) {
                log.error(t, "Read handler threw an exception");
            }
        }
    }

    private class WriteHandler implements Runnable {
        public void run() {
            IoHandler<? super StreamChannel> handler = NioPipeChannelImpl.this.handler;
            try {
                handler.handleWritable(NioPipeChannelImpl.this);
            } catch (Throwable t) {
                log.error(t, "Write handler threw an exception");
            }
        }
    }

    public String toString() {
        return String.format("pipe channel (NIO) <%s>", Integer.toString(hashCode(), 16));
    }
}
