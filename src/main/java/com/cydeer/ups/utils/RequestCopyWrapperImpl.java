package com.cydeer.ups.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangsong.
 * @date 2016/10/28 下午10:03
 */
public class RequestCopyWrapperImpl implements RequestCopyWrapper {
	private static ThreadLocal<RequestCloneInputWrapper> requestLocal = new ThreadLocal<RequestCloneInputWrapper>();
	// 是否截流标识
	private static final String HOLD_STREAM = RequestCopyWrapperImpl.class.getName() + ".hold_stream";
	// 是否已读流标识
	private static final String READ_STREAM = RequestCopyWrapperImpl.class.getName() + ".read_stream";
	private static final String METHOD_POST = "POST";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

	@Override
	public HttpServletRequest wrapper(HttpServletRequest request) throws IOException {
		return this.wrapper(request, null);
	}

	@Override
	public HttpServletRequest wrapper(HttpServletRequest request, String encoding) throws IOException {
		// 生成request的外包处理
		RequestCloneInputWrapper req = new RequestCloneInputWrapper(request, encoding, this);
		requestLocal.set(req);
		return req;
	}

	@Override
	public void unWrapper() throws IOException {
		if (requestLocal.get() != null) {
			requestLocal.set(null);
		}
	}

	@Override
	public synchronized void cloneStream() {
		if (requestLocal.get() == null) {
			return;
		}
		// 判断是否已读过流
		Object read = requestLocal.get().getAttribute(READ_STREAM);
		if (read != null && (boolean) read) {
			return;
		}
		requestLocal.get().setAttribute(HOLD_STREAM, true);
		// 当是截流模式时，在进入controller执行前必须触发getInputSteam()把流提前克隆好，不然Tomcat对Form表单提交时不触发getInputStream()
		try {
			if (requestLocal.get().formRequest) {
				requestLocal.get().getInputStream();
			} else {
				requestLocal.get().buildCloneStream();
			}
		} catch (IOException e) {
			// 静默处理
		}
	}

	@Override
	public String getContent() throws IOException {
		return this.getContent(null);
	}

	@Override
	public String getContent(String encode) throws IOException {
		if (requestLocal.get() == null) {
			return "";
		}
		return requestLocal.get().getContent(encode);
	}

	@Override
	public void setContent(String newContent) throws IOException {
		if (!this.isHoldStream()) {
			throw new IllegalArgumentException("必须是截流模式才能设置新的请求体内容");
		}
		if (requestLocal.get() != null) {
			requestLocal.get().setContent(newContent);
		}
	}

	private boolean isHoldStream() {
		if (requestLocal.get() != null) {
			Object hold = requestLocal.get().getAttribute(HOLD_STREAM);
			if (hold != null) {
				return (boolean) hold;
			}
		}
		return false;
	}

	private static class RequestCloneInputWrapper extends HttpServletRequestWrapper {
		private static String DEFAULT_ENCODE = "UTF-8";
		// private byte[] copiedBytes;
		private HttpServletRequest request;
		private String encoding;
		private RequestCopyWrapperImpl copyWrapper;
		private CloneServletInputStream cloneStream;
		private CopyServletInputStream copySteam;
		private CopyReader copyReader;
		private boolean formRequest = false;
		private String formData = null;

		RequestCloneInputWrapper(HttpServletRequest request, String encoding, RequestCopyWrapperImpl copyWrapper)
				throws IOException {
			super(request);
			this.request = request;
			// 先从请求体对象中的编码为准
			this.encoding = request.getCharacterEncoding();
			// 再是过滤器自己设置的值
			if (StringUtils.isBlank(this.encoding)) {
				this.encoding = encoding;
			}
			// 最后才是默认UTF-8
			if (StringUtils.isBlank(this.encoding)) {
				this.encoding = DEFAULT_ENCODE;
			}
			this.copyWrapper = copyWrapper;
			formRequest = StringUtils.containsIgnoreCase(this.request.getHeader(CONTENT_TYPE), FORM_URLENCODED);
			if (formRequest) {
				// 为什么要这么做：因为Tomcat中对getParameterMap()方法的实现，在获取流时，不是通过request.getInputStream()获取
				StringBuilder sb = new StringBuilder();
				@SuppressWarnings("unchecked")
				Map<String, String[]> params = request.getParameterMap();
				Set<String> keys = params.keySet();
				for (String key : keys) {
					String[] values = params.get(key);
					if (values != null && values.length > 0) {
						for (String value : values) {
							sb.append(StringUtils.join(URLEncoder.encode(key, encoding), "=",
									URLEncoder.encode(value, encoding), "&"));
						}
					}
				}
				formData = StringUtils.removeEnd(sb.toString(), "&");
			}
		}

		public String getContent(String encode) throws IOException {
			if (this.copyWrapper.isHoldStream()) {
				if (cloneStream != null) {
					if (encode == null)
						encode = this.encoding;
					return new String(cloneStream.cloneBytes, encode);
				}
			} else {
				if (copySteam != null) {
					return new String(copySteam.getContentBytes(), this.encoding);
				} else if (copyReader != null) {
					return new String(copyReader.getContentChars());
				}
			}
			return "";
		}

		public void setContent(String newContent) throws IOException {
			if (this.copyWrapper.isHoldStream() && cloneStream != null) {
				cloneStream.setContent(newContent.getBytes(this.encoding));
			}
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			// 已读流，之后再设置截流模式将不生效
			requestLocal.get().setAttribute(READ_STREAM, true);
			if (!StringUtils.equals(this.request.getMethod(), METHOD_POST)) {
				return super.getInputStream();
			}
			if (this.copyWrapper.isHoldStream()) {// 如果是截流模式，采用流克隆
				this.buildCloneStream();
				return this.cloneStream.getStream();
			} else {
				this.buildWrapperStream();
				return this.copySteam.getStream();
			}
		}

		@Override
		public BufferedReader getReader() throws IOException {
			if (!StringUtils.equals(this.request.getMethod(), METHOD_POST)) {
				return super.getReader();
			}
			if (this.copyWrapper.isHoldStream()) {// 如果是截流模式，采用流克隆
				this.buildCloneStream();
				return cloneStream.getReader();
			} else {
				this.buildCopyReader();
				return this.copyReader.getReader();
			}
		}

		private synchronized void buildCloneStream() throws IOException {
			if (cloneStream == null) {
				cloneStream = new CloneServletInputStream(this, request, encoding);
			}
		}

		private synchronized void buildWrapperStream() throws IOException {
			if (copySteam == null) {
				copySteam = new CopyServletInputStream(this, request.getInputStream());
			}
		}

		private synchronized void buildCopyReader() throws IOException {
			if (copyReader == null) {
				copyReader = new CopyReader(this, request.getReader());
			}
		}

		/**
		 * <pre>
		 * 对于application/x-www-form-urlencoded的表单提交的数据，是通过 getParameterMap() 接口还原出Body的数据，因为
		 * 客户端对特殊字符（如空格）encode方式不同，会导致服务端重新编码后的Body长度与原始长度不符，导致SpringMVC对对象
		 * 入参的解析出问题。
		 * 故此接口修正新的长度
		 */
		@Override
		public int getContentLength() {
			if (this.formRequest) {
				if (cloneStream != null && cloneStream.cloneBytes != null) {
					return cloneStream.cloneBytes.length;
				}
				if (copySteam != null) {
					return copySteam.getContentBytes().length;
				}
			}
			return super.getContentLength();
		}

	}

	private static class CloneServletInputStream {
		//		private String uriEncode;
		private byte[] cloneBytes;
		private RequestCloneInputWrapper cloneRequest;
		private HttpServletRequest request;

		CloneServletInputStream(RequestCloneInputWrapper cloneRequest, HttpServletRequest request, String uriEncode)
				throws IOException {
			this.cloneRequest = cloneRequest;
			this.request = request;
			//			this.uriEncode = uriEncode;
			cloneBytesFromRequest();
		}

		public synchronized ServletInputStream getStream() throws IOException {
			return new CachedServletInputStream(cloneBytes);
		}

		public synchronized BufferedReader getReader() throws IOException {
			return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cloneBytes)));
		}

		public synchronized void setContent(byte[] newContent) {
			cloneBytes = newContent;
		}

		private void cloneBytesFromRequest() throws IOException {
			cloneBytes = new byte[0];
			// 针对“application/x-www-form-urlencoded”请求体，从参数中的 key/value
			// 模拟还原出Body内容
			if (cloneRequest.formRequest) {
				// //
				// 为什么要这么做：因为Tomcat中对getParameterMap()方法的实现，在获取流时，不是通过request.getInputStream()获取
				// StringBuilder sb = new StringBuilder();
				// @SuppressWarnings("unchecked")
				// Map<String, String[]> params = request.getParameterMap();
				// Set<String> keys = params.keySet();
				// for (String key : keys) {
				// String[] values = params.get(key);
				// if (values != null && values.length > 0) {
				// for (String value : values) {
				// sb.append(StringUtils.join(URLEncoder.encode(key, uriEncode),
				// "=",
				// URLEncoder.encode(value, uriEncode), "&"));
				// }
				// }
				// }
				// cloneBytes = StringUtils.removeEnd(sb.toString(),
				// "&").getBytes();
				cloneBytes = cloneRequest.formData.getBytes();
			} else {// 采用父亲的输入流进行克隆
				ServletInputStream inputStream = request.getInputStream();
				if (inputStream != null) {
					ByteArrayOutputStream cachedByteOutput = new ByteArrayOutputStream();
					IOUtils.copy(inputStream, cachedByteOutput);
					cloneBytes = cachedByteOutput.toByteArray();
					inputStream.close();
					cachedByteOutput.close();
				}
			}
		}
	}

	/* An inputstream which reads the cached request body */
	private static class CachedServletInputStream extends ServletInputStream {
		private ByteArrayInputStream input;

		public CachedServletInputStream(byte[] copiedBytes) throws IOException {
			/* create a new input stream from the cached request body */
			input = new ByteArrayInputStream(copiedBytes);
		}

		@Override
		public int read() throws IOException {
			return input.read();
		}
	}

	private static class CopyServletInputStream extends ServletInputStream {
		private ServletInputStream inputStream;
		private ByteArrayOutputStream copyOutput = new ByteArrayOutputStream();
		private boolean copyOver = false;
		private byte[] copyBytes;

		CopyServletInputStream(RequestCloneInputWrapper cloneRequest, ServletInputStream inputStream)
				throws IOException {
			this.inputStream = inputStream;
			if (cloneRequest.formRequest) {
				copyBytes = cloneRequest.formData.getBytes();
				copyOver = true;
			}
		}

		public ServletInputStream getStream() throws IOException {
			if (copyOver) {// 当拷贝结束时，需要模拟新的InputStream
				return new CachedServletInputStream(copyBytes);
			} else {
				return this;
			}
		}

		public byte[] getContentBytes() {
			return copyBytes;
		}

		@Override
		public int read() throws IOException {
			int data = inputStream.read();
			if (data == -1) {
				copyBytes = copyOutput.toByteArray();
				copyOver = true;
				return data;
			}
			copyOutput.write(data);
			return data;
		}
	}

	private static class CopyReader extends BufferedReader {
		private CharArrayWriter copyOutput = new CharArrayWriter();
		private boolean copyOver = false;
		private char[] copyChars;

		CopyReader(RequestCloneInputWrapper cloneRequest, Reader reader) {
			super(reader);
			if (cloneRequest.formRequest) {
				copyChars = cloneRequest.formData.toCharArray();
				copyOver = true;
			}
		}

		public BufferedReader getReader() throws IOException {
			if (copyOver) {// 当拷贝结束时，需要模拟新的Reader
				CharArrayReader reader = new CharArrayReader(copyChars);
				return new CachedCharReader(reader);
			} else {
				return this;
			}
		}

		public char[] getContentChars() {
			return copyChars;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			int data = super.read(cbuf, off, len);
			copyOutput.write(cbuf);
			if (data == -1) {
				copyChars = copyOutput.toCharArray();
				copyOver = true;
			}
			return data;
		}
	}

	private static class CachedCharReader extends BufferedReader {

		public CachedCharReader(Reader in) {
			super(in);
		}

	}
}
