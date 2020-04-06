package com.abstractx1.sensortest;

public class HttpResponse {
    private int code;
    private String body;

    public HttpResponse(int _code, String _body) {
        this.code = _code;
        this.body = _body;
    }

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }
}
