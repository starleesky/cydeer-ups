package com.cydeer.ups.utils;

import com.cydeer.core.utils.IUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <pre>
 * 对 response 内容响应体进行包装，拦截 content 内容
 * @author winner
 */
public interface ResponseCloneWrapper extends IUtils {

	/**
	 * <pre>
	 * 对 response 包装处理，以便对响应体内容的拦截
	 * 默认采用 autoFlush=true 模式。
	 * 用于过滤器调用。
	 * @param response
	 * @return
	 */
	HttpServletResponse wrapper(ServletRequest request, HttpServletResponse response);

	/**
	 * <pre>
	 * 解除响应流的外包，另外，当 autoFlush 为 false 会将响应体内容输出到输出流中。
	 * 用于过滤器调用。
	 */
	void unWrapper() throws IOException;

	/**
	 * <pre>
	 * 设置是否要拦截输出流。
	 * 默认不拦截，只对输出流进行克隆。
	 * @param hold
	 */
	void holdStream(boolean hold);

	/**
	 * <pre>
	 * 返回当前是否拦截。
	 * 注意：
	 * >. 当 isHoldStream() 首次调用后，@{link {@link #holdStream(boolean)} 将不再生效；
	 * >. 因此必须在开始响应流写入之前必须确定好是否是需要拦截输出流的设置；
	 * >. 比如：String拦截器的 {@link HandlerInterceptorAdapter#preHandle(HttpServletRequest, HttpServletResponse, Object)}
	 * 和 {@link HandlerInterceptorAdapter#postHandle(HttpServletRequest, HttpServletResponse, Object, ModelAndView)}
	 * 中均可设置。
	 * @return
	 */
	boolean isHoldStream();
	//	/**
	//	 * <pre>
	//	 * 对 response 包装处理
	//	 * 用于过滤器调用。
	//	 * @param response 响应对象
	//	 * @param autoFlush true, 表示对原生的响应体输出流及时自动 flush 出去；false, 表示内容不写出到原生响应体输出流中，最
	//	 * 终通过  {@link #flush()} 才将最终内容响应出去。
	//	 * @return
	//	 */
	//	HttpServletResponse wrapper(HttpServletResponse response, boolean autoFlush);

	/**
	 * 判断返回内容是否是二进制
	 *
	 * @return
	 */
	boolean isBinary();

	/**
	 * 获取内容
	 *
	 * @return
	 * @throws IOException
	 */
	String getContent();

	/**
	 * 获取内容，指定编码
	 *
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	String getContent(Charset charset);

	/**
	 * <pre>
	 * 设置返回客户端的响应体内容。
	 * 只有在外包时的 autoFlush 为 false 时才能生效。
	 * @param content 字符串内容
	 */
	void setContent(String content);

	/**
	 * <pre>
	 * 设置返回客户端的响应体内容。
	 * 只有在外包时的 autoFlush 为 false 时才能生效。
	 * @param content 二进制数据内容
	 */
	void setContent(byte[] content);
}
