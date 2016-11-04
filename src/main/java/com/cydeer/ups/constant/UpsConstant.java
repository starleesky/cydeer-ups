package com.cydeer.ups.constant;

public interface UpsConstant {

	/**
	 * 管理默认账号
	 */
	String DEFAULT_ADMIN_NAME = "admin";

	/**
	 * 邮箱账号同步下来默认的密码
	 */
	String DEFAULT_USER_PSW = "666666";
	/** LDAP格式加密过的密码 */
	//String DEFAULT_USER_PSW_LDAP = LdapPasswordOperator.single().encrypt(Type.MD5, DEFAULT_USER_PSW);
	/**
	 * UPS系统路径
	 */
	String UPS_SYSTEM_PATH = "/ups";

	/**
	 * 模块类型：模块组
	 */
	int MODULE_TYPE_GROUP = 1;
	/**
	 * 模块类型：模块
	 */
	int MODULE_TYPE_MODULE = 2;
	/**
	 * 模块类型：操作
	 */
	int MODULE_TYPE_OPERATION = 3;

	String ADMINISTRATIVE_STR = "行政";

	String SESSION_ENV = "";
}
