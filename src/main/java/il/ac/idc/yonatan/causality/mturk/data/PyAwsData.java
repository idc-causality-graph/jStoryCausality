package il.ac.idc.yonatan.causality.mturk.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import il.ac.idc.yonatan.causality.mturk.RequestTypes;
import lombok.Data;
import lombok.ToString;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
@ToString(exclude = "awsSecretKey")
public class PyAwsData {

    @JsonProperty("aws_key")
    private String awsKey;
    @JsonProperty("aws_secret_key")
    private String awsSecretKey;
    @JsonProperty("is_sandbox")
    private boolean sandbox;

    @JsonProperty("request")
    private RequestTypes requestType;

    @JsonProperty("request_data")
    private RequestData requestData;

}
