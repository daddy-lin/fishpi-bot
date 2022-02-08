package io.dannio.fishpi.config;

import io.dannio.fishpi.service.ChatroomService;
import io.github.danniod.fish4j.api.FishApi;
import io.github.danniod.fish4j.api.FishApiImpl;
import io.github.danniod.fish4j.client.WebSocketClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Configuration
public class FishApiConfig {

    private static final int RECONNECT_DELAYS[] = {10000, 60000, 180000, 300000};
    private int reconnectTimes = 0;

    @Bean
    public FishApi fishApi(Retrofit retrofit) {
        return new FishApiImpl(retrofit);
    }


    @Bean
    public Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .client(client)
                .baseUrl("https://fishpi.cn/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }


    @Bean
    public WebSocket webSocket(OkHttpClient client, WebSocketClient webSocketClient) {
        Request request = new Request.Builder()
                .url("https://fishpi.cn/chat-room-channel?apiKey=FgKt3UMtyNiimukgWBqYyzJp4VrUPKVd")
                .build();
        return client.newWebSocket(request, webSocketClient);
    }


    @Bean
    public WebSocketClient webSocketClient(ChatroomService service) {
        return new WebSocketClient((webSocket, response) -> {
            webSocket.send("-hb-");
            reconnectTimes = 0;
        }, (webSocket, i, s) -> {
            webSocket.close(i, s);
            reconnect(service);
        }, (webSocket, throwable, response) -> {
            log.warn("websocket broken. onFailure", throwable);
            reconnect(service);
        }, (webSocket, text) -> service.messageToTelegram(text));
    }


    @SneakyThrows
    private void reconnect(ChatroomService service) {
        if (reconnectTimes >= RECONNECT_DELAYS.length) {
            return;
        }
        Thread.sleep(RECONNECT_DELAYS[reconnectTimes++]);
        webSocketClient(service);
    }


    @Bean
    public OkHttpClient customUaClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(chain.request()
                        .newBuilder()
                        .removeHeader("User-Agent")
                        .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36")
                        .build())
                ).build();
    }


}
