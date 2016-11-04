package com.cydeer.ups.core.entity;

import java.util.Date;

/**
 * @author zhangsong.
 * @date 2016/10/20 下午7:38
 */
public class Module {
	//模块ID
	private int id;
	//模块或操作名称
	private String name;
	//模块或操作PATH路径
	private String path;
	//模块或操作全 路径
	private String fullPath;
	//模块下指定的默认页，只针对模块类型有效
	private String defaultPage;
	//模块类型：1模块组，2模块，3操作
	private int type;
	//是否是虚拟模块
	private Integer isVirtual;
	//模块或操作图标，暂先不支持
	private String icon;
	//所属的系统ID
	private int systemId;
	//对应父亲模块组ID
	private int parentId;
	//节点的排序字段，值从 1 开始增加
	private int sort;
	//是否生效
	private Integer isValid;
	//当前模块下的Get操作是否日志记录，0不记录，1记录
	private Integer getLog;
	//当前模块是否做日志记录，0不记录，1记录
	private Integer doLog;
	//创建时间
	private Date createTime;
	//修改时间
	private Date modifyTime;
}
