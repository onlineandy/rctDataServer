package com.opensource.rct.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "rct")
public class ConfigParameter {

    @NotNull
    /**
     * IP of the inverter
     */
    private String hostname;

    private int port = 8899;
    private Long timeoutConverter = 3000L;

    private String hostnameInfluxDb = "";
    private int portInfluxDb = 8086;

    private int panelPower = 0;
    private int panelsA = 0;
    private int panelsB = 0;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Long getTimeoutConverter() {
        return timeoutConverter;
    }

    public void setTimeoutConverter(Long timeoutConverter) {
        this.timeoutConverter = timeoutConverter;
    }

    public String getHostnameInfluxDb() {
        return hostnameInfluxDb;
    }

    public void setHostnameInfluxDb(String hostnameInfluxDb) {
        this.hostnameInfluxDb = hostnameInfluxDb;
    }

    public int getPortInfluxDb() {
        return portInfluxDb;
    }

    public void setPortInfluxDb(int portInfluxDb) {
        this.portInfluxDb = portInfluxDb;
    }

    public int getPanelPower() {
        return panelPower;
    }

    public void setPanelPower(int panelPower) {
        this.panelPower = panelPower;
    }

    public int getPanelsA() {
        return panelsA;
    }

    public void setPanelsA(int panelsA) {
        this.panelsA = panelsA;
    }

    public int getPanelsB() {
        return panelsB;
    }

    public void setPanelsB(int panelsB) {
        this.panelsB = panelsB;
    }
}
