/*
 * 
 * @author swing
 */
package com.cydeer.ups.utils;

import com.cydeer.core.utils.Utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <pre>
 * 增强 request 输入流功能，通过流数据的克隆让其支持输入流多次被读取的功能。
 * 解决底层输入流默认只能被一次的问题，也就是避免的 'stream closed' 的异常。
 *
 * 注意：该功能只适用于底层框架（如：springMVC）在读取 body 之前才可以，也就是说一般在拦截器的 preHandler 中是可正常
 * 使用，如果是 postHandle 再调同样会报错。
 * @author
 */
public class ResponseCloneFilter implements Filter {
	private static final String FILTER_APPLIED = ResponseCloneFilter.class.getName();
	//	private boolean autoFlush = true;

	public void init(FilterConfig filterConfig) throws ServletException {
		//		String config = filterConfig.getInitParameter("autoFlush");
		//		if(StringUtils.isNotBlank(config)){
		//			autoFlush = BooleanUtils.toBoolean(filterConfig.getInitParameter("autoFlush"));
		//		}
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		if (req.getAttribute(FILTER_APPLIED) != null) {
			chain.doFilter(req, resp);
		} else {
			req.setAttribute(FILTER_APPLIED, Boolean.TRUE);
			ResponseCloneWrapper responseClone = Utils.get(ResponseCloneWrapper.class);
			if (req instanceof HttpServletRequest) {
				resp = responseClone.wrapper(req, (HttpServletResponse) resp);
			}
			try {
				chain.doFilter(req, resp);
			} catch (Exception e) {
				responseClone.unWrapper();
				throw e;
			} finally {
				//保证绝对关闭相关资源
				responseClone.unWrapper();
			}
		}
	}

}
