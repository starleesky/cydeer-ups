package com.cydeer.ups.proxy;

import com.cydeer.ups.client.utils.UpsClientUtil;
import com.cydeer.ups.constant.UpsConstant;
import com.cydeer.ups.core.entity.UpsSystem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author zhangsong.
 * @date 2016/10/20 下午7:32
 */
public class UpsProxyUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpsProxyUtils.class);
	private static volatile UpsProxyUtils instance = null;
	private List<UpsSystem> allSystems;

	public static UpsProxyUtils single() {
		if (instance == null) {
			synchronized (UpsProxyUtils.class) {
				if (instance == null) {
					instance = new UpsProxyUtils();
				}
			}
		}
		return instance;
	}

	private enum ProxyServerPlan {
		LOCAL, REMOTE, ZOOKEEPER;

		public static ProxyServerPlan buildPlan(String plan) {
			if (StringUtils.equalsIgnoreCase("local", plan))
				return LOCAL;
			if (StringUtils.equalsIgnoreCase("remote", plan))
				return REMOTE;
			return ZOOKEEPER;
		}
	}

	private ProxyServerPlan proxyServerPlan = ProxyServerPlan.LOCAL;
	private int proxyPort = 8081;

	/**
	 * <pre>
	 * 判断当前请求是否需要代理处理
	 *
	 * @param rootPath
	 * @return
	 */
	public boolean shouldProxy(String rootPath) {
		// 如果是UPS系统，不需要代理
		if (StringUtils.equalsIgnoreCase(UpsConstant.UPS_SYSTEM_PATH, rootPath))
			return false;
		return true;
		/*for (UpsSystem system : allSystems) {
			if (StringUtils.equalsIgnoreCase(system.getPath(), rootPath))
				return true;
		}
		return false;*/
	}

	/**
	 * <pre>
	 * 根据系统标识路径获取后端代理服务器URI
	 * 不饮食 ups 自身系统
	 *
	 * @param request
	 * @return
	 */
	public URI getProxyUri(HttpServletRequest request) {
		String rootPath = UpsClientUtil.getRootPath(request);

		if (StringUtils.isBlank(rootPath)) {
			LOGGER.error("获取根路径名称为空");
			return null;
		}
		if (StringUtils.equalsIgnoreCase(UpsConstant.UPS_SYSTEM_PATH, rootPath)) {
			return null;
		}
		String server = null;
		switch (proxyServerPlan) {
		case LOCAL:
			server = "127.0.0.1:" + proxyPort;
			break;
		case REMOTE:
			server = getProxyUriFormRemote(request);
			break;
		case ZOOKEEPER:
		default:
			// TODO: 2016/10/28  zk 支持
			/*ClusterServerController serverCtrl = this.findServerCtrl(request);
			if (serverCtrl == null) {
				throw new ApiException(UpsErrorMessage.USER_NO_LOGIN);
			}
			NodeValue serverNode;
			if (proxyFilter) {
				serverNode = filterAvailableAppNode(serverCtrl, rootPath);
			} else {
				serverNode = serverCtrl.appServer(rootPath).roundRobin();
			}

			if (serverNode != null) {
				server = serverNode.getName();
			}
			if (StringUtils.isBlank(server)) {
				logger.error("无法通过 {} 获取代理服务地址，注册中心地址 : {}", proxyServerPlan.name(), serverCtrl.getClient()
				                                                                               .getConnectionString());
				return null;
			}
			break;*/
		}
		try {
			return new URI(StringUtils.join("http://", server));
		} catch (URISyntaxException e) {
			LOGGER.error("无法为 {} 主机地址生成URI：{}", server, e.getMessage());
			return null;
		}
	}

	private String getProxyUriFormRemote(HttpServletRequest request) {
		String host = request.getRemoteHost();
		if (StringUtils.startsWith(host, "0")) {// 以0开头，表示通过回环IP进行访问
			host = "127.0.0.1:" + proxyPort;
		} else {
			host += ":" + proxyPort;
		}

		return host;
	}

}
