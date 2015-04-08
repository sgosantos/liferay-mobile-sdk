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

package com.liferay.mobile.android.auth.basic;

import com.liferay.mobile.android.auth.Authentication;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Request;

/**
 * @author Bruno Farache
 */
public class BasicAuthentication implements Authentication {

	public BasicAuthentication(String username, String password) {
		_username = username;
		_password = password;
	}

	@Override
	public void authenticate(Request.Builder builder) {
		String credentials = Credentials.basic(_username, _password);

		builder.addHeader("Authorization", credentials);
	}

	public String getPassword() {
		return _password;
	}

	public String getUsername() {
		return _username;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public void setUsername(String username) {
		_username = username;
	}

	private String _password;
	private String _username;

}