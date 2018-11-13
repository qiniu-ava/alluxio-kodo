/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.utils;

import static com.aliyun.oss.internal.OSSUtils.COMMON_RESOURCE_MANAGER;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.internal.model.OSSErrorResult;

/**
 * A simple factory used for creating instances of <code>ClientException</code>
 * and <code>OSSException</code>.
 */
public class ExceptionFactory {

    public static ClientException createNetworkException(IOException ex) {
        String requestId = "Unknown";
        String errorCode = ClientErrorCode.UNKNOWN;

        if (ex instanceof SocketTimeoutException) {
            errorCode = ClientErrorCode.SOCKET_TIMEOUT;
        } else if (ex instanceof SocketException) {
            errorCode = ClientErrorCode.SOCKET_EXCEPTION;
        } else if (ex instanceof ConnectTimeoutException) {
            errorCode = ClientErrorCode.CONNECTION_TIMEOUT;
        } else if (ex instanceof UnknownHostException) {
            errorCode = ClientErrorCode.UNKNOWN_HOST;
        } else if (ex instanceof HttpHostConnectException) {
            errorCode = ClientErrorCode.CONNECTION_REFUSED;
        } else if (ex instanceof NoHttpResponseException) {
            errorCode = ClientErrorCode.CONNECTION_TIMEOUT;
        } else if (ex instanceof ClientProtocolException) {
            Throwable cause = ex.getCause();
            if (cause instanceof NonRepeatableRequestException) {
                errorCode = ClientErrorCode.NONREPEATABLE_REQUEST;
                return new ClientException(cause.getMessage(), errorCode, requestId, cause);
            }
        }

        return new ClientException(ex.getMessage(), errorCode, requestId, ex);
    }


    public static OSSException createInvalidResponseException(String requestId, int statusCode, String rawResponseError,
            Throwable cause) { 
        return createInvalidResponseException(requestId, statusCode, 
                COMMON_RESOURCE_MANAGER.getFormattedString("FailedToParseResponse", cause.getMessage()),
                rawResponseError);
    }

    public static OSSException createInvalidResponseException(String requestId, String message, int statusCode) {
        return createOSSException(requestId, OSSErrorCode.INVALID_RESPONSE, message, statusCode);
    }

    public static OSSException createInvalidResponseException(String requestId, int statusCode, String message,
            String rawResponseError) {
        return createOSSException(requestId, OSSErrorCode.INVALID_RESPONSE, message, rawResponseError, statusCode);
    }

    public static OSSException createOSSException(OSSErrorResult errorResult) {
        return createOSSException(errorResult, null);
    }

    public static OSSException createOSSException(OSSErrorResult errorResult, String rawResponseError) {
        return new OSSException(errorResult.Message, errorResult.StatusCode, errorResult.Code, errorResult.RequestId, errorResult.HostId,
                errorResult.Header, errorResult.ResourceType, errorResult.Method, rawResponseError);
    }

    public static OSSException createOSSException(String requestId, String errorCode, String message, int statusCode) {
        return new OSSException(message, statusCode, errorCode, requestId, null, null, null, null);
    }

    public static OSSException createOSSException(String requestId, String errorCode, String message,
            String rawResponseError, int statusCode) {
        return new OSSException(message, statusCode, errorCode, requestId, null, null, null, null, rawResponseError);
    }

    public static OSSException createUnknownOSSException(String requestId, int statusCode) {
        String message = "No body in response, http status code " + Integer.toString(statusCode);
        return new OSSException(message, statusCode, ClientErrorCode.UNKNOWN, requestId, null, null, null, null);
    }
}
