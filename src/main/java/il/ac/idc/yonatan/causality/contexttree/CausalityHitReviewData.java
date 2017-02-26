package il.ac.idc.yonatan.causality.contexttree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ygraber on 2/10/17.
 */
@Data
public class CausalityHitReviewData {

    private String hitId;
    private String assignmentId;
    // Is the answers the worker replayed are consistent with each other
    private boolean consistentAnswers;

    @Setter(AccessLevel.NONE)
    private List<CausalityData> causalityDataList = new ArrayList<>();

    @Data
    public static class CausalityData {

        private String queryNodeId;
        private String queryText;

        /**
         * Cotains the triplet: <NODE_ID, NODE_SUMMARY, IS_CAUSE_FOR_QUERY_NODE?>
         */
        @Setter(AccessLevel.NONE)
        private List<Triple<String, String, Boolean>> causeNodesTextRelations = new ArrayList<>();

    }
}
