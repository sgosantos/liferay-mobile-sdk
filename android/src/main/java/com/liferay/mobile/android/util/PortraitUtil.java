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

package com.liferay.mobile.android.util;

import android.util.Base64;
import android.util.Log;

import com.liferay.mobile.android.http.HttpHeader;
import com.liferay.mobile.android.http.HttpStatus;
import com.liferay.mobile.android.http.HttpUtil;
import com.liferay.mobile.android.service.Session;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URLEncoder;

import java.security.MessageDigest;

/**
 * @author Bruno Farache
 * @author Silvio Santos
 */
public class PortraitUtil {

	public static String downloadPortrait(
			Session session, String portraitURL, OutputStream os)
		throws Exception {

		return downloadPortrait(session, portraitURL, os, null);
	}

	public static String downloadPortrait(
			Session session, String portraitURL, OutputStream os,
			String modifiedDate)
		throws Exception {

		String lastModified = null;
		InputStream is = null;

		try {
			Request.Builder builder = new Request.Builder()
				.get()
				.url(portraitURL);

			if (Validator.isNotNull(modifiedDate)) {
				builder.addHeader(HttpHeader.IF_MODIFIED_SINCE, modifiedDate);
			}

			OkHttpClient client = HttpUtil.getOkHttpClient(session);
			Response response = client.newCall(builder.build()).execute();

			int status = response.code();

			if (status == HttpStatus.OK) {
				is = response.body().byteStream();

				int count;
				byte data[] = new byte[8192];

				while ((count = is.read(data)) != -1) {
					os.write(data, 0, count);
				}

				lastModified = response.header(HttpHeader.LAST_MODIFIED);
			}
		}
		catch (Exception e) {
			Log.e(_CLASS_NAME, "Couldn't download portrait", e);

			throw e;
		}
		finally {
			close(is);
			close(os);
		}

		return lastModified;
	}

	public static String downloadPortrait(
			Session session, String portraitURL, String filePath)
		throws Exception {

		return downloadPortrait(session, portraitURL, filePath, null);
	}

	public static String downloadPortrait(
			Session session, String portraitURL, String filePath,
			String modifiedDate)
		throws Exception {

		return downloadPortrait(
			session, portraitURL, new FileOutputStream(filePath), modifiedDate);
	}

	public static String getPortraitURL(
		Session session, boolean male, long portraitId, String uuid) {

		StringBuilder sb = new StringBuilder();

		sb.append(session.getServer());
		sb.append("/image/user_");

		if (male) {
			sb.append("male");
		}
		else {
			sb.append("female");
		}

		sb.append("_portrait?img_id=");
		sb.append(portraitId);
		appendToken(sb, uuid);

		return sb.toString();
	}

	protected static void appendToken(StringBuilder sb, String uuid) {
		if (Validator.isNull(uuid)) {
			return;
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(uuid.getBytes());

			byte[] bytes = digest.digest();
			String token = null;

			try {
				token = Base64.encodeToString(bytes, Base64.NO_WRAP);
			}
			catch (RuntimeException re) {
				if ("Stub!".equals(re.getMessage())) {
					token =
						org.apache.commons.codec.binary.Base64.
							encodeBase64String(bytes);
				}
			}

			if (token != null) {
				sb.append("&img_id_token=");
				sb.append(URLEncoder.encode(token, "UTF8"));
			}
		}
		catch (Exception e) {
			Log.e(_CLASS_NAME, "Couldn't generate portrait image token", e);
		}
	}

	protected static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			}
			catch (IOException ioe) {
			}
		}
	}

	private static final String _CLASS_NAME = PortraitUtil.class.getName();

}