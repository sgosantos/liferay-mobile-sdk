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
import com.liferay.mobile.android.auth.basic.BasicAuthentication;
import com.liferay.mobile.android.service.Session;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import java.net.Proxy;

import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;

/**
 * @author Silvio Santos
 */
public class DigestAuthenticator implements Authenticator {

	public DigestAuthenticator(Session session) {
		_session = session;
	}

	@Override
	public Request authenticate(Proxy proxy, Response response)
		throws IOException {

		Request.Builder builder = response.request().newBuilder();

		try {
			processChallenge(response);
			builder.addHeader(_AUTHORIZATION, getAuthorization(response));
		}
		catch (Exception e) {
			throw new IOException(e);
		}

		return builder.build();
	}

	@Override
	public Request authenticateProxy(Proxy proxy, Response response)
		throws IOException {

		return null;
	}

	public String getAuthorization(Response response) throws Exception {
		BasicHttpRequest request = new BasicHttpRequest(
			response.request().method(), response.request().uri().getPath());

		UsernamePasswordCredentials credentials = getCredentials(_session);

		return _digestScheme.authenticate(
			credentials, request, new BasicHttpContext()).getValue();
	}

	protected UsernamePasswordCredentials getCredentials(Session session)
		throws Exception {

		Authentication auth = session.getAuthentication();

		if (auth == null) {
			throw new Exception("Session's authentication can't be null");
		}

		if (!(auth instanceof BasicAuthentication)) {
			throw new Exception(
				"Can't sign in if authentication implementation is not" +
					"BasicAuthentication");
		}

		String username = ((BasicAuthentication)auth).getUsername();
		String password = ((BasicAuthentication)auth).getPassword();

		return new UsernamePasswordCredentials(username, password);
	}

	protected void processChallenge(Response response)
		throws MalformedChallengeException {

		String authHeader = response.header(HttpHeader.WWW_AUTHENTICATE);

		BasicHeader header = new BasicHeader(
			HttpHeader.WWW_AUTHENTICATE, authHeader);

		_digestScheme.processChallenge(header);
	}

	private static final String _AUTHORIZATION = "Authorization";

	private DigestScheme _digestScheme = new DigestScheme();
	private final Session _session;

}