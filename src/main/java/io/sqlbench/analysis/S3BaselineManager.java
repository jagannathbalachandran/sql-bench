package io.sqlbench.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqlbench.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class S3BaselineManager implements BaselineManager {
    private static final Logger LOG = LoggerFactory.getLogger(S3BaselineManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final S3Client s3;
    private final String bucket;
    private final String prefix;

    public S3BaselineManager(String bucket, String prefix) {
        this.s3 = S3Client.create();
        this.bucket = bucket;
        this.prefix = prefix.endsWith("/") ? prefix : prefix + "/";
    }

    @Override
    public Map<String, Long> loadBaseline(String suiteName) throws IOException {
        String key = prefix + "baselines/" + suiteName + ".json";
        try {
            byte[] bytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket).key(key).build()).asByteArray();
            return MAPPER.readValue(bytes, new TypeReference<>() {});
        } catch (NoSuchKeyException e) {
            LOG.info("No S3 baseline found at s3://{}/{}", bucket, key);
            return Map.of();
        } catch (Exception e) {
            throw new IOException("Failed to load baseline from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveBaseline(String suiteName, List<QueryResult> results) throws IOException {
        Map<String, Long> baseline = new LinkedHashMap<>();
        for (QueryResult r : results) {
            if (r.isSuccess()) baseline.put(r.getQueryAlias(), r.getTotalClientTimeMs());
        }
        String key = prefix + "baselines/" + suiteName + ".json";
        try {
            byte[] json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(baseline);
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                    RequestBody.fromBytes(json));
            LOG.info("Saved S3 baseline at s3://{}/{}", bucket, key);
        } catch (Exception e) {
            throw new IOException("Failed to save baseline to S3: " + e.getMessage(), e);
        }
    }
}
