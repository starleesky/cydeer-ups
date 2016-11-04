package com.cydeer.ups.utils;

import org.apache.commons.io.Charsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

public class ResponseCloneWrapperImpl implements ResponseCloneWrapper {
	private static ThreadLocal<ServletRequest> requestLocal = new ThreadLocal<ServletRequest>();
	private static final String CLONE_RESP = ResponseCloneWrapperImpl.class.getName() + ".clone_resp";
	private static final String HOLD_STREAM = ResponseCloneWrapperImpl.class.getName() + ".hold_stream";
	private static final String HOLD_STREAM_STABLE = HOLD_STREAM + ".stable";

	@Override
	public HttpServletResponse wrapper(ServletRequest request, HttpServletResponse response) {
		//生成response的外包处理
		ResponseCloneOutput resp = new ResponseCloneOutput(response, this);
		request.setAttribute(CLONE_RESP, resp);
		requestLocal.set(request);
		return resp;
	}

	@Override
	public void unWrapper() throws IOException {
		ResponseCloneOutput resp = this.getCloneResp();
		try {
			if (resp != null) {
				resp.originalFlush();
			}
			requestLocal.set(null);
		} catch (IOException e) {
			requestLocal.set(null);
			throw e;
		}
	}

	@Override
	public void holdStream(boolean hold) {
		if(requestLocal.get() != null){
			requestLocal.get().setAttribute(HOLD_STREAM, true);
		}
	}
	
	/*
	 * 读取当前请求是否是截流
	 */
	@Override
	public boolean isHoldStream(){
		if(requestLocal.get() == null){
			return false;
		}
		//当前请求已读过：从已经读过的读取
		Object hold = requestLocal.get().getAttribute(HOLD_STREAM_STABLE);
		if(hold != null){
			return (boolean)hold;
		}
		//当前请求未读过：从用户设置的值读取，若用户未设置默认为 false
		boolean holdStream = false;
		hold = requestLocal.get().getAttribute(HOLD_STREAM);
		if(hold != null){
			holdStream = (boolean)hold;
		}
		//将当前请求的结果保存到当前请求对象中
		requestLocal.get().setAttribute(HOLD_STREAM_STABLE, holdStream);
		return holdStream;
	}
	
//	@Override
//	public HttpServletResponse wrapper(HttpServletResponse response, boolean autoFlush) {
//		ResponseCloneOutput resp = new ResponseCloneOutput(response, autoFlush);
//		respLocal.set(resp);
//		return resp;
//	}

	@Override
	public boolean isBinary() {
		ResponseCloneOutput resp = this.getCloneResp();
		return resp != null? resp.isBinary() : false;
	}

	@Override
	public String getContent() {
		return this.getContent(Charsets.UTF_8);
	}

	@Override
	public String getContent(Charset charset) {
		ResponseCloneOutput resp = this.getCloneResp();
		if (resp == null) {
			return "";
		}
		return resp.getContent(charset);
	}

	@Override
	public void setContent(String content) {
		ResponseCloneOutput resp = this.getCloneResp();
		if (resp != null) {
			resp.setContent(content);
		}
	}

	@Override
	public void setContent(byte[] content) {
		ResponseCloneOutput resp = this.getCloneResp();
		if (resp != null) {
			resp.setContent(content);
		}
	}
	
	private ResponseCloneOutput getCloneResp(){
		if(requestLocal.get() == null){
			return null;
		}
		return (ResponseCloneOutput)requestLocal.get().getAttribute(CLONE_RESP);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * <pre>
	 * 参考：http://blog.csdn.net/shuwei003/article/details/7986158
	 * @author winner
	 */
	private static class ResponseCloneOutput extends HttpServletResponseWrapper {
		private ResponseCloneWrapperImpl wrapper;
		private HttpServletResponse response;
		private ResponseCloneOutputStream outputStreamWrapper;
		private ResponseClonePrintWriter writeWrapper;
		private String respContent;
		private byte[] respByteContent;

		public ResponseCloneOutput(HttpServletResponse response, ResponseCloneWrapperImpl wrapper) {
			super(response);
			this.response = response;
			this.wrapper = wrapper;
		}

		public String getContent(Charset charset) {
			if (writeWrapper != null) {
				return writeWrapper.getContent();
			} else if (outputStreamWrapper != null) {
				return new String(outputStreamWrapper.getContent(), charset);
			} else {
				return "";
			}
		}

		public boolean isBinary() {
			return writeWrapper == null;
		}
		public void setContent(String content) {
			if (this.wrapper.isHoldStream()){
				this.respContent = content;
			}
		}
		public void setContent(byte[] content) {
			if (this.wrapper.isHoldStream()){
				this.respByteContent = content;
			}
		}

		public void originalFlush() throws IOException {
			if (this.wrapper.isHoldStream()){
				if (writeWrapper != null)
					writeWrapper.originalFlush(respContent);
				if (outputStreamWrapper != null)
					outputStreamWrapper.originalFlush(respByteContent);
			}
		}

		public void finalize() throws Throwable {
			super.finalize();
			if (writeWrapper != null)
				writeWrapper.close();
		}

		// 覆盖getWriter()方法，使用我们自己定义的Writer
		@Override
		public PrintWriter getWriter() throws IOException {
			if (writeWrapper == null) {
				synchronized (ResponseCloneOutput.class) {
					if (writeWrapper == null) {
						writeWrapper = new ResponseClonePrintWriter(this.response.getWriter(), this.wrapper);
					}
				}
			}

			return writeWrapper;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (outputStreamWrapper == null) {
				synchronized (ResponseCloneOutput.class) {
					if (outputStreamWrapper == null) {
						outputStreamWrapper = new ResponseCloneOutputStream(this.response.getOutputStream(), this.wrapper);
					}
				}
			}
			return outputStreamWrapper;
		}

	}

	private static class ResponseClonePrintWriter extends PrintWriter {
		private ResponseCloneWrapperImpl wrapper;
		private PrintWriter originalWriter;
		private StringWriter strWriter = new StringWriter();

		public ResponseClonePrintWriter(PrintWriter originalWriter, ResponseCloneWrapperImpl wrapper) {
			super(originalWriter);
			this.originalWriter = originalWriter;
			this.wrapper = wrapper;
		}

		public String getContent() {
			return strWriter.toString();
		}

		public void originalFlush(String content) throws IOException {
			content = content == null ? this.getContent() : content;
			this.originalWriter.print(content);
			this.originalWriter.flush();
			super.close();
		}

		@Override
		public void write(int c) {
			strWriter.write(c);
			if (!this.wrapper.isHoldStream()){
				super.write(c);
			}
		}

		@Override
		public void write(char[] buf, int off, int len) {
			strWriter.write(buf, off, len);
			if (!this.wrapper.isHoldStream()){
				super.write(buf, off, len);
			}
		}

		@Override
		public void write(char[] buf) {
			write(buf, 0, buf.length);
		}

		@Override
		public void write(String s, int off, int len) {
			strWriter.write(s, off, len);
			if (!this.wrapper.isHoldStream()){
				super.write(s, off, len);
			}
		}

		@Override
		public void write(String s) {
			write(s, 0, s.length());
		}

		@Override
		public void flush() {
			if (!this.wrapper.isHoldStream()){
				super.flush();
			}
		}

		@Override
		public void close() {
			if (!this.wrapper.isHoldStream()){
				super.close();
			}
		}
	}

	private static class ResponseCloneOutputStream extends ServletOutputStream {
		private ByteArrayOutputStream cachedOutputStream = new ByteArrayOutputStream();
		private ResponseCloneWrapperImpl wrapper;
		private ServletOutputStream outputStream;

		private ResponseCloneOutputStream(ServletOutputStream outputStream, ResponseCloneWrapperImpl wrapper) {
			this.outputStream = outputStream;
			this.wrapper = wrapper;
		}

		public byte[] getContent() {
			return this.cachedOutputStream.toByteArray();
		}

		public void originalFlush(byte[] content) throws IOException {
			content = content == null ? this.getContent() : content;
			this.outputStream.write(content);
			this.outputStream.flush();
			this.outputStream.close();
		}

		@Override
		public void flush() throws IOException {
			if (!this.wrapper.isHoldStream()){
				outputStream.flush();
			}
		}

		@Override
		public void write(int b) throws IOException {
			cachedOutputStream.write(b);
			if (!this.wrapper.isHoldStream()){
				outputStream.write(b);
			}
		}

		@Override
		public void close() throws IOException {
			if (!this.wrapper.isHoldStream()){
				outputStream.close();
			}
		}
	}
}
