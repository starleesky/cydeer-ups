package com.cydeer.ups.utils;

import com.cydeer.core.utils.IUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author zhangsong.
 * @date 2016/10/28 下午10:02
 */
public interface RequestCopyWrapper extends IUtils {
	/**
	 * <pre>
	 * 对 request 包装处理
	 * 用于过滤器调用。
	 * @param request
	 * @return
	 */
	HttpServletRequest wrapper(HttpServletRequest request) throws IOException;

	/**
	 * @param request
	 * @param uriEncode 默认采用 UTF-8实现
	 * @return
	 */
	HttpServletRequest wrapper(HttpServletRequest request, String encoding) throws IOException;

	/**
	 * <pre>
	 * 解除请求流的外包，清除线程变量中的数据。
	 */
	void unWrapper() throws IOException;

	/**
	 * <pre>
	 * 默认处理采用流拷贝而不是流克隆，通过本方法设置成流克隆模式。
	 * 流拷贝：即在底层系统（如Spring）使用请求流后，顺便拷贝相应的流信息，以便在Controller处理后可以低成本的获取请求体信息。
	 * 流克隆：即在底层系统（如Spring）需要请求流前，就提前进行流克隆处理，以便在Controller处理前就可以获取请求体信息。
	 *
	 * 注意：
	 * 1. 该方法只有当系统（如Spring）调用 getInputStream() 之前设置才能生效。
	 * 2. 该方法主要适用在Controller 执行之前需要请求体内容的情景，如Filter的 doFilter()之前、或是Spring拦截器 preHandle()
	 * 3. 如果在此情况下没有设置 cloneStream()调用 {@link #getContent()} 时，将返回空字符串。
	 * 4. 如果是在系统调用 getInputStream() 需要请求体内容，则不需要启用 cloneStream() 操作。
	 * @param hold
	 */
	void cloneStream();

	/**
	 * 获取请求体内容。
	 * 字符编码默认以设置的 uriEncode 为准，若 uriEncode 未设置，默认以 UTF-8 准
	 *
	 * @return
	 * @throws IOException
	 */
	String getContent() throws IOException;

	/**
	 * 获取请求体内容
	 *
	 * @param encode 指定编码
	 * @return
	 * @throws IOException
	 */
	String getContent(String encode) throws IOException;

	/**
	 * 设置新的请求体内容。只有在截流模式下才可设置新请求体
	 *
	 * @param newContent 新的请求体内容
	 * @return
	 * @throws IOException
	 */
	void setContent(String newContent) throws IOException;
}
