package org.xdove.thridpart.hikvision.video.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public class ServiceRequestsTest {

    private ServiceRequests requests;
    private String token;

    @Before
    public void setUp() throws Exception {
        Config config = new Config(
                System.getenv("Host"),
                Integer.parseInt(System.getenv("Port")),
                System.getenv("phone"),
                System.getenv("password"));
        requests = new ServiceRequests(config);
        this.token = System.getenv("jwt");
        this.requests.setToken(this.token);
    }

    public static void main(String[] args) throws IOException {
        startHttpServer();
    }

    public static void startHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(18080), 10);
        httpServer.createContext("/callback", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String body = IOUtils.toString(exchange.getRequestBody(), Charset.defaultCharset());
                String url = exchange.getRequestURI().toString();
                System.out.printf("%s \t %s \n", url, body);
            }
        });
        httpServer.start();
    }

    @Test
    public void testRequestAuth() {
        String phone = System.getenv("phone");
        String password = System.getenv("password");
        Map<String, Object> ret = this.requests.requestAuth(phone, password);   
        System.out.println(ret);
    }

    @Test
    public void testRequestDeviceInfo() {
        String name = System.getenv("name");
        String deviceId = System.getenv("deviceId");
        String startPage = System.getenv("startPage");
        String pageSize = System.getenv("pageSize");
        Map<String, Object> ret = this.requests.requestDeviceInfo(name, deviceId, startPage, pageSize);   
        System.out.println(ret);
    }

    @Test
    public void testRequestHistoryWarning() {
        String type = System.getenv("type");
        String deviceId = System.getenv("deviceId");
        String startDateTime = System.getenv("startDateTime");
        String endDateTime = System.getenv("endDateTime");
        String startPage = System.getenv("startPage");
        String pageSize = System.getenv("pageSize");
        Map<String, Object> ret = this.requests.requestHistoryWarning(type, deviceId, startDateTime, endDateTime, startPage, pageSize);   
        System.out.println(ret);
    }

    @Test
    public void testRequestSubscription() {
        String url = System.getenv("url");
        String type = System.getenv("type");
        Map<String, Object> ret = this.requests.requestSubscription(url, type);   
        System.out.println(ret);
    }

    @Test
    public void testRequestVideoStream() {
        String uid = System.getenv("uid");
        Map<String, Object> ret = this.requests.requestVideoStream(uid);   
        System.out.println(ret);
    }


}