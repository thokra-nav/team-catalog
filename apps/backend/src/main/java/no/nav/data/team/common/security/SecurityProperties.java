package no.nav.data.team.common.security;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static no.nav.data.team.common.utils.StreamUtils.safeStream;

@Data
@Configuration
@ConfigurationProperties(prefix = "team-catalog.security")
public class SecurityProperties {

    private boolean enabled = true;
    private String allowedAppIdMappings = "";
    private String encKey = "";
    private String identClaim = "";
    private List<String> writeGroups;
    private List<String> adminGroups;
    private List<String> redirectUris;

    public boolean isValidRedirectUri(String uri) {
        return uri == null || safeStream(redirectUris).anyMatch(origin -> StringUtils.startsWithIgnoreCase(uri, origin));
    }
}