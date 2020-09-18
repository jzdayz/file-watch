package com.wonders;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("wf")
@Data
public class WfProperties {
    private String path;
    private int savedDays = 2;
}
