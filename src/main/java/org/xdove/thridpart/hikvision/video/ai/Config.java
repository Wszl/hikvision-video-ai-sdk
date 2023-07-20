package org.xdove.thridpart.hikvision.video.ai;

import lombok.Data;

/**
 * ServiceRequests 配置类
 * @author Wszl
 * @date 2023-07-05
 */
@Data
public class Config {
    private String host;
    private int port;
    private String phone;
    private String password;
    private String urlPrefix;
    private String charset;

    public Config(String host, int port, String phone, String password) {
        this.host = host;
        this.port = port;
        this.phone = phone;
        this.password = password;
        defaultConfig();
    }
    private void defaultConfig() {
        this.urlPrefix = "/artemis";
    }

}
