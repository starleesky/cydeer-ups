package com.cydeer.ups.proxy;

import com.cydeer.core.result.ApiException;
import com.cydeer.core.utils.http.RequestUtil;
import com.cydeer.ups.client.utils.UpsClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.message.BasicHeader;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * @author zhangsong.
 * @date 2016/10/20 下午7:32
 */
public class UpsProxyServlet extends ProxyServlet {

	private final static Logger LOGGER = LoggerFactory.getLogger(UpsProxyServlet.class);

	@Override
	protected String getConfigParam(String key) {
		// 解决 targetUri 没设置，基类报错的问题
		if (StringUtils.equals(P_TARGET_URI, key))
			return "";
		return super.getConfigParam(key);
	}

	/**
	 * 按一定规则进行目标代理服务器的设置
	 */
	@Override
	protected String getTargetUri(HttpServletRequest servletRequest) {
		String targetUri = (String) servletRequest.getAttribute(ATTR_TARGET_URI);
		if (StringUtils.isNotBlank(targetUri)) {
			return targetUri;
		}
		// 根据请求的 /my/path.html 路径解析根路径名称
		String fullPath = RequestUtil.fullPath(servletRequest);
		URI proxyHost = UpsProxyUtils.single().getProxyUri(servletRequest);
		if (proxyHost == null) {
			throw new IllegalArgumentException("找不到路径 [" + fullPath + "] 匹配的后端代理系统主机,请确定您访问的服务是否已经正确启动");
		}
		targetUri = StringUtils.join(proxyHost.toString(), fullPath);
		LOGGER.debug("代理转发后端服务请求： {}", targetUri);
		servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
		servletRequest.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(proxyHost));
		return targetUri;
	}

	/**
	 * <pre>
	 * 增加对请求头的处理，比如添加自定义 cookie 信息
	 */
	@Override
	protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
		super.copyRequestHeaders(servletRequest, proxyRequest);
		//将用户信息添加到头中
		Header header = UpsClientUtil.buildUserHeader(servletRequest);
		//Header envMetaHeader = UpsClientUtil.buildEnvMetaHeader(servletRequest);
		if (header != null) {
			proxyRequest.addHeader(header);
			//proxyRequest.addHeader(envMetaHeader);
		}
		// 重新设置 host 头信息，保留用户请求的主机头
		proxyRequest.setHeader(new BasicHeader(HttpHeaders.HOST, servletRequest.getHeader(HttpHeaders.HOST)));
		// 设置在301/302等状态码时，httpClient不做自动跳转工作
		proxyRequest.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
	}

	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		try {
			super.service(servletRequest, servletResponse);
		} catch (ServletException e) {
			throw e;
		} catch (IOException e) {

			throw new IOException("请确定您访问的服务是否已经正确启动 ", e);
		}

	}

	@Override
	protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse)
			throws IOException {
		System.out.println("code : " + proxyResponse.getStatusLine().getStatusCode());
		// 添加ups自定义流信息
		if (proxyResponse.getStatusLine().getStatusCode() == 404) {
			throw new ApiException("404", "您访问链接返回404,请确定您访问的服务是否已经正确启动 ");
		}
		if (proxyResponse.getStatusLine().getStatusCode() == 400) {
			throw new ApiException("400", "您访问链接返回400,请确定请求方式是否正确或服务请求header是否过大,请重新分配用户权限 ");
		}
		super.copyResponseEntity(proxyResponse, servletResponse);
	}
}
