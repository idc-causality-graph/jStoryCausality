package il.ac.idc.yonatan.causality.contexttree;

import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ygraber on 2/2/17.
 */
@Data
public class DownHitReviewData {
    private String hitId;
    private String nodeId;
    private String nodeSummary;
    private List<Pair<String, Integer>> ranks = new ArrayList<>();
    private boolean hitDone;
    public String getRankList() {
        return ranks.stream().map(p -> p.getRight().toString()).collect(Collectors.joining(","));
    }

}
