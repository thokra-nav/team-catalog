package no.nav.data.team.common.utils;

import no.nav.data.team.common.security.dto.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class MdcUtils {

    private MdcUtils() {
    }

    private static final String CORRELATION_ID = "correlationId";
    private static final String CALL_ID = "callId";
    private static final String USER_ID = "userId";
    private static final String CONSUMER_ID = "consumerId";
    private static final String REQUEST_PATH = "requestPath";

    private static String createUUID() {
        return UUID.randomUUID().toString();
    }

    public static String getOrGenerateCorrelationId() {
        return Optional.ofNullable(MDC.get(CORRELATION_ID)).orElse(createUUID());
    }

    public static void createCorrelationId() {
        MDC.put(CORRELATION_ID, createUUID());
    }

    public static void clearCorrelationId() {
        MDC.remove(USER_ID);
    }

    public static String getCallId() {
        return MDC.get(CALL_ID);
    }

    public static void setCallId(String correlationId) {
        MDC.put(CALL_ID, correlationId);
    }

    public static void clearCallId() {
        MDC.remove(CALL_ID);
    }

    public static String getUser() {
        return MDC.get(USER_ID);
    }

    public static void setUserFromSecurity() {
        setUser(getCurrentSecurityContextUser());
    }

    public static void setUser(String user) {
        MDC.put(USER_ID, user);
    }

    public static void clearUser() {
        MDC.remove(USER_ID);
    }

    public static void setConsumerId(String consumer) {
        MDC.put(CONSUMER_ID, consumer);
    }

    public static void clearConsumer() {
        MDC.remove(CONSUMER_ID);
    }

    public static void setRequestPath(String path) {
        MDC.put(REQUEST_PATH, path);
    }

    public static void clearRequestPath() {
        MDC.remove(REQUEST_PATH);
    }


    public static Runnable wrapAsync(Runnable runnable, String user) {
        return () -> {
            setUser(user);
            setConsumerId(Constants.APP_ID);
            createCorrelationId();
            try {
                runnable.run();
            } finally {
                clearMdc();
            }
        };
    }

    private static String getCurrentSecurityContextUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(authentication -> authentication.getDetails() instanceof UserInfo ? (UserInfo) authentication.getDetails() : null)
                .map(userInfo -> StringUtils.isBlank(userInfo.getName()) ? userInfo.getAppName() : userInfo.formatUser())
                .orElse("no-auth");
    }

    private static void clearMdc() {
        createCorrelationId();
        clearCallId();
        clearUser();
        clearConsumer();
        clearRequestPath();
    }
}