package com.cydeer.ups.utils;

import com.cydeer.core.utils.IUtilsBuilder;

public class ResponseCloneWrapperBuilder implements IUtilsBuilder<ResponseCloneWrapper> {

	@Override
	public String getConfName() {
		return null;
	}

	@Override
	public ResponseCloneWrapper build() {
		return new ResponseCloneWrapperImpl();
	}

}
