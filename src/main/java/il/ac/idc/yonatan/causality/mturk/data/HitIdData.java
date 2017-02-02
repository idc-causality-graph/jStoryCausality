package il.ac.idc.yonatan.causality.mturk.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
public class HitIdData implements RequestData{
    @JsonProperty("hit_id")
    private String hitId;
}
