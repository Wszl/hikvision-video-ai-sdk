package org.xdove.thridpart.hikvision.video.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xdove.thridpart.hikvision.video.ai.exception.InitAuthException;

import com.alibaba.fastjson.JSONObject;

import lombok.Setter;

/**
 * 海康视频AI服务
 * @author Wszl
 * @date 2022年04月20日
 */
public class ServiceRequests {

    private final static Logger log = LogManager.getLogger();

    private final Config config;
    private String name;
    private RequestConfig requestConfig;
    private HttpClient client;
    @Setter
    private String token;
    @Setter
    private Function<Map<String, Object>, Void> outerStorageCallback;

    /** 获取 Token */
    private final String PATH_AUTH = "/auth/login/password";
    /** 订阅/取消接口 */
    private final String PATH_SUBSCRIBE = "/analysis/subscription";
    /** 告警历史数据查询 */
    private final String PATH_HISTORY_WARNING = "/analysis/analysiswarninfo/listPage";
    /** 设备信息查询 */
    private final String PATH_DEVICE_INFO = "/analysis/analysisdevice/getInfoByDept";
    /** 视屏实时流地址查询 */
    private final String PATH_VIDEO_STREAM = "/analysis/analysisdevice/getVideoByUid";

    public ServiceRequests(@NotNull Config config) {
        this.config = config;
        this.name = "Default";
        this.requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(1000)
                .setConnectTimeout(5000)
                .build();
        this.client = HttpClientBuilder.create().build();
    }

    public ServiceRequests(@NotNull Config config, String name, RequestConfig requestConfig) {
        this(config);
        this.name = name;
        this.requestConfig = requestConfig;
    }

    public void initAuth(String phone, String password) {
        if (Objects.isNull(token)) {
            Map<String, Object> ret = requestAuth(phone, password);
            Map<String, Object> data = (Map<String, Object>) ret.get("data");
            if (Objects.isNull(data)) {
                log.warn("init auth failed, data not found. phone={} password={}, ret={}", phone, password, ret);
                throw new InitAuthException("init auth failed");
            }
            String token = (String) data.get("jwtToken");
            if (Objects.isNull(token)) {
                log.warn("init auth failed, token not found. phone={} password={}, ret={}", phone, password, ret);
                throw new InitAuthException("init auth failed");
            }
            this.token = token;
            if (Objects.nonNull(outerStorageCallback)) {
                outerStorageCallback.apply(ret);
            }
        }
    }

    /**
     * 第三方平台根据账号密码获取 Token
     * @param phone 账户
     * @param password 密码
     * @return
     */
    public Map<String, Object> requestAuth(final @NotNull String phone, final @NotNull String password) {
        Map<String, String> param = new HashMap<>(2);
        param.put("phone", phone);
        param.put("password", password);
        return postRequest(PATH_AUTH, param);
    }

    /**
     * 订阅/取消接口
     * 第三方平台携带token请求接口,并传入接受的完整url路径
     * @param url 完整发送路径 例 http://127.0.0.1:8888/api
     * @param type 1订阅 2取消订阅
     * @return 
     */
    public Map<String, Object> requestSubscription(@NotNull final String url, @NotNull final String type) {
        Map<String, String> param = new HashMap<>(2);
        param.put("url", url);
        param.put("type", type);
        return postRequest(PATH_SUBSCRIBE, param);
    }

    /**
     * 告警历史数
     * 告警数据查询(分页)
     * @param type 告警类型
     * @param deviceId 设备唯一编码
     * @param startDateTime 开始时间yyMMdd
     * @param endDateTime 结束时间yyMMdd (结束时间要与开始时间一起传)
     * @param startPage 从第几页开始
     * @param pageSize 每页条数
     * @return
     */
    public Map<String, Object> requestHistoryWarning(@NotNull final String type, @NotNull final String deviceId, final String startDateTime, final String endDateTime,
                                                   final @NotNull String startPage, final @NotNull String pageSize) {
        Map<String, String> param = new HashMap<>(6);
        param.put("type", type);
        param.put("deviceId", deviceId);
        param.put("startDateTime", startDateTime);
        param.put("endDateTime", endDateTime);
        param.put("startPage", startPage);
        param.put("pageSize", pageSize);
        return getRequest(PATH_HISTORY_WARNING, param);
    }

    /**
     * 设备信息查询
     * 设备信息查询(分页)
     * @param name 设备名称,模糊查询
     * @param deviceId 设备唯一编码
     * @param pageSize 每页条数
     * @param startPage 从第几页开始
     * @return 
     */
    public Map<String, Object> requestDeviceInfo(final String name, final String deviceId, final @NotNull String startPage, final @NotNull String pageSize) {
        Map<String, String> param = new HashMap<>(6);
        param.put("pageSize", pageSize);
        param.put("startPage", startPage);
        param.put("name", name);
        param.put("deviceId", deviceId);
        return getRequest(PATH_DEVICE_INFO, param);
    }

    /**
     * 视屏实时流地址查询
     * @param uid 设备唯一编码(查询设备信息查询到的uid)
     * @return 
     */
    public Map<String, Object> requestVideoStream(@NotNull final String uid) {
        Map<String, String> param = new HashMap<>(1);
        param.put("uid", uid);
        return getRequest(PATH_VIDEO_STREAM, param);
    }

    private String combPath(final String path, Map<String, String> p) {
        StringBuilder queryParam = new StringBuilder();
        if (Objects.nonNull(p)) {
            queryParam.append("?");
            p.forEach((k, v) -> {
                queryParam.append("&")
                        .append(k)
                        .append("=")
                        .append(v);
            });
        }
        return config.getHost() + ":" + config.getPort() + config.getUrlPrefix() + path + queryParam;
    }

    private Map<String, Object> handleJsonResponse(final String content) {
        Objects.requireNonNull(content);
        JSONObject json = JSONObject.parseObject(content);
        if (json.containsKey("code") && json.getString("code").equals("200")) {
            return json.getInnerMap();
        } else {
            log.info(json.getString("msg"));
            throw new RuntimeException(json.getString("msg"));
        }
    }

    private Map<String, Object> getRequest(String path, Map<String, String> p) {
        HttpResponse response = null;
        try {
            String url = combPath(path, p);
            HttpGet get = new HttpGet(url);
            get.setConfig(this.requestConfig);
            get.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            if (Objects.nonNull(this.token)) {
                get.setHeader(HttpHeaders.AUTHORIZATION, this.token);
            }

            if (log.isDebugEnabled()) {
                log.debug("get request path=[{}], headers=[{}]", get.getURI().toString(), get.getAllHeaders());
            }

            response = client.execute(get);
            String respContent = readInputStream(response.getEntity().getContent(),
                    Objects.isNull(response.getEntity().getContentEncoding()) ? config.getCharset() : response.getEntity().getContentEncoding().getValue());
            if (log.isDebugEnabled()) {
                log.debug("path=[{}], params=[{}], response status=[{}] content=[{}]", path, p,
                        response.getStatusLine().getStatusCode(), respContent);
            }
            return handleJsonResponse(respContent);
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (Objects.nonNull(response)) {
                    EntityUtils.consume(response.getEntity());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, Object> postRequest(String path, Map<String, String> p) {
        HttpResponse response = null;
        try {
            String url = combPath(path, null);
            final HttpEntity body = combBody(p);
            HttpPost post = new HttpPost(url);
            post.setConfig(this.requestConfig);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            if (Objects.nonNull(this.token)) {
                post.setHeader(HttpHeaders.AUTHORIZATION, this.token);
            }
            post.setEntity(body);

            if (log.isDebugEnabled()) {
                log.debug("post request path=[{}], headers=[{}] body=[{}]",
                        post.getURI().toString(), post.getAllHeaders(), readInputStream(body.getContent(), config.getCharset()));
            }

            response = client.execute(post);
            String respContent = readInputStream(response.getEntity().getContent(),
                    Objects.isNull(response.getEntity().getContentEncoding()) ? config.getCharset() : response.getEntity().getContentEncoding().getValue());
            if (log.isDebugEnabled()) {
                log.debug("path=[{}], params=[{}], response status=[{}] content=[{}]", path, p,
                        response.getStatusLine().getStatusCode(), respContent);
            }
            return handleJsonResponse(respContent);
        } catch (Exception e) {
            log.info("path=[{}], params=[{}] error.", path, p, e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (Objects.nonNull(response)) {
                    EntityUtils.consume(response.getEntity());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private HttpEntity combBody(Map<String, String> p) throws UnsupportedEncodingException {
        p.entrySet().removeIf((e) -> Objects.isNull(e.getValue()));
        return new UrlEncodedFormEntity(p.entrySet().stream()
                .map(e -> new BasicNameValuePair(e.getKey() ,e.getValue()))
                .collect(Collectors.toList()), config.getCharset()
        );
    }

    private String readInputStream(InputStream i, String encoding) throws IOException {
        return IOUtils.toString(i, Objects.isNull(encoding) ? config.getCharset() : encoding);
    }

}
