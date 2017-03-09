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

    private String upHitId;

    private String downHitId;

    @Setter(AccessLevel.NONE)
    private Set<String> completedDownAssignmentsIds = new HashSet<>();

    @Setter(AccessLevel.NONE)
    private Set<String> completedUpAssignmentsIds = new HashSet<>();

    /**
     * Data related to constructing causalityGraph
     */
    private CausalityData causalityData = new CausalityData();

    /**
     * This is a normalized score of the worker-normalized scores the workers has given, with respect to the parent.
     * That is, if the average score is 0.7, the normalized score here will be <code>0.7*parent.normalizedImportanceScore</code>
     */
    private Double normalizedImportanceScore;

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

    /**
     * A list of scores, normalized in the context of the worker's other scores.
     * That is, if a worker has given to 3 siblings the scores [1,3,5], the normalized scores will be [1/5, 3/5, 5/5].
     * This list contains the workers scores of a specific child. So if this is the second child, and this is the first
     * worker which scores it, this list will contain a single element with value [3/5] (and the eveventImportanceScores
     * will contain the raw score - [3]
     */
    @Setter(AccessLevel.NONE)
    private List<Double> eventImportanceWorkerNormalizedScores = new ArrayList<>();

    public boolean isDownPhaseDone(int repFactor) {
        //No down phase for leafs.
        return isLeaf() || (completedDownAssignmentsIds.size() == repFactor);
    }

    public boolean isUpPhaseDone(int repFactor) {
        //No down phase for leafs.
        return isLeaf() || (completedUpAssignmentsIds.size() == repFactor);
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

    public boolean isMetaLeaf() {
        return !isLeaf() && getChildren().stream().allMatch(Node::isLeaf);
    }

    /**
     * This is the average [1-7] of the scores the workers has given
     *
     * @return
     */
    public double getAverageImportanceScore() {
        return eventImportanceWorkerNormalizedScores.stream().collect(Collectors.averagingDouble(x -> x));
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
        if (vote == null) {
            return null;
        }
        return summaries.get(vote);
    }

    public Integer getBestSummaryIdx() {
        if (bestSummaryVotes.isEmpty()) {
            return null;
        }
        return Multisets.copyHighestCountFirst(ImmutableSortedMultiset.copyOf(bestSummaryVotes))
                .iterator().next();
    }

    public int getLeftmostLeafIndex() {
        if (isLeaf()) {
            return contextTree.getLeafNodeLevel().getNodes().indexOf(this);
        } else {
            return getChildren().get(0).getLeftmostLeafIndex();
        }

    }

}
