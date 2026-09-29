package com.aicareer.jobradar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.scrapers")
public class ScraperProperties {

    private List<String> greenhouseSlugs = new ArrayList<>();
    private List<String> leverSlugs = new ArrayList<>();
    private List<String> ashbySlugs = new ArrayList<>();
    private boolean remoteokEnabled = true;
    private String remoteokTags = "java,python,react,node,typescript,golang";
    private boolean remotiveEnabled = true;
    private boolean workableEnabled = true;
}
