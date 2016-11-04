package com.cydeer.ups.proxy;

import com.cydeer.core.result.ApiException;
import com.cydeer.core.utils.http.RequestUtil;
import com.cydeer.ups.client.dto.UserDto;
import com.cydeer.ups.client.support.PermissionInterceptor;
import com.cydeer.ups.client.support.UpsClientImpl;
import com.cydeer.ups.client.utils.UpsClientUtil;
import com.cydeer.ups.constant.UpsConstant;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import static com.cydeer.ups.constant.UpsErrorMessage.USER_NO_LOGIN;

/**
 * @author zhangsong.
 * @date 2016/10/20 下午7:31
 */
public class UpsProxyFilter implements Filter {

	private static final String LOGIN_URL = "/ups/login?target=";
	private static final String FILTER_APPLIED = UpsProxyFilter.class.getName();
	// 正则：匹配路径带有扩展名的地址，如：/path/xxx.htm
	private static final Pattern REG_EXT_NAME_PATTERN = Pattern.compile(".*\\.\\w+");
	private FilterConfig filterConfig;
	private String proxyServletName;
	private String upsServletName;

	@Override public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		this.proxyServletName = filterConfig.getInitParameter("proxyServletName");
		this.upsServletName = filterConfig.getInitParameter("upsServletName");
	}

	@Override public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		if (servletRequest.getAttribute(FILTER_APPLIED) != null) {
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			servletRequest.setAttribute(FILTER_APPLIED, Boolean.TRUE);
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			//
			UpsClientImpl.setRequest(request, PermissionInterceptor.UserRecover.SESSION);
			//用户信息判断
			UserDto user = UpsClientUtil.recoverUserFromSession(request);
			//	long beforeRequestTime = System.currentTimeMillis();
			try {
				this.doRealFilter(request, response, filterChain);
			} finally {
				UpsClientImpl.setRequest(null, null);
			}
		}
	}

	private void doRealFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String rootPath = UpsClientUtil.getRootPath(request);
		if (UpsProxyUtils.single().shouldProxy(rootPath)) {
			// 说明当前请求属于后端系统的请求，需要进行代理处理
			try {
				this.filterConfig.getServletContext().getNamedDispatcher(proxyServletName).forward(request, response);

			} catch (ApiException e) {
				if (USER_NO_LOGIN.getCode().equals(e.getCode())) {
					response.sendRedirect(StringUtils.join(LOGIN_URL, URLEncoder
							.encode(RequestUtil.fullPathWithQuery(request), "UTF-8")));
				} else {
					throw e;
				}
			}

		} else if (StringUtils.equals(UpsConstant.UPS_SYSTEM_PATH, rootPath)) {
			String fullPath = RequestUtil.fullPath(request);
			if (!REG_EXT_NAME_PATTERN.matcher(fullPath).matches()) {
				//对于当前是UPS的请求，进行主要截流处理，不然 getContent()不能获取Form表单提交的请求体
				// TODO: 2016/10/28  克隆流
				/*Utils.get(RequestCopyWrapper.class).cloneStream();*/
				// 当前请求为 ups 系统，而且请求路径不包含扩展名，进入 spring-mvc 处理
				this.filterConfig.getServletContext().getNamedDispatcher(upsServletName).forward(request, response);
				//解决 PermissionInterceptor已经把线程变量置为空的问题
				UpsClientImpl.setRequest(request, PermissionInterceptor.UserRecover.SESSION);
			} else {
				chain.doFilter(request, response);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override public void destroy() {

	}
}
