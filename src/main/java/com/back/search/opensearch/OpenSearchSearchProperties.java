package com.back.search.opensearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search.opensearch")
public class OpenSearchSearchProperties {
    private boolean enabled = false;
    private boolean indexOnStartup = false;
    private String url = "http://localhost:9200";
    private String indexName = "toptop_search";
    private String username = "";
    private String password = "";
}
