package org.example.metrics.exporter.processing.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.example.metrics.exporter.model.MetricResponseModel;
import org.example.metrics.exporter.model.mob.AppMetricDimension;
import org.example.metrics.exporter.model.mob.AppMetricResponse;
import org.example.metrics.exporter.model.mob.AppmetricaRequestModel;
import org.example.metrics.exporter.properties.web.YandexMetrikaProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebStatsSender {

    private static final int SUCCESS_STATUS_CODE = 200;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    private final YandexMetrikaProperties yandexMetrikaProperties;

    public List<MetricResponseModel> sendRequest(AppmetricaRequestModel requestModel) {
        try {
            var request = buildRequest(requestModel);
            var response = httpClient.execute(request);

            if (SUCCESS_STATUS_CODE != response.getStatusLine().getStatusCode()) {
                log.warn("Response received with status failed. {} : {}", response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity()));
                return sendRequest(requestModel);
            }
            return processResponse(response.getEntity());
        } catch (Exception e) {
            log.warn("Failed to send request", e);
            return emptyList();
        }
    }

    private List<MetricResponseModel> processResponse(HttpEntity entity) throws IOException {
        var stringBody = EntityUtils.toString(entity);
        var parsedEntity = objectMapper.readValue(stringBody, AppMetricResponse.class);

        return parsedEntity.getData().stream()
                .map(data -> new MetricResponseModel()
                        .setMetricName(data.getDimensions().stream().map(AppMetricDimension::getName).collect(joining(" -> ")))
                        .setValues(data.getMetrics().get(0)))
                .collect(toList());
    }

    private HttpUriRequest buildRequest(AppmetricaRequestModel request) throws URISyntaxException {
        return RequestBuilder.get()
                .setUri(buildUri(request))
                .setHeader(AUTHORIZATION, "OAuth " + yandexMetrikaProperties.getOAuthToken())
                .build();
    }

    private URI buildUri(AppmetricaRequestModel request) throws URISyntaxException {
        return new URIBuilder(yandexMetrikaProperties.getHost() + yandexMetrikaProperties.getStatsPath())
                .addParameter("id", request.getId())
                .addParameter("date1", request.getDate1())
                .addParameter("date2", request.getDate2())
                .addParameter("group", request.getGroup())
                .addParameter("metrics", request.getMetrics())
                .addParameter("dimensions", request.getDimensions())
                .addParameter("include_undefined", request.getIncludeUndefined())
                .addParameter("accuracy", request.getAccuracy())
                .addParameter("proposedAccuracy", request.getProposedAccuracy())
                .addParameter("row_ids", request.getRows())
                .build();
    }
}
