package com.ss.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dolphineor on 2015-7-23.
 */
public class GarbledCodeParser {

    private static final Pattern PATTERN = Pattern.compile("\\s*|t*|r*|n*");


    /**
     * 判断字符是否是中文
     *
     * @param c 字符
     * @return 是否是中文
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    /**
     * 判断字符串是否是乱码
     *
     * @param strName 字符串
     * @return 是否是乱码
     */
    public static boolean isGarbledCode(String strName) {
        Matcher matcher = PATTERN.matcher(strName);
        String after = matcher.replaceAll("");
        String tmp = after.replaceAll("\\p{P}", "");
        char[] ch = tmp.trim().toCharArray();
        int chLength = ch.length;
        float count = 0;
        for (char c : ch) {
            if (!Character.isLetterOrDigit(c)) {
                if (!isChinese(c)) {
                    count += 1;
                }
            }
        }

        return count / chLength > 0;

    }

}
