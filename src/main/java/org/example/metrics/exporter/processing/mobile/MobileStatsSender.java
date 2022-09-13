package org.example.metrics.exporter.processing.mobile;

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
import org.example.metrics.exporter.model.mob.AppMetricResponse;
import org.example.metrics.exporter.model.mob.AppmetricaRequestModel;
import org.example.metrics.exporter.properties.mob.AppmetricaProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobileStatsSender {

    private static final int SUCCESS_STATUS_CODE = 200;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    private final AppmetricaProperties appmetricaProperties;

    public MetricResponseModel sendRequest(AppmetricaRequestModel requestModel) {
        try {
            var request = buildRequest(requestModel);
            var response = httpClient.execute(request);

            if (SUCCESS_STATUS_CODE != response.getStatusLine().getStatusCode()) {
                log.warn("For filter [{}] response received with status failed ({}). Date interval [{};{}]", requestModel.getFilters(),
                        response.getStatusLine().getStatusCode(), requestModel.getDate1(), requestModel.getDate2());
                return sendRequest(requestModel);
            }
            return processResponse(response.getEntity(), requestModel);
        } catch (Exception e) {
            log.warn("Failed to send request", e);
            return null;
        }
    }

    private MetricResponseModel processResponse(HttpEntity entity, AppmetricaRequestModel requestModel) throws IOException {
        var stringBody = EntityUtils.toString(entity);
        var parsedEntity = objectMapper.readValue(stringBody, AppMetricResponse.class);

        return parsedEntity.getData().stream()
                .map(data -> new MetricResponseModel()
                        .setAlias(requestModel.getAlias())
                        .setValues(data.getMetrics().get(0)))
                .findAny().get();
    }

    private HttpUriRequest buildRequest(AppmetricaRequestModel request) throws URISyntaxException {
        return RequestBuilder.get()
                .setUri(buildUri(request))
                .setHeader(AUTHORIZATION, "OAuth " + appmetricaProperties.getOAuthToken())
                .build();
    }

    private URI buildUri(AppmetricaRequestModel request) throws URISyntaxException {
        return new URIBuilder(appmetricaProperties.getHost() + appmetricaProperties.getStatsPath())
                .addParameter("id", request.getId())
                .addParameter("date1", request.getDate1())
                .addParameter("date2", request.getDate2())
                .addParameter("group", request.getGroup())
                .addParameter("metrics", request.getMetrics())
                .addParameter("include_undefined", request.getIncludeUndefined())
                .addParameter("accuracy", request.getAccuracy())
                .addParameter("proposedAccuracy", request.getProposedAccuracy())
                .addParameter("row_ids", "[]")
                .addParameter("filters", request.getFilters())
                .build();
    }
}
