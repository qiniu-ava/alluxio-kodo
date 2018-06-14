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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.qiniu.util.StringUtils;

@JsonInclude(Include.NON_NULL)
public class QiniuObjectListing {
    public QiniuObjectMetadata[] items;
    public String marker = "";
    public String error = "";
    public String[] commonPrefixes;

    public ObjectListing toObjectListing(String bucket) {
        if (!error.equals("")) {
            return null;
        }

        String mMarker = "";
        if (marker != null && !marker.equals("null")) {
            mMarker = marker;
        }
        ObjectListing objectListing = new ObjectListing();
        if (commonPrefixes != null) {
            for (String prefix: commonPrefixes) {
                objectListing.addCommonPrefix(prefix);
            }
        }
        if (items != null) {
            for (QiniuObjectMetadata item: items) {
                OSSObjectSummary sm = item.toObjectSummary();
                sm.setBucketName(bucket);
                objectListing.addObjectSummary(sm);
            }
        }
        objectListing.setBucketName(bucket);
        objectListing.setTruncated(!mMarker.equals(""));
        objectListing.setEncodingType("");
        objectListing.setNextMarker(mMarker);
        objectListing.setMarker(mMarker);
        return objectListing;
    }

    @Override
    public String toString() {
        String[] itemStrs = new String[items.length];
        for (int i=0; i < items.length; i++) {
            itemStrs[i] = items[i].toString();
        }
        return String.format(
            "{ error: %s, items: %s, marker: %s, commonPrefixes: %s }",
            error,
            items == null ? "[]" : StringUtils.join(itemStrs, ",", null),
            marker,
            commonPrefixes == null ? "[]" : StringUtils.join(commonPrefixes, "", null)
        );
    }
}
