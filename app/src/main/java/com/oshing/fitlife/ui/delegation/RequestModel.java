package com.oshing.fitlife.ui.delegation;

public class RequestModel {

    private String fromUid;
    private String fromName;
    private String fromEmail;
    private long sentAt;

    public RequestModel() {}

    public String getFromUid() { return fromUid; }
    public String getFromName() { return fromName; }
    public String getFromEmail() { return fromEmail; }

    public void setFromUid(String fromUid) { this.fromUid = fromUid; }
}
