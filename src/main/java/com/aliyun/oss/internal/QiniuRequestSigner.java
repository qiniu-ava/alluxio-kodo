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
import com.aliyun.oss.common.utils.LogUtils;
import com.qiniu.util.Auth;
import com.qiniu.util.UrlSafeBase64;
//import static com.aliyun.oss.common.utils.LogUtils.getLog;

public class QiniuRequestSigner implements RequestSigner {
    static private String DEFAULT_IO_DOMAIN = "";
    static private String DEFAULT_RS_DOMAIN = "rs.qiniu.com";
    // static private String DEFAULT_RS_DOMAIN = "rs-z0.qiniu.com";
    static private String DEFAULT_RSF_DOMAIN = "rsf-z0.qiniu.com";
    static private String DEFAULT_UP_DOMAIN = "up.qiniu.com";
    static private String REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";
    static private String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    static public String UPLOAD_FILE_SIZE = "upload-file-size";

    static private int UPLOAD_TOKEN_DEUFAULT_EXPIRE_TIME = 3600 * 10;
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

        if (QiniuRequestSigner.DEFAULT_IO_DOMAIN == null) {
            if (System.getenv("KODO_ORIGHOST") == null || System.getenv("KODO_ORIGHOST").equals("")) {
                QiniuRequestSigner.DEFAULT_IO_DOMAIN = "iovip.qbox.me";
            } else {
                QiniuRequestSigner.DEFAULT_IO_DOMAIN = System.getenv("KODO_ORIGHOST");
            }
        }
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
            case MAKE_BLOCK:
                signMakeBlock(request);
                break;
            case MAKE_FILE:
                signMakeFile(request);
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
                URL originURL = new URL("http://" + this.endPoint.getHost() + "/" + this.key);
                String privateUrl = auth.privateDownloadUrl(originURL.toString());
                URL url = new URL(privateUrl);
                url = new URL("http", QiniuRequestSigner.DEFAULT_IO_DOMAIN, url.getFile());
                request.setAbsoluteUrl(url);
                Map<String, String> headers = new HashMap<String, String>();
                headers.put(OSSHeaders.HOST, this.endPoint.getHost());
                request.setHeaders(headers);
                request.setParameters(null);
                request.setEndpoint(new URI("http://" + this.endPoint.getHost()));
                request.setResourcePath(url.getFile());
                request.setContent(null);
                request.setMethod(HttpMethod.GET);
            } catch (MalformedURLException err) {
                LogUtils.getLog().warn("load file from " + this.key + " failed, error: " + err.getMessage() + ", " + err.toString());
            } catch (URISyntaxException err) {
                LogUtils.getLog().warn("load file from " + this.key + " failed, error: " + err.getMessage() + ", " + err.toString());
            }
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

    private void signMakeBlock(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();
        String bucket = request.getBucket();
        String key = request.getKey();
        long blockSize = request.getContentLength();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
            Auth auth = Auth.create(accessKeyId, secretAccessKey);
            String token = auth.uploadToken(bucket, key, UPLOAD_TOKEN_DEUFAULT_EXPIRE_TIME, null, true);

            try {
                String path = "mkblk/" + String.valueOf(blockSize);
                URL url = new URL("http://" + DEFAULT_UP_DOMAIN + "/" + path);
                request.setAbsoluteUrl(url);
                request.setEndpoint(new URI("http://" + DEFAULT_UP_DOMAIN));
                request.setResourcePath(path);

                Map<String, String> headers = new HashMap<String, String>();
                headers.put(OSSHeaders.AUTHORIZATION, "UpToken " + token);
                headers.put(OSSHeaders.CONTENT_TYPE, OCTET_STREAM_CONTENT_TYPE);
                request.setHeaders(headers);
                request.setParameters(null);
                request.setMethod(HttpMethod.POST);
            } catch (MalformedURLException e) {
            } catch (URISyntaxException e) {
            }
        }
    }

    private void signMakeFile(RequestMessage request) throws ClientException {
        String bucket = request.getBucket();
        String key = request.getKey();
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();
        String fileSizeStr = Objects.toString(request.getHeaders().get(UPLOAD_FILE_SIZE), "-1");
        long fileSize = Long.parseLong(fileSizeStr);
        if (fileSize == -1) {
            throw new ClientException("failed to upload file, file size is empty");
        }

        String path = String.format("mkfile/%s/key/%s/mimeType/%s/",
            String.valueOf(fileSize),
            UrlSafeBase64.encodeToString(key),
            UrlSafeBase64.encodeToString(OCTET_STREAM_CONTENT_TYPE)
        );

        Auth auth = Auth.create(accessKeyId, secretAccessKey);
        String token = auth.uploadToken(bucket, key, UPLOAD_TOKEN_DEUFAULT_EXPIRE_TIME, null, true);

        Map<String, String> oldHeaders = request.getHeaders();
        String contentLength = oldHeaders.get(OSSHeaders.CONTENT_LENGTH);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(OSSHeaders.CONTENT_TYPE, "text/plain");
        headers.put(OSSHeaders.AUTHORIZATION, "UpToken " + token);
        if (contentLength == null || contentLength.equals("")) {
            contentLength = String.valueOf(request.getContentLength());
        }
        headers.put(OSSHeaders.CONTENT_LENGTH, contentLength);
        request.setHeaders(headers);

        try {
            URL url = new URL("http://" + DEFAULT_UP_DOMAIN + "/" + path);
            request.setAbsoluteUrl(url);
            request.setEndpoint(new URI("http://" + DEFAULT_UP_DOMAIN));
            request.setResourcePath(path);
            request.setMethod(HttpMethod.POST);
        } catch (MalformedURLException e) {
        } catch (URISyntaxException e) {}
    }

    private void signDeleteObj(RequestMessage request) throws ClientException {
        String accessKeyId = creds.getAccessKeyId();
        String secretAccessKey = creds.getSecretAccessKey();

        if (accessKeyId.length() > 0 && secretAccessKey.length() > 0) {
        }
    }
}
