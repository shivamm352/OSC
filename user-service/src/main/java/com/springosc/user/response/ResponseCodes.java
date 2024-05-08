package com.springosc.user.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseCodes {

    public static final int SUCCESS = 200;
    public static final int EMAIL_EXISTS = 30;
    public static final int USER_DATA_SENT = 200;
    public static final int USER_DATA_NOT_SENT = 220;

    public static final int VALID_OTP = 500;
    public static final int INVALID_USER_ID = 1999;
    public static final int INVALID_ATTEMPTS_1_2 = 502;
    public static final int INVALID_ATTEMPTS_3_OR_MORE = 301;
    public static final int UNKNOWN_ERROR = 0;

    public static final int SUCCESSFULLY_SAVED = 200;

    public static final int INVALID_USERID = 201;
    public static final int INVALID_PASSWORD = 202;
    public static final int SESSION_EXISTS = 204;
    public static final int LOGGED_IN = 200;


    public static final int LOGGED_OUT = 200;

    public static final int EMAIL_NOT_SENT = 199;

    public static final int EMAIL_ALREADY_EXISTS = 1999;
    public static final int INVALID_OTP_ATTEMPTS_1_2 = 199;


}
