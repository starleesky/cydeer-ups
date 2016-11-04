package com.cydeer.ups.utils;

import com.cydeer.core.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author zhangsong.
 * @date 2016/10/28 下午10:05
 */
public class RequestCopyInputFilter implements Filter {
	private static final String FILTER_APPLIED = RequestCopyInputFilter.class.getName();
	//当采用POST请求的表单提交时，解析文本采用的编码。默认 UTF-8
	private String encoding = "UTF-8";

	public void init(FilterConfig filterConfig) throws ServletException {
		String encoding = filterConfig.getInitParameter("encoding");
		if (StringUtils.isNotBlank(encoding)) {
			this.encoding = encoding;
		}
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		if (req.getAttribute(FILTER_APPLIED) != null) {
			chain.doFilter(req, resp);
		} else {
			req.setAttribute(FILTER_APPLIED, Boolean.TRUE);
			if (req instanceof HttpServletRequest) {
				HttpServletRequest request = (HttpServletRequest) req;
				req = Utils.get(RequestCopyWrapper.class).wrapper(request, encoding);
			}

			try {
				chain.doFilter(req, resp);
			} catch (Exception e) {
				Utils.get(RequestCopyWrapper.class).unWrapper();
				throw e;
			} finally {
				//保证绝对关闭相关资源
				Utils.get(RequestCopyWrapper.class).unWrapper();
			}
		}
	}

}
