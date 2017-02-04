package il.ac.idc.yonatan.causality.contexttree;

import lombok.Data;

import java.util.List;

/**
 * Created by ygraber on 2/2/17.
 */
@Data
public class DownHitReviewData {
    private String hitId;
    private String nodeId;
    private List<String> rootNodeSummaries;
    private List<String> nodeSummaries;
    private boolean hitDone;
    private Integer importanceScore;
    private String mostImportantEvent;

}
