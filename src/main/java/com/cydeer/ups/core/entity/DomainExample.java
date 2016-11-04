package com.cydeer.ups.core.entity;

import java.io.Serializable;

/**
 * Created by zhangsong on 16/3/24.
 */
public class DomainExample implements Serializable {

	private Integer id;

	private String name;

	private Integer age;

	private String descPwd;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getDescPwd() {
		return descPwd;
	}

	public void setDescPwd(String descPwd) {
		this.descPwd = descPwd;
	}
}
