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

import com.liferay.mobile.android.auth.Authentication;
import com.liferay.mobile.android.exception.RedirectException;
import com.liferay.mobile.android.exception.ServerException;
import com.liferay.mobile.android.service.Session;
import com.liferay.mobile.android.task.UploadAsyncTask;
import com.liferay.mobile.android.util.Validator;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.InputStream;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Bruno Farache
 * @author Silvio Santos
 */
public class HttpUtil {

	public static final String JSONWS_PATH_61 = "api/secure/jsonws";

	public static final String JSONWS_PATH_62 = "api/jsonws";

	public static void checkStatusCode(Response response)
		throws ServerException {

		int status = response.code();

		if ((status == HttpStatus.MOVED_PERMANENTLY) ||
			(status == HttpStatus.MOVED_TEMPORARILY) ||
			(status == HttpStatus.SEE_OTHER) ||
			(status == HttpStatus.TEMPORARY_REDIRECT)) {

			throw new RedirectException(getRedirectUrl(response));
		}

		if (status == HttpStatus.UNAUTHORIZED) {
			throw new ServerException("Authentication failed.");
		}

		if (status != HttpStatus.OK) {
			throw new ServerException(
				"Request failed. Response code: " + status);
		}
	}

	public static OkHttpClient getHttpClient(Session session) {
		OkHttpClient client = new OkHttpClient();

		int timeout = session.getConnectionTimeout();
		client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
		client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
		client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);

		client.setFollowRedirects(false);

		return client;
	}

	public static Request getHttpPost(Session session, String URL, String body)
		throws Exception {

		MediaType type = MediaType.parse("application/json; charset=utf-8");
		RequestBody requestBody = RequestBody.create(type, body);

		Request.Builder builder = new Request.Builder()
			.url(URL)
			.post(requestBody);

		authenticate(session, builder);

		return builder.build();
	}

	public static String getURL(Session session, String path) {
		StringBuilder sb = new StringBuilder();

		String server = session.getServer();

		sb.append(server);

		if (!server.endsWith("/")) {
			sb.append("/");
		}

		sb.append(_JSONWS_PATH);
		sb.append(path);

		return sb.toString();
	}

	public static JSONArray post(Session session, JSONArray commands)
		throws Exception {

		OkHttpClient client = getHttpClient(session);
		Request request = getHttpPost(
			session, getURL(session, "/invoke"), commands.toString());

		Response response = client.newCall(request).execute();
		String json = response.body().string();

		handleServerError(response, json);

		return new JSONArray(json);
	}

	public static JSONArray post(Session session, JSONObject command)
		throws Exception {

		JSONArray commands = new JSONArray();
		commands.put(command);

		return post(session, commands);
	}

	@SuppressWarnings("unused")
	public static void setJSONWSPath(String jsonwsPath) {
		_JSONWS_PATH = jsonwsPath;
	}

	public static JSONArray upload(
			Session session, JSONObject command, UploadAsyncTask task)
		throws Exception {

		String path = (String)command.keys().next();
		JSONObject parameters = command.getJSONObject(path);

		OkHttpClient client = getHttpClient(session);

		RequestBody body = getUploadRequestBody(parameters, task);

		Request.Builder builder = new Request.Builder()
			.url(getURL(session, path))
			.post(body);

		authenticate(session, builder);

		Response response = client.newCall(builder.build()).execute();
		String json = response.body().string();

		handleServerError(response, json);

		return new JSONArray("[" + json + "]");
	}

	protected static void authenticate(Session session, Request.Builder builder)
		throws Exception {

		Authentication authentication = session.getAuthentication();

		if (authentication != null) {
			authentication.authenticate(builder);
		}
	}

	protected static String getRedirectUrl(Response response) {
		String url = response.header(HttpHeader.LOCATION);

		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		return url;
	}

	protected static RequestBody getUploadRequestBody(
			JSONObject parameters, UploadAsyncTask task)
		throws Exception {

		MultipartBuilder builder = new MultipartBuilder()
			.type(MultipartBuilder.FORM);

		Iterator<String> it = parameters.keys();

		while (it.hasNext()) {
			String key = it.next();
			Object value = parameters.get(key);

			if (value instanceof InputStream) {
				InputStream inputStream = (InputStream)value;

				String mimeType = parameters.getString("mimeType");
				String title = parameters.getString("title");

				RequestBody body = InputStreamRequestBody.create(
					MediaType.parse(mimeType), inputStream, task);

				builder.addFormDataPart(key, title, body);
			}
			else {
				builder.addFormDataPart(key, value.toString());
			}
		}

		return builder.build();
	}

	protected static void handlePortalException(String json)
		throws ServerException {

		try {
			if (isJSONObject(json)) {
				JSONObject jsonObj = new JSONObject(json);

				if (jsonObj.has("exception")) {
					String message = jsonObj.getString("exception");
					String detail = jsonObj.optString("message", null);

					JSONObject error = jsonObj.optJSONObject("error");

					if (error != null) {
						message = error.getString("type");
						detail = error.getString("message");
					}

					throw new ServerException(message, detail);
				}
			}
		}
		catch (JSONException je) {
			throw new ServerException(je);
		}
	}

	protected static void handleServerError(Response response, String json)
		throws ServerException {

		checkStatusCode(response);
		handlePortalException(json);
	}

	protected static boolean isJSONObject(String json) {
		if (Validator.isNotNull(json) && json.startsWith("{")) {
			return true;
		}

		return false;
	}

	private static String _JSONWS_PATH = JSONWS_PATH_62;

}