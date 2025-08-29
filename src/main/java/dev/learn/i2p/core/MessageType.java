package dev.learn.i2p.core;
public enum MessageType { TEXT(1), IMAGE(2);
    public final int code; MessageType(int c){ this.code=c; } }
