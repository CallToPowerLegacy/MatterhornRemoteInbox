/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.api;

/**
 * IngestClientException
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class IngestClientException extends Exception {

    /**
     * Types of IngestClientExceptions
     */
    public enum Type {

        GENERAL, NETWORK_ERROR, CLIENT_ERROR, SERVER_ERROR;
    }
    private Type type = Type.GENERAL;

    /**
     * constructor
     *
     * @param message
     */
    public IngestClientException(String message) {
        super(message);
    }

    /**
     * constructor
     *
     * @param message
     * @param type
     */
    public IngestClientException(String message, Type type) {
        super(message);
        this.type = type;
    }

    /**
     * constructor
     *
     * @param message
     * @param type
     * @param th
     */
    public IngestClientException(String message, Type type, Throwable th) {
        super(message, th);
        this.type = type;
    }

    /**
     * get the IngestClientException type
     *
     * @return IngestClientException type
     */
    public Type getType() {
        return type;
    }
}
