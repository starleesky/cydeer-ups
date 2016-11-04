package com.cydeer.ups.utils;

import com.cydeer.core.utils.IUtilsBuilder;

/**
 * @author zhangsong.
 * @date 2016/10/28 下午10:03
 */
public class RequestCopyWrapperBuilder implements IUtilsBuilder<RequestCopyWrapper> {
	@Override
	public String getConfName() {
		return null;
	}

	@Override
	public RequestCopyWrapper build() {
		return new RequestCopyWrapperImpl();
	}
}
