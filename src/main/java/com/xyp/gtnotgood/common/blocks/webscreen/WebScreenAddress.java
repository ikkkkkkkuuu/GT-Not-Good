package com.xyp.gtnotgood.common.blocks.webscreen;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class WebScreenAddress {

    public static final String DEFAULT_HOME = "https://www.bilibili.com";
    public static final int MAX_LENGTH = 2048;

    private WebScreenAddress() {}

    public static String normalize(String address) {
        if (address == null) throw new IllegalArgumentException("Missing web address");
        String value = address.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) throw new IllegalArgumentException("Invalid web address");
        int schemeEnd = value.indexOf("://");
        if (schemeEnd < 0 || !value.substring(0, schemeEnd).matches("[A-Za-z][A-Za-z0-9+.-]*")) {
            value = "https://" + value;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (
                !("https".equals(scheme) || "http".equals(scheme)) || host == null
                    || host.isEmpty()
                    || uri.getRawUserInfo() != null
                    || uri.getPort() < -1
                    || uri.getPort() == 0
                    || uri.getPort() > 65535
            ) throw new IllegalArgumentException("Only HTTP and HTTPS addresses without credentials are supported");
            StringBuilder normalized = new StringBuilder(scheme).append("://").append(host.toLowerCase(Locale.ROOT));
            if (uri.getPort() != -1) normalized.append(':').append(uri.getPort());
            if (uri.getRawPath() != null) normalized.append(uri.getRawPath());
            if (uri.getRawQuery() != null) normalized.append('?').append(uri.getRawQuery());
            if (uri.getRawFragment() != null) normalized.append('#').append(uri.getRawFragment());
            if (normalized.length() > MAX_LENGTH) throw new IllegalArgumentException("Web address is too long");
            return normalized.toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid web address", exception);
        }
    }
}
