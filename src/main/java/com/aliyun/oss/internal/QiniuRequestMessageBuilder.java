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

import static com.aliyun.oss.common.utils.CodingUtils.assertParameterInRange;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_FILE_SIZE_LIMIT;
import static com.aliyun.oss.internal.OSSUtils.determineFinalEndpoint;
import static com.aliyun.oss.internal.OSSUtils.determineResourcePath;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.common.comm.QiniuCommand;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.comm.io.FixedLengthInputStream;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.WebServiceRequest;

/*
 * HTTP request message builder.
 */
public class QiniuRequestMessageBuilder {

    private URI endpoint;
    private QiniuCommand command = QiniuCommand.GET_OBJ_DATA;
    private String bucket;
    private String key;
    private Map<String, String> headers = new HashMap<String, String>();
    private Map<String, String> parameters = new LinkedHashMap<String, String>();
    private InputStream inputStream;
    private long inputSize = 0;
    private ServiceClient innerClient;
    private boolean useChunkEncoding = false;

    private WebServiceRequest originalRequest;

    public QiniuRequestMessageBuilder(ServiceClient innerClient) {
        this.innerClient = innerClient;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public QiniuRequestMessageBuilder setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public QiniuCommand getCommand() {
        return command;
    }

    public QiniuRequestMessageBuilder setCommand(QiniuCommand command) {
        this.command = command;
        return this;
    }

    public String getBucket() {
        return bucket;
    }

    public QiniuRequestMessageBuilder setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public String getKey() {
        return key;
    }

    public QiniuRequestMessageBuilder setKey(String key) {
        this.key = key;
        return this;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public QiniuRequestMessageBuilder setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public QiniuRequestMessageBuilder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public QiniuRequestMessageBuilder setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }

    public QiniuRequestMessageBuilder addParameter(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public QiniuRequestMessageBuilder setInputStream(InputStream instream) {
        this.inputStream = instream;
        return this;
    }

    public QiniuRequestMessageBuilder setInputStreamWithLength(FixedLengthInputStream instream) {
        assertParameterInRange(inputSize, -1, DEFAULT_FILE_SIZE_LIMIT);
        this.inputStream = instream;
        this.inputSize = instream.getLength();
        return this;
    }

    public long getInputSize() {
        return inputSize;
    }

    public QiniuRequestMessageBuilder setInputSize(long inputSize) {
        assertParameterInRange(inputSize, -1, DEFAULT_FILE_SIZE_LIMIT);
        this.inputSize = inputSize;
        return this;
    }

    public boolean isUseChunkEncoding() {
        return useChunkEncoding;
    }

    public QiniuRequestMessageBuilder setUseChunkEncoding(boolean useChunkEncoding) {
        this.useChunkEncoding = useChunkEncoding;
        return this;
    }

    public QiniuRequestMessageBuilder setOriginalRequest(WebServiceRequest originalRequest) {
        this.originalRequest = originalRequest;
        return this;
    }

    public RequestMessage build() {
        Map<String, String> sentHeaders = new HashMap<String, String>(this.headers);
        sentHeaders.put(OSSHeaders.DATE, DateUtil.formatRfc822Date(new Date()));
        Map<String, String> sentParameters = new LinkedHashMap<String, String>(this.parameters);

        RequestMessage request = new RequestMessage(this.originalRequest, this.bucket, this.key);
        ClientConfiguration clientCofig = this.innerClient.getClientConfiguration();
        request.setBucket(bucket);
        request.setKey(key);
        request.setEndpoint(determineFinalEndpoint(this.endpoint, this.bucket, clientCofig));
        request.setResourcePath(determineResourcePath(this.bucket, this.key, clientCofig.isSLDEnabled()));
        request.setHeaders(sentHeaders);
        request.setParameters(sentParameters);
        request.setCommand(this.command);
        request.setContent(this.inputStream);
        request.setContentLength(this.inputSize);
        request.setUseChunkEncoding(this.inputSize == -1 ? true : this.useChunkEncoding);

        return request;
    }

}
