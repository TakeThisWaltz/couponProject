package kr.azazel.barcode.service;

import com.azazel.framework.util.LOG;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ji on 2016. 10. 24..
 */

public class PhoneNumberParser {
    private static final String TAG = "PhoneNumberParser";

    private static final String NUM_ZERO = "0Oo공영ㅇ";
    private static final String NUM_ONE = "1일ㅣ|Il";
    private static final String NUM_MID = "5오7칠";
    private static final String NUM_TEL = "6육륙7칠9구";
    private static final String NUM_OTHER = "234689이둘삼사육륙팔구";

    private static final String NUM_STR = NUM_ZERO + NUM_ONE + NUM_MID + NUM_OTHER;

    private static final Map<Character, Character> NUMBER_CONVERTER = new HashMap<Character, Character>();
    static{
        NUMBER_CONVERTER.put('0', '0');
        NUMBER_CONVERTER.put('O', '0');
        NUMBER_CONVERTER.put('o', '0');
        NUMBER_CONVERTER.put('공', '0');
        NUMBER_CONVERTER.put('영', '0');
        NUMBER_CONVERTER.put('ㅇ', '0');

        NUMBER_CONVERTER.put('1', '1');
        NUMBER_CONVERTER.put('일', '1');
        NUMBER_CONVERTER.put('ㅣ', '1');
        NUMBER_CONVERTER.put('|', '1');
        NUMBER_CONVERTER.put('I', '1');
        NUMBER_CONVERTER.put('l', '1');

        NUMBER_CONVERTER.put('2', '2');
        NUMBER_CONVERTER.put('이', '2');
        NUMBER_CONVERTER.put('둘', '2');

        NUMBER_CONVERTER.put('3', '3');
        NUMBER_CONVERTER.put('삼', '3');

        NUMBER_CONVERTER.put('4', '4');
        NUMBER_CONVERTER.put('사', '4');

        NUMBER_CONVERTER.put('5', '5');
        NUMBER_CONVERTER.put('오', '5');

        NUMBER_CONVERTER.put('6', '6');
        NUMBER_CONVERTER.put('육', '6');
        NUMBER_CONVERTER.put('륙', '6');

        NUMBER_CONVERTER.put('7', '7');
        NUMBER_CONVERTER.put('칠', '7');

        NUMBER_CONVERTER.put('8', '8');
        NUMBER_CONVERTER.put('팔', '8');

        NUMBER_CONVERTER.put('9', '9');
        NUMBER_CONVERTER.put('구', '9');

    }

    private static final String REG_EXP = "["+NUM_ZERO+"][(&nbsp;)\\s]*["+NUM_ONE + NUM_MID+"][(&nbsp;)\\s]*["+NUM_ZERO+NUM_ONE+NUM_TEL+"]" +
            "[(&nbsp;)\\.\\s-]*["+NUM_STR+"]?[(&nbsp;)\\s-]*["+NUM_STR+"][(&nbsp;)\\s]*["+NUM_STR+"][(&nbsp;)\\s]*["+NUM_STR+"]" +
            "[(&nbsp;)\\.\\s-]*["+NUM_STR+"][(&nbsp;)\\s]*["+NUM_STR+"][(&nbsp;)\\s]*["+NUM_STR+"][(&nbsp;)\\s]*["+NUM_STR+"]";

    public static String searchPhoneNumber(String source){



        Pattern pattern = Pattern.compile(REG_EXP);
        Matcher match = pattern.matcher(source);

        if(match.find()){
            String num = match.group();

            LOG.d(TAG, "parsed - num : " + num);

            if(num != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < num.length(); i++) {
                    char ch = num.charAt(i);
                    if(NUMBER_CONVERTER.containsKey(ch)){
                        sb.append(NUMBER_CONVERTER.get(ch));
                    }
                }

                return sb.toString();
            }

        }else {
            LOG.d(TAG, "not found");
        }

        return null;
    }


}
