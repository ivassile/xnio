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

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 *
 */
public final class NioXnioConfiguration {
    private int readSelectorThreads = 2;
    private int writeSelectorThreads = 1;
    private int connectSelectorThreads = 1;

    private Executor executor;
    private ThreadFactory selectorThreadFactory;

    private String name;

    public int getReadSelectorThreads() {
        return readSelectorThreads;
    }

    public void setReadSelectorThreads(final int readSelectorThreads) {
        this.readSelectorThreads = readSelectorThreads;
    }

    public int getWriteSelectorThreads() {
        return writeSelectorThreads;
    }

    public void setWriteSelectorThreads(final int writeSelectorThreads) {
        this.writeSelectorThreads = writeSelectorThreads;
    }

    public int getConnectSelectorThreads() {
        return connectSelectorThreads;
    }

    public void setConnectSelectorThreads(final int connectSelectorThreads) {
        this.connectSelectorThreads = connectSelectorThreads;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    public ThreadFactory getSelectorThreadFactory() {
        return selectorThreadFactory;
    }

    public void setSelectorThreadFactory(final ThreadFactory selectorThreadFactory) {
        this.selectorThreadFactory = selectorThreadFactory;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
