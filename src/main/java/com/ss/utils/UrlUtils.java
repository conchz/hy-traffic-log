package com.ss.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * url and html utils.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class UrlUtils {

    private static final String HTTP_PREFIX = "http://";
    private static final String WWW = "www";

    /**
     * canonicalizeUrl
     * <p>
     * Borrowed from Jsoup.
     *
     * @param url
     * @param refer
     * @return canonicalizeUrl
     */
    public static String canonicalizeUrl(String url, String refer) {
        URL base;
        try {
            try {
                base = new URL(refer);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute may be abs on its own, so try that
                URL abs = new URL(refer);
                return abs.toExternalForm();
            }
            // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
            if (url.startsWith("?"))
                url = base.getPath() + url;
            URL abs = new URL(base, url);
            return encodeIllegalCharacterInUrl(abs.toExternalForm());
        } catch (MalformedURLException e) {
            return "";
        }
    }

    /**
     * @param url
     * @return
     */
    public static String encodeIllegalCharacterInUrl(String url) {
        //TODO more charator support
        return url.replace(" ", "%20");
    }

    public static String getHost(String url) {
        String host = url;
        int i = StringUtils.ordinalIndexOf(url, "/", 3);
        if (i > 0) {
            host = StringUtils.substring(url, 0, i);
        }
        return host;
    }

    private static Pattern patternForProtocal = Pattern.compile("[\\w]+://");

    public static String removeProtocol(String url) {
        return patternForProtocal.matcher(url).replaceAll("");
    }

    public static String getDomain(String url) {
        String domain = removeProtocol(url);
        int i = StringUtils.indexOf(domain, "/", 1);
        if (i > 0) {
            domain = StringUtils.substring(domain, 0, i);
        }
        return domain;
    }

    /**
     * allow blank space in quote
     */
    private static Pattern patternForHrefWithQuote = Pattern.compile("(<a[^<>]*href=)[\"']([^\"'<>]*)[\"']", Pattern.CASE_INSENSITIVE);

    /**
     * disallow blank space without quote
     */
    private static Pattern patternForHrefWithoutQuote = Pattern.compile("(<a[^<>]*href=)([^\"'<>\\s]+)", Pattern.CASE_INSENSITIVE);

    public static String fixAllRelativeHrefs(String html, String url) {
        html = replaceByPattern(html, url, patternForHrefWithQuote);
        html = replaceByPattern(html, url, patternForHrefWithoutQuote);
        return html;
    }

    public static String replaceByPattern(String html, String url, Pattern pattern) {
        StringBuilder stringBuilder = new StringBuilder();
        Matcher matcher = pattern.matcher(html);
        int lastEnd = 0;
        boolean modified = false;
        while (matcher.find()) {
            modified = true;
            stringBuilder.append(StringUtils.substring(html, lastEnd, matcher.start()));
            stringBuilder.append(matcher.group(1));
            stringBuilder.append("\"").append(canonicalizeUrl(matcher.group(2), url)).append("\"");
            lastEnd = matcher.end();
        }
        if (!modified) {
            return html;
        }
        stringBuilder.append(StringUtils.substring(html, lastEnd));
        return stringBuilder.toString();
    }

    private static final Pattern patternForCharset = Pattern.compile("charset\\s*=\\s*['\"]*([^\\s;'\"]*)");

    public static String getCharset(String contentType) {
        Matcher matcher = patternForCharset.matcher(contentType);
        if (matcher.find()) {
            String charset = matcher.group(1);
            if (Charset.isSupported(charset)) {
                return charset;
            }
        }
        return null;
    }

    /**
     * @param orgUrl 原始站点URL
     * @param desUrl 实际URL
     * @return
     */
    public static boolean match(String orgUrl, String desUrl) {
        boolean isMatch = false;

        if (!orgUrl.contains(HTTP_PREFIX)) {
            orgUrl = HTTP_PREFIX + orgUrl;
        }

        if (!desUrl.contains(HTTP_PREFIX)) {
            desUrl = HTTP_PREFIX + desUrl;
        }

        try {
            URL _url1 = new URL(orgUrl);
            URL _url2 = new URL(desUrl);

            String domain1 = _url1.getHost();
            String domain2 = _url2.getHost();

            if (orgUrl.contains(WWW)) {
                if (desUrl.contains(WWW)) {
                    /**
                     * domain1: www.best-ad.cn
                     * domain2: www.best-ad.cn
                     */
                    if (domain1.equals(domain2)) {
                        isMatch = true;
                    }
                } else {
                    /**
                     * domain1: www.best-ad.cn
                     * domain2: sem.best-ad.cn
                     */
                    if (domain2.contains(domain1.replace(WWW, ""))) {
                        isMatch = true;
                    }
                }

            } else {
                if (desUrl.contains(WWW)) {
                    /**
                     * domain1: best-ad.cn
                     * domain2: www.best-ad.cn
                     */
                    if (domain2.replace(WWW, "").contains(domain1)) {
                        isMatch = true;
                    }
                } else {
                    /**
                     * domain1: hy.best-ad.cn
                     * domain2: api.hy.best-ad.cn
                     */
                    if (domain2.contains(domain1)) {
                        isMatch = true;
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return isMatch;
    }

}
