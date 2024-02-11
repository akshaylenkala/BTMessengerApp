package com.example.androidbluetoothchat;
/** @noinspection ALL*/
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