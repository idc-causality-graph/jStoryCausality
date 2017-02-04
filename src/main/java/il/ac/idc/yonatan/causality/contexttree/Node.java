package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Multisets;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static java.util.stream.Collectors.toList;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
@JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
public class Node {

    static ContextTree contextTree;

    private String id;

    private String parentNodeId;

    private boolean leaf;

    @Setter(AccessLevel.NONE)
    private List<String> summaries = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<String> upHitIds = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<String> downHitIds = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private Set<String> completedUpHitIds = new HashSet<>();

    @Setter(AccessLevel.NONE)
    private Set<String> completedDownHitIds = new HashSet<>();

    /**
     * A list of texts describing the most important event of this node.
     * Each text is a text written by a unique worker in the down phase
     */
    @Setter(AccessLevel.NONE)
    private List<String> mostImportantEvents = new ArrayList<>();

    private LinkedHashMap<String, List<String>> upHitTaskData;

    /**
     * A list of summary indexes votes. That is, if the list is [0,3,3,2], it means summary #3 got 2 votes, summary
     * #0 got 1 vote, summary #2 got 1 vote, and summary #1 didn't get any votes. Probably best summary is #3.
     */
    @Setter(AccessLevel.NONE)
    private List<Integer> bestSummaryVotes = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<Integer> eventImportanceScores = new ArrayList<>();

    public boolean isDownPhaseDone() {
        //No down phase for root.
        return parentNodeId==null || (downHitIds.size() > 0 && downHitIds.size() == completedDownHitIds.size());
    }

    private List<String> childIds = new ArrayList<>();

    private Node getNode(String nodeId) {
        return contextTree.getNodeRepository().get(nodeId);
    }

    public List<Node> getChildren() {
        return childIds.stream().map(this::getNode).collect(toList());
    }

    public Node getParent() {
        return getNode(parentNodeId);
    }

    public boolean isUpPhaseDone() {
        return upHitIds.size() > 0 && upHitIds.size() == completedUpHitIds.size();
    }

    public double getAverageImportanceScore() {
        return eventImportanceScores.stream().collect(Collectors.averagingInt(x -> x));
    }

    public double getNormAverageImportanceScore() {
        return Math.max((getAverageImportanceScore() - 1) / 6, 0);
    }

    /**
     * Get the summary with most votes. In case more than one has the most amount of votes, elect the one with the
     * lowest index.
     * In case no votes were casted, null is returned
     *
     * @return the most voted summary, or null if no votes were casted
     */
    public String getBestSummary() {
        Integer vote = getBestSummaryIdx();
        if (vote==null){
            return null;
        }
        return summaries.get(vote);
    }

    public Integer getBestSummaryIdx(){
        if (bestSummaryVotes.isEmpty()) {
            return null;
        }
        return Multisets.copyHighestCountFirst(ImmutableSortedMultiset.copyOf(bestSummaryVotes))
                .iterator().next();
    }


}
