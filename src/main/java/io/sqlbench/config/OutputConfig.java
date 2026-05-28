package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OutputConfig {
    private String localDir = "./benchmark_results";
    private boolean uploadToS3 = false;
    private String s3Bucket;
    private String s3Prefix = "results/";
    private boolean slackEnabled = false;
    private String slackWebhook;

    public String getLocalDir() { return localDir; }
    public void setLocalDir(String localDir) { this.localDir = localDir; }
    public boolean isUploadToS3() { return uploadToS3; }
    public void setUploadToS3(boolean uploadToS3) { this.uploadToS3 = uploadToS3; }
    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }
    public String getS3Prefix() { return s3Prefix; }
    public void setS3Prefix(String s3Prefix) { this.s3Prefix = s3Prefix; }
    public boolean isSlackEnabled() { return slackEnabled; }
    public void setSlackEnabled(boolean slackEnabled) { this.slackEnabled = slackEnabled; }
    public String getSlackWebhook() { return slackWebhook; }
    public void setSlackWebhook(String slackWebhook) { this.slackWebhook = slackWebhook; }
}
