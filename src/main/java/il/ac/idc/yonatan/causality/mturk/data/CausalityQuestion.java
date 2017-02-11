package il.ac.idc.yonatan.causality.mturk.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Data
@AllArgsConstructor
@JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
public class CausalityQuestion {

    private String question;
    @Setter(AccessLevel.NONE)
    private List<CauseAndNodeId> causes = new ArrayList<>();
    private String questionNodeId;

    @Data
    @AllArgsConstructor
    @JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
    public static class CauseAndNodeId {
        private String text;
        private String nodeId;
    }

}
