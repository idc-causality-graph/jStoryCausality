package il.ac.idc.yonatan.causality.mturk.data;

import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

/**
 * Created by ygraber on 2/2/17.
 */
@Data
public class DownHitResult {
    private boolean hitDone;
    private List<IdScoreAndEvent> idsAndScoresAndEvents;
}
