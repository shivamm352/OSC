package com.springosc.user.common;

import java.util.Random;

public class Helper {

    private static final Random random = new Random();

    public static long generateOTP(){
        return 100000L + random.nextLong(900000L);
    }

    public static String generateUserId(String userId) {
        String prefix = userId.substring(0, Math.min(userId.length(), 3)).toLowerCase().replace(" ", "");
        int randomDigits = 100 + random.nextInt(900);
        return prefix + randomDigits;
    }

}
