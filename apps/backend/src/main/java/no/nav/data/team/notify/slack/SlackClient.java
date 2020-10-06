package no.nav.data.team.notify.slack;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import no.nav.data.common.exceptions.NotFoundException;
import no.nav.data.common.exceptions.TechnicalException;
import no.nav.data.common.utils.JsonUtils;
import no.nav.data.common.utils.MetricUtils;
import no.nav.data.common.web.TraceHeaderRequestInterceptor;
import no.nav.data.team.notify.slack.dto.SlackDtos.CreateConversationRequest;
import no.nav.data.team.notify.slack.dto.SlackDtos.CreateConversationResponse;
import no.nav.data.team.notify.slack.dto.SlackDtos.PostMessageRequest;
import no.nav.data.team.notify.slack.dto.SlackDtos.PostMessageRequest.Block;
import no.nav.data.team.notify.slack.dto.SlackDtos.PostMessageResponse;
import no.nav.data.team.notify.slack.dto.SlackDtos.Response;
import no.nav.data.team.notify.slack.dto.SlackDtos.UserResponse;
import no.nav.data.team.resource.NomClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class SlackClient {

    private static final String LOOKUP_BY_EMAIL = "/users.lookupByEmail?email={email}";
    private static final String OPEN_CONVERSATION = "/conversations.open";
    private static final String POST_MESSAGE = "/chat.postMessage";

    private final RestTemplate restTemplate;
    private final LoadingCache<String, String> userIdCache;
    private final LoadingCache<String, String> conversationCache;

    public SlackClient(RestTemplateBuilder restTemplateBuilder, SlackProperties properties) {
        restTemplate = restTemplateBuilder
                .additionalInterceptors(TraceHeaderRequestInterceptor.correlationInterceptor())
                .rootUri(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                .build();

        this.userIdCache = MetricUtils.register("slackUserIdCache",
                Caffeine.newBuilder().recordStats()
                        .expireAfterAccess(Duration.ofMinutes(60))
                        .maximumSize(100).build(this::doGetUserId));
        this.conversationCache = MetricUtils.register("slackConversationCache",
                Caffeine.newBuilder().recordStats()
                        .expireAfterAccess(Duration.ofMinutes(60))
                        .maximumSize(100).build(this::doOpenConversation));
    }

    public String getUserIdByIdent(String ident) {
        var email = NomClient.getInstance().getByNavIdent(ident).orElseThrow().getEmail();
        return getUserIdByEmail(email);
    }

    public String getUserIdByEmail(String email) {
        return userIdCache.get(email);
    }

    public String openConversation(String channelId) {
        return conversationCache.get(channelId);
    }

    private String doGetUserId(String email) {
        try {
            var response = restTemplate.getForEntity(LOOKUP_BY_EMAIL, UserResponse.class, email);
            UserResponse user = checkResponse(response);
            return user.getUser().getId();
        } catch (Exception e) {
            if (e.getMessage().contains("users_not_found")) {
                log.debug("Couldn't find user for email {}", email);
                return null;
            }
            throw new TechnicalException("Failed to get userId for " + email, e);
        }
    }

    public void sendMessage(String email, List<Block> blocks) {
        try {
            var userId = getUserIdByEmail(email);
            if (userId == null) {
                throw new NotFoundException("Couldn't find slack user for email" + email);
            }
            var channel = openConversation(userId);
            sendMessageToChannel(channel, blocks);
        } catch (Exception e) {
            throw new TechnicalException("Failed to send message to " + email + " " + JsonUtils.toJson(blocks), e);
        }
    }

    private void sendMessageToChannel(String channel, List<Block> blockKit) {
        var request = new PostMessageRequest(channel, blockKit);
        var response = restTemplate.postForEntity(POST_MESSAGE, request, PostMessageResponse.class);
        try {
            checkResponse(response);
        } catch (Exception e) {
            throw new TechnicalException("Failed to send message to channel " + channel, e);
        }
    }

    private String doOpenConversation(String userId) {
        try {
            var response = restTemplate.postForEntity(OPEN_CONVERSATION, new CreateConversationRequest(userId), CreateConversationResponse.class);
            CreateConversationResponse create = checkResponse(response);
            return create.getChannel().getId();
        } catch (Exception e) {
            throw new TechnicalException("Failed to get channel for " + userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T checkResponse(ResponseEntity<? extends Response> response) {
        Assert.notNull(response.getBody(), "empty body");
        Assert.isTrue(response.getBody().isOk(), "Not ok error: " + response.getBody().getError());
        return (T) response.getBody();
    }
}