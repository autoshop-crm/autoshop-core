package com.vladko.autoshopcore.security;

public interface AuthServiceClient {

    AuthenticatedUser validateAccessToken(String accessToken);
}
