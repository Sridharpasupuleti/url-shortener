package com.example.url_shortener.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.url_shortener.config.RateLimitProperties;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private final RateLimitProperties properties;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
        validateTrustedProxies(properties.getTrustedProxies());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddress;
        }

        List<String> forwardedAddresses = parseForwardedAddresses(forwardedFor);
        if (forwardedAddresses.isEmpty()) {
            return remoteAddress;
        }

        for (int index = forwardedAddresses.size() - 1; index >= 0; index--) {
            String address = forwardedAddresses.get(index);
            if (!isTrustedProxy(address)) {
                return address;
            }
        }
        return forwardedAddresses.get(0);
    }

    private List<String> parseForwardedAddresses(String forwardedFor) {
        List<String> addresses = new ArrayList<>();
        for (String value : forwardedFor.split(",")) {
            try {
                addresses.add(normalizeAddress(value.trim()));
            } catch (IllegalArgumentException exception) {
                return List.of();
            }
        }
        return addresses;
    }

    private boolean isTrustedProxy(String address) {
        for (String trustedProxy : properties.getTrustedProxies()) {
            if (trustedProxy != null && !trustedProxy.isBlank()
                    && matches(address, trustedProxy.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String address, String configuredProxy) {
        String[] parts = configuredProxy.split("/", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("Invalid trusted proxy: " + configuredProxy);
        }
        byte[] candidate = parseAddress(address);
        byte[] network = parseAddress(parts[0]);
        if (candidate.length != network.length) {
            return false;
        }

        int prefixLength = parts.length == 2
                ? parsePrefixLength(parts[1], network.length * 8)
                : network.length * 8;
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int index = 0; index < fullBytes; index++) {
            if (candidate[index] != network[index]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
    }

    private int parsePrefixLength(String prefix, int maximum) {
        try {
            int length = Integer.parseInt(prefix);
            if (length < 0 || length > maximum) {
                throw new IllegalArgumentException("Invalid trusted proxy prefix length: " + prefix);
            }
            return length;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid trusted proxy prefix length: " + prefix, exception);
        }
    }

    private byte[] parseAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Invalid IP address: " + address);
        }
        if (address.indexOf(':') >= 0) {
            if (!address.matches("[0-9a-fA-F:.]+")) {
                throw new IllegalArgumentException("Invalid IP address: " + address);
            }
            try {
                return InetAddress.getByName(address).getAddress();
            } catch (UnknownHostException exception) {
                throw new IllegalArgumentException("Invalid IP address: " + address, exception);
            }
        }

        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IP address: " + address);
        }
        byte[] parsedAddress = new byte[4];
        for (int index = 0; index < octets.length; index++) {
            if (!octets[index].matches("\\d{1,3}")) {
                throw new IllegalArgumentException("Invalid IP address: " + address);
            }
            int octet = Integer.parseInt(octets[index]);
            if (octet > 255) {
                throw new IllegalArgumentException("Invalid IP address: " + address);
            }
            parsedAddress[index] = (byte) octet;
        }
        return parsedAddress;
    }

    private String normalizeAddress(String address) {
        try {
            return InetAddress.getByAddress(parseAddress(address)).getHostAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid IP address: " + address, exception);
        }
    }

    private void validateTrustedProxies(List<String> trustedProxies) {
        for (String trustedProxy : trustedProxies) {
            if (trustedProxy != null && !trustedProxy.isBlank()) {
                matches(trustedProxy.split("/", -1)[0], trustedProxy.trim());
            }
        }
    }
}
