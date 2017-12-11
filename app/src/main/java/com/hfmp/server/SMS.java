package com.hfmp.server;

public class SMS {
    private String id;
    private String msg;
    private boolean sent;
    private String to;

    public SMS() {
    }

    public SMS(String id, String msg, boolean sent, String to) {
        this.id = id;
        this.msg = msg;
        this.sent = sent;
        this.to = to;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean getSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
