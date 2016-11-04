package com.cydeer.ups.controller;

import com.cydeer.core.result.ApiResult;
import com.cydeer.ups.core.entity.DomainExample;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by zhangsong on 15/8/20.
 */
@Controller
public class DemoController {

	/*@Resource
	private UserInfoService userInfoService;*/

	@RequestMapping("/ups/test/demo")
	@ResponseBody
	public ApiResult<DomainExample> demo(@RequestParam(value = "userName") String userName,
			@RequestParam(value = "pwd") String pwd) {
		DomainExample userInfo = new DomainExample();
		userInfo.setName(userName);
		userInfo.setDescPwd(pwd);
		userInfo.setAge(18);
		/*userInfoService.save(userInfo);*/
		return new ApiResult<>(userInfo);
	}

	@RequestMapping(value = "/ups/demo")  //,produces = "application/json;charset=UTF-8"
	@ResponseBody
	public ApiResult<String> demoVo(@RequestParam(value = "id") Integer id) {
		//UserAddress userAddress = userAddressService.findByUserId(id);
		DomainExample userInfo = new DomainExample();

        /*String s = Jackson.base().writeValueAsString(userAddress);
        System.out.println(s);*/

		return new ApiResult<String>("ok");
	}
}
