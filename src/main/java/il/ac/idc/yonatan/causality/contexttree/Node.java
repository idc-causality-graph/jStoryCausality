package il.ac.idc.yonatan.causality.contexttree;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
public class Node {

    private String id;

    private Integer chosenSummary;

    private List<String> summaries = new ArrayList<>();

    private String parentNodeId;

    private List<String> upHitIds = new ArrayList<>();

    private List<String> downHitIds = new ArrayList<>();

    private Set<String> completedUpHitIds = new HashSet<>();

    private Set<String> completedDownHitIds = new HashSet<>();

    private LinkedHashMap<String, List<String>> upHitTaskData;

    private List<Integer> summaryRates = new ArrayList<>();

    public double getAverageRate() {
        return summaryRates.stream().collect(Collectors.averagingInt(x -> x));
    }

    public double getNormalizedAverageRate() {
        return Math.max((getAverageRate() - 1) / 6, 0);
    }

    /**
     * Stores for each childId the chosen summaries by the workers
     */
    private Multimap<String, Integer> chosenSummariesForChildren = HashMultimap.create();

    private boolean leaf;

    private boolean upPhaseDone;

    public boolean isDownPhaseDone() {
        return downHitIds.size() == completedDownHitIds.size();
    }

    private List<String> childIds = new ArrayList<>();


}
