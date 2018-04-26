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

package com.aliyun.oss.model;

import java.util.Date;

import com.aliyun.oss.internal.OSSHeaders;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public final class QiniuObjectMetadata {
    public String error = "";
    public String key = "";
    public String hash = "";
    public long fsize = 0;
    public long putTime = 0;
    public String mimeType = "";
    public int type = 0;

    public ObjectMetadata toObjectMetadata() {
        if (!error.equals("")) {
            return null;
        }

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setHeader("key", key);
        objectMetadata.setHeader(OSSHeaders.CONTENT_LENGTH, fsize);
        objectMetadata.setHeader(OSSHeaders.LAST_MODIFIED, new Date(putTime));
        objectMetadata.setHeader(OSSHeaders.ETAG, hash);
        return objectMetadata;
    }

    public OSSObjectSummary toObjectSummary() {
        if (!error.equals("")) {
            return null;
        }

        OSSObjectSummary objectSummary = new OSSObjectSummary();
        objectSummary.setKey(key);
        objectSummary.setSize(fsize);
        objectSummary.setLastModified(new Date(putTime));
        objectSummary.setETag(hash);
        return objectSummary;
    }

    @Override
    public String toString() {
        return String.format(
            "{key: %s, hash: %s, fsize: %s, putTime: %s, mimeType: %s, type: %s}",
            key,
            hash,
            String.valueOf(fsize),
            String.valueOf(putTime),
            mimeType,
            String.valueOf(type)
        );
    }
}
