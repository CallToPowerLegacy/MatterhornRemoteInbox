/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import java.util.concurrent.FutureTask;

/**
 * InboxTask
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class InboxTask {

    public enum Type {

        STARTING, CREATE_MEDIAPACKAGE, UPLOAD_FILE, START_INGEST
    };
    private final Type type;
    private final FutureTask task;

    public InboxTask(Type type, FutureTask task) {
        this.type = type;
        this.task = task;
    }

    public Type getType() {
        return type;
    }

    public FutureTask getFutureTask() {
        return task;
    }
}
