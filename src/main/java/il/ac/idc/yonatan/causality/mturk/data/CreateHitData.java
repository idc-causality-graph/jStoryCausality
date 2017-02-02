package il.ac.idc.yonatan.causality.mturk.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
public class CreateHitData implements RequestData {
    @JsonProperty("question_html")
    private String questionHtml;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("reward")
    private double reward;
    @JsonProperty("duration_minutes")
    private int durationMinutes;
    @JsonProperty("lifetime_minutes")
    private int lifetimeMinutes;
    @JsonProperty("number_of_duplications")
    private int numberOfDuplications;
    @JsonProperty("keywords_array")
    private List<String> keywords;
    @JsonProperty("annotation")
    private String annotation;

}
