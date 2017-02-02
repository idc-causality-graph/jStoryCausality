package il.ac.idc.yonatan.causality.mturk.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetHitResponse {
    @JsonProperty("RequesterAnnotation")
    private String annotation;
    //Title
    @JsonProperty("Title")
    private String title;
    //Question
    @JsonProperty("Question")
    private String question;

    @JsonProperty("AssignmentDurationInSeconds")
    private int assignmentDurationInSeconds;

    @JsonProperty("HITId")
    private String hitId;

    @JsonProperty("HITTypeId")
    private String hitTypeId;

    @JsonProperty("Amount")
    private double amount;

    @JsonProperty("Expiration")
    private Date expiration;

    @JsonProperty("IsValid")
    private boolean isValid;

    @JsonProperty("HITStatus")
    private String hitStatus;
    //Assignable

    @JsonProperty("Keywords")
    private String keywords;

    @JsonProperty("HITReviewStatus")
    private String hitReviewStatus;
    //NotReviewed
}
