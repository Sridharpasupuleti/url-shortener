package com.example.url_shortener.serviceimplementation;

public class Base62 {

    private static final String CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String encode(long number) {

        if (number == 0)
            return "0";

        StringBuilder sb = new StringBuilder();

        while (number > 0) {
            sb.append(CHARS.charAt((int)(number % 62)));
            number /= 62;
        }

        return sb.reverse().toString();
    }
}