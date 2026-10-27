package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
public class BookWebClientImpl implements BookWebClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookApiResponse.class)
                .timeout(TIMEOUT)
                .switchIfEmpty(Mono.error(
                        new ClientException("Book service returned an empty response for book " + id)))
                .map(this::toDto)
                .onErrorMap(ex -> toClientException(ex, "book " + id));
    }

    private BookDto toDto(BookApiResponse response) {
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price()
        );
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .timeout(TIMEOUT)
                .map(this::toDto)
                .onErrorMap(ex -> toClientException(ex, "all books"));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        Mono<BookDto> bookOne = getBookAsync(id1);
        Mono<BookDto> bookTwo = getBookAsync(id2);

        return Mono.zip(bookOne, bookTwo)
                .map(tuple -> List.of(
                        tuple.getT1(),
                        tuple.getT2()
                ));
    }

    private ClientException toClientException(Throwable ex, String target) {
        if (ex instanceof ClientException clientException) {
            return clientException;
        }
        if (ex instanceof WebClientResponseException responseException) {
            return new ClientException("Book service returned "
                    + responseException.getStatusCode() + " for " + target, ex);
        }
        if (ex instanceof WebClientRequestException) {
            return new ClientException("Book service is unreachable (" + target + ")", ex);
        }
        if (ex instanceof TimeoutException) {
            return new ClientException("Book service timed out (" + target + ")", ex);
        }
        return new ClientException("Unexpected response from book service for " + target, ex);
    }
}
