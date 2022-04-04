package com.riba2reality.exeterlocatecore.DataStores;

public class ServerMessage {

    //public String urlString;

    public String message;

    //public String address;

    //public Boolean useSSL;

    public enum MessageType{
        LOCATION,
        WIFI,
        BLUETOOTH,
        ACCEL,
        MAG,
        COMBINED,
        User

    }

    public MessageType messageType;


}//end of class
