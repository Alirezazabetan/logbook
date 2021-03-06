package org.zalando.logbook.httpclient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.zalando.logbook.Headers;
import org.zalando.logbook.Origin;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.http.util.EntityUtils.toByteArray;

final class RemoteResponse implements org.zalando.logbook.HttpResponse {

    private final HttpResponse response;
    private byte[] body;

    RemoteResponse(final HttpResponse response) {
        this.response = response;
    }

    @Override
    public Origin getOrigin() {
        return Origin.REMOTE;
    }

    @Override
    public String getProtocolVersion() {
        return response.getStatusLine().getProtocolVersion().toString();
    }

    @Override
    public int getStatus() {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        final Map<String, List<String>> headers = Headers.empty();

        Stream.of(response.getAllHeaders())
                .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())))
                .forEach(headers::put);

        // TODO immutable?
        return headers;
    }

    @Override
    public String getContentType() {
        return Optional.of(response)
                .map(request -> request.getFirstHeader("Content-Type"))
                .map(Header::getValue)
                .orElse("");
    }

    @Override
    public Charset getCharset() {
        return Optional.of(response)
                .map(request -> request.getFirstHeader("Content-Type"))
                .map(Header::getValue)
                .map(ContentType::parse)
                .map(ContentType::getCharset)
                .orElse(UTF_8);
    }

    @Override
    public org.zalando.logbook.HttpResponse withBody() throws IOException {
        if (body == null) {
            @Nullable final HttpEntity entity = response.getEntity();

            if (entity == null) {
                return withoutBody();
            } else {
                this.body = toByteArray(entity);

                final ByteArrayEntity copy = new ByteArrayEntity(body);
                copy.setChunked(entity.isChunked());
                copy.setContentEncoding(entity.getContentEncoding());
                copy.setContentType(entity.getContentType());

                response.setEntity(copy);
            }
        }

        return this;
    }

    @Override
    public RemoteResponse withoutBody() {
        this.body = new byte[0];
        return this;
    }

    @Override
    public byte[] getBody() {
        return body == null ? new byte[0] : body;
    }

}
