package com.example.androidbluetoothchat;

// Create a new class MessageData.java
public class MessageData {
    private byte[] messageBytes;
    private String timestamp;
    private String senderReceiver;

    public MessageData(byte[] messageBytes,String senderReceiver,String timestamp) {
        this.senderReceiver=senderReceiver;
        this.messageBytes = messageBytes;
        this.timestamp = timestamp;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public String getSenderReceiver(){
        return senderReceiver;
    }
}