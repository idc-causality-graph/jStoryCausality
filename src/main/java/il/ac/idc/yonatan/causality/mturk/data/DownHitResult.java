package il.ac.idc.yonatan.causality.mturk.data;

import lombok.Data;

/**
 * Created by ygraber on 2/2/17.
 */
@Data
public class DownHitResult {
    private boolean hitDone;
    private Integer importanceScore;
    private String mostImportantEvent;
}
