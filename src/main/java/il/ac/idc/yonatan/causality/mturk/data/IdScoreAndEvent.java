package il.ac.idc.yonatan.causality.mturk.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ygraber on 2/18/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdScoreAndEvent {
    private String nodeId;
    private Integer score;
    private String mostImportantEvent;
}
