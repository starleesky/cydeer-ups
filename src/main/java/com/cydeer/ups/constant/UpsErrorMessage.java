package com.cydeer.ups.constant;

import com.cydeer.core.result.ErrorMessage;

/**
 * @author zhangsong.
 * @date 2016/10/28 下午5:14
 */
public enum UpsErrorMessage implements ErrorMessage {
	USER_NO_LOGIN("用户未登录"),
	USER_NO_PERMISSION("用户没有权限");

	private String name;

	UpsErrorMessage(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override public String getCode() {
		return this.toString();
	}

	@Override public String getMessage() {
		return name;
	}
}
