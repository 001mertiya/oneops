package com.oneops.ecv.auth;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthUtilTest {


    private static String SECRET = "secret";
    private static String USER = "user";

    @Test
    public void testAuthenticate() throws Exception {
        AuthUtil authUtil = new AuthUtil();
        authUtil.setSecret(SECRET);
        authUtil.setUser(USER);
        String authHeader = "Basic ";
        String userSecret = USER + ":" + SECRET;
        String authString = String.valueOf(Base64.encodeBase64URLSafeString(userSecret.getBytes()));
        Assert.assertTrue(authUtil.authenticate(authHeader + authString));
    }

    @Test
    public void testAuthenticateInvalidCred() throws Exception {
        AuthUtil authUtil = new AuthUtil();
        authUtil.setSecret(SECRET);
        authUtil.setUser(USER);
        String authHeader = "Basic ";
        String userSecret = USER + ":" + SECRET + "1";
        String authString = String.valueOf(Base64.encodeBase64URLSafeString(userSecret.getBytes()));
        Assert.assertFalse(authUtil.authenticate(authHeader + authString));
    }


}