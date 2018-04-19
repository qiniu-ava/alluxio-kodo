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

package com.aliyun.oss.internal;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.naming.AuthenticationException;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.auth.ServiceSignature;
import com.aliyun.oss.common.comm.RequestMessage;
import com.qiniu.util.Auth;

public class QiniuRequestSigner implements RequestSigner {
    static public String DEFAULT_ORIGIN_DOMAIN = "";

    private String httpMethod;

    /* Note that resource path should not have been url-encoded. */
    private URI endPoint;
    private String bucket;
    private String key;
    private Credentials creds;

    public QiniuRequestSigner(String httpMethod, URI endPoint, String bucket, String key, Credentials creds) {
        this.httpMethod = httpMethod;
        this.endPoint = endPoint;
        this.bucket = bucket;
        this.key = key;
        this.creds = creds;
    }

    @Override
    public void sign(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
            // String url = String.format("http://", QiniuRequestSigner.DEFAULT_ORIGIN_DOMAIN, request.getAbsoluteUrl());
            Auth auth = Auth.create(this.creds.getAccessKeyId(), this.creds.getSecretAccessKey());
            try {
                URL originURL = new URL("http", this.endPoint.toString(), this.key);
                String privateUrl = auth.privateDownloadUrl(originURL.toString());
                URL url = new URL(privateUrl);
                url = new URL("http", QiniuRequestSigner.DEFAULT_ORIGIN_DOMAIN, url.getFile());
                System.out.println("private url with token");
                System.out.println(url);
                request.setAbsoluteUrl(url);
                request.addHeader(OSSHeaders.HOST, this.endPoint.toString());
            } catch (MalformedURLException err) {}
        }
    }
}
