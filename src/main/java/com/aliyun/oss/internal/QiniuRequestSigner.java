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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.comm.QiniuCommand;
import com.aliyun.oss.common.comm.RequestMessage;
import com.qiniu.util.Auth;

public class QiniuRequestSigner implements RequestSigner {
    static private String DEFAULT_IO_DOMAIN = "iovip.qbox.me";
    static private String DEFAULT_RS_DOMAIN = "rs.qiniu.com";
    // static private String DEFAULT_RS_DOMAIN = "rs-z0.qiniu.com";
    static private String DEFAULT_RSF_DOMAIN = "rsf-z0.qiniu.com";
    static private String DEFAULT_UP_DOMAIN = "up.qiniu.com";
    static private String REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private QiniuCommand command;

    /* Note that resource path should not have been url-encoded. */
    private URI endPoint;
    private String bucket;
    private String key;
    private Credentials creds;

    public QiniuRequestSigner(QiniuCommand command, URI endPoint, String bucket, String key, Credentials creds) {
        this.command = command;
        this.endPoint = endPoint;
        this.bucket = bucket;
        this.key = key;
        this.creds = creds;
    }

    @Override
    public void sign(RequestMessage request) throws ClientException {
        switch (command) {
            case GET_OBJ_DATA:
                signGetObject(request);
                break;
            case GET_OBJ_META:
                signGetMeta(request);
                break;
            case LIST_OBJ:
                signListObj(request);
                break;
            case POST_OBJ:
                signPostObj(request);
                break;
            case DELETE_OBJ:
                signDeleteObj(request);
                break;
        }
    }

    private void signGetObject(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
            Auth auth = Auth.create(this.creds.getAccessKeyId(), this.creds.getSecretAccessKey());
            try {
                URL originURL = new URL("http", this.endPoint.toString(), this.key);
                String privateUrl = auth.privateDownloadUrl(originURL.toString());
                URL url = new URL(privateUrl);
                url = new URL("http", QiniuRequestSigner.DEFAULT_IO_DOMAIN, url.getFile());
                request.setAbsoluteUrl(url);
                request.addHeader(OSSHeaders.HOST, this.endPoint.toString());
            } catch (MalformedURLException err) {}
        }
    }

    private void signGetMeta(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();
        String bucket = request.getBucket();
        String key = request.getKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
            Auth auth = Auth.create(this.creds.getAccessKeyId(), this.creds.getSecretAccessKey());
            try {
                String path = "stat/" + SignUtils.encodeEntryURI(bucket, key);
                URL url = new URL("http://" + QiniuRequestSigner.DEFAULT_RS_DOMAIN + "/" + path);
                String token = auth.signRequest(url.toString(), null, REQUEST_CONTENT_TYPE);
                request.setAbsoluteUrl(url);
                Map<String, String> headers = new HashMap<String, String>();
                headers.put(OSSHeaders.AUTHORIZATION, "QBox " + token);
                request.setHeaders(headers);
                request.setParameters(null);
                request.setEndpoint(new URI("http://" + QiniuRequestSigner.DEFAULT_RS_DOMAIN));
                request.setResourcePath(path);
                request.setContent(null);
                request.setMethod(HttpMethod.GET);
            } catch (MalformedURLException err) {
            } catch (URISyntaxException err) {}
        }
    }

    private void signListObj(RequestMessage request) throws ClientException {
        try {
            String accessKeyId = creds.getAccessKeyId();
            String secretAccessKey = creds.getSecretAccessKey();
            String bucket = Objects.toString(request.getBucket(), "");
            String prefix = URLEncoder.encode(Objects.toString(request.getKey(), ""), "UTF-8");
            String marker = Objects.toString(request.getParameters().get("marker"), "");
            String limit = Objects.toString(request.getParameters().get("limit"), "");
            String delimiter = URLEncoder.encode(Objects.toString(request.getParameters().get("delimiter"), ""), "UTF-8");

            if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
                try {
                    Auth auth = Auth.create(this.creds.getAccessKeyId(), this.creds.getSecretAccessKey());
                    String path = String.format(
                        "list?bucket=%s&prefix=%s&limit=%s&marker=%s&delimiter=%s",
                        bucket,
                        prefix,
                        limit,
                        marker,
                        delimiter
                    );
                    URL url = new URL("http://" + DEFAULT_RSF_DOMAIN + "/" + path);
                    String token = auth.signRequest(url.toString(), null, REQUEST_CONTENT_TYPE);
                    request.setAbsoluteUrl(url);
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put(OSSHeaders.AUTHORIZATION, "QBox " + token);
                    headers.put(OSSHeaders.CONTENT_TYPE, REQUEST_CONTENT_TYPE);
                    request.setHeaders(headers);
                    request.setParameters(null);
                    request.setEndpoint(new URI("http://" + DEFAULT_RSF_DOMAIN));
                    request.setResourcePath(path);
                    request.setContent(null);
                    request.setMethod(HttpMethod.GET);
                } catch (MalformedURLException err) {
                } catch (URISyntaxException err) {}
            }
        } catch (UnsupportedEncodingException err) {}
    }

    private void signPostObj(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
        }
    }

    private void signDeleteObj(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
        }
    }
}
