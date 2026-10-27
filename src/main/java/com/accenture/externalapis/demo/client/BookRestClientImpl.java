package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);

            if (response == null) {
                throw new ClientException("Book service returned an empty response for book " + id);
            }

            return toDto(response);

        } catch (HttpClientErrorException ex) {
            throw new ClientException("Client error while fetching book " + id + ": " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new ClientException("Book service error while fetching book " + id + ": " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            throw new ClientException("Book service is unreachable or timed out (book " + id + ")", ex);
        } catch (RestClientException ex) {
            throw new ClientException("Unexpected response from book service for book " + id, ex);
        }
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
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] responses = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);

            if (responses == null) {
                throw new ClientException("Book service returned an empty response for books");
            }

            return Arrays.stream(responses)
                    .map(this::toDto)
                    .toList();

        } catch (HttpClientErrorException ex) {
            throw new ClientException("Client error while fetching books: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new ClientException("Book service error while fetching books: " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            throw new ClientException("Book service is unreachable or timed out", ex);
        } catch (RestClientException ex) {
            throw new ClientException("Unexpected response from book service for all books", ex);
        }
    }
}
