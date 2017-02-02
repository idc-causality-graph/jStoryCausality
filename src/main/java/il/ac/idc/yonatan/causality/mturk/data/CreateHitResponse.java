package il.ac.idc.yonatan.causality.mturk.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateHitResponse {
    @JsonProperty("IsValid")
    private boolean valid;

    @JsonProperty("HITTypeId")
    private String hitTypeId;

    @JsonProperty("HITId")
    private String hitId;
}
