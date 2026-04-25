package com.example.androidnativegrupo5.data.model;

public class LoginRequest {

    private String usernameOrEmail;
    private String password;

    public LoginRequest(String usernameOrEmail, String password)
    {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }
}
