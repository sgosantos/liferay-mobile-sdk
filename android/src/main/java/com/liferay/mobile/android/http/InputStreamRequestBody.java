/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.mobile.android.http;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.internal.Util;

import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * @author Silvio Santos
 */
public class InputStreamRequestBody {

	public static RequestBody create(
		final MediaType contentType, final InputStream inputStream) {

		if (inputStream == null) {
			throw new NullPointerException("inputStream == null");
		}
		else {
			return new RequestBody() {
				public MediaType contentType() {
					return contentType;
				}

				public long contentLength() {
					return -1;
				}

				public void writeTo(BufferedSink sink) throws IOException {
					Source source = null;

					try {
						source = Okio.source(inputStream);
						sink.writeAll(source);
					}
					finally {
						Util.closeQuietly(source);
					}
				}
			};
		}
	}

}