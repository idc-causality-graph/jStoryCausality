package il.ac.idc.yonatan.causality.contexttree;

import com.google.common.base.Preconditions;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class DownPhaseManager implements PhaseManager {
    private ContextTreeManager contextTreeManager;

    private AppConfig appConfig;

    private CausalityPhaseManager causalityPhaseManager;

    private HitManager hitManager;

    public DownPhaseManager(AppConfig appConfig, ContextTreeManager contextTreeManager,
                            CausalityPhaseManager causalityPhaseManager, HitManager hitManager) {
        this.appConfig = appConfig;
        this.hitManager = hitManager;
        this.contextTreeManager = contextTreeManager;
        this.causalityPhaseManager = causalityPhaseManager;
    }

    public List<String> canCreateHits() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Phases phase = contextTree.getPhase();
        if (phase != Phases.DOWN_PHASE) {
            return newArrayList("Context tree in phase " + phase);
        }
        Node rootNode = contextTree.getRootNode();
        if (rootNode.getBestSummaryVotes().isEmpty()) {
            return newArrayList("Missing best summary choice for the root node");
        }
        // All hits for down phase are created together - it's enough to see one with
        if (contextTree.areDownPhaseHitCreated()) {
            return newArrayList("HITs already created for DOWN_PHASE");
        }
        return Collections.emptyList();
    }

    /**
     * Get the list of summaries of this nodes and all its ancestors
     */
    private List<String> getParentsSummaries(Node node) {
        List<String> parentsSummaries = new ArrayList<>();
        do {
            parentsSummaries.add(node.getBestSummary());
            node = node.getParent();
        } while (node != null);
        Collections.reverse(parentsSummaries);
        return parentsSummaries;
    }

    private void createHitForNode(Node node, int replicas) {
        List<String> parentsSummaries = getParentsSummaries(node);

        List<Node> children = node.getChildren();
        List<Pair<String, String>> childrenIdsAndSummaries = children.stream()
                .map(child -> Pair.of(child.getId(), child.getBestSummary()))
                .collect(toList());

        String hitId = hitManager.createDownHit(parentsSummaries, childrenIdsAndSummaries, node.isLeaf(), replicas);
        node.setDownHitId(hitId);
    }

    public void createHits() throws IOException {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node rootNode = contextTree.getRootNode();
        List<String> allRootSummaries = rootNode.getSummaries();

        Collection<Node> allNodes = contextTree.getAllNodes();
        for (Node node : allNodes) {
            if (node.isLeaf()) {
                // No need to do that for leaf - has no children!
                continue;
            }
            if (appConfig.isUseMetaLeafs() && node.isMetaLeaf()) {
                continue;
            }
            if (node.getId().equals(contextTree.getRootNodeId())) {
                //root node, a single maximum importance rating
                node.getEventImportanceScores().add(7);
                node.getEventImportanceWorkerNormalizedScores().add(1.0);
                node.setNormalizedImportanceScore(1.0);
            }

            createHitForNode(node, appConfig.getReplicationFactor());
        }
        contextTreeManager.save();

    }

    public void handleDownPhaseReview(String nodeId, String hitId, String assignmentId, boolean approved, String reason,
                                      List<Triple<String, Integer, String>> idsAndScoresAndImportantEvents) throws IOException {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNode(nodeId);
        if (approved) {
            double maxWorkerScore = idsAndScoresAndImportantEvents.stream()
                    .map(Triple::getMiddle)
                    .max(Integer::compareTo)
                    .get();

            for (Triple<String, Integer, String> idAndScoreAndImportantEvent : idsAndScoresAndImportantEvents) {
                String childId = idAndScoreAndImportantEvent.getLeft();
                Integer score = idAndScoreAndImportantEvent.getMiddle();
                String mostImportantEvent = idAndScoreAndImportantEvent.getRight();
                Node child = contextTree.getNode(childId);
                child.getEventImportanceScores().add(score);
                child.getEventImportanceWorkerNormalizedScores().add((double) score / maxWorkerScore);
                child.getMostImportantEvents().add(mostImportantEvent);
                if (appConfig.isUseMetaLeafs() && child.isMetaLeaf()) {
                    // if this is a metaleaf, and we are in metaleaf mode, copy the impotency data to the actual
                    // leaf
                    Node grandchild = child.getChildren().get(0);
                    grandchild.getEventImportanceScores().add(score);
                    grandchild.getEventImportanceWorkerNormalizedScores().add((double) score / maxWorkerScore);
                    grandchild.getMostImportantEvents().add(mostImportantEvent);
                }
            }
            node.getCompletedDownAssignmentsIds().add(hitId + ":" + assignmentId);
//            node.getCompletedDownHitIds().add(hitId);

            //Check if are done. If so, perform the next step
            Stream<Node> nodeStream = contextTree.getAllNodes().stream();

            if (appConfig.isUseMetaLeafs()) {
                // Skip metaleaf level if we are in "metaleaf" mode
                nodeStream=nodeStream.filter(n -> !n.isMetaLeaf());

            }
            boolean isAllNodesDone = nodeStream.allMatch(n -> n.isDownPhaseDone(appConfig.getReplicationFactor()));

            if (isAllNodesDone) {
                // down phase is done. we have a full context tree.
                // now, normalize it!
                Node rootNode = contextTree.getRootNode();
                normalizeNode(rootNode);
                contextTree.setPhase(Phases.CAUSALITY_PHASE);
                contextTreeManager.save();
                causalityPhaseManager.initCausalityGraph();
            } else {
                contextTreeManager.save();
            }
        }
        hitManager.submitDownHitReview(hitId, assignmentId, approved, reason);

    }

    private void normalizeNode(Node node) {
        List<Node> children = node.getChildren();

        for (Node child : children) {
            double childNormalizedImportanceScore = child.getAverageImportanceScore() * node.getNormalizedImportanceScore();
            child.setNormalizedImportanceScore(childNormalizedImportanceScore);
            normalizeNode(child);
        }
    }

    public List<DownHitReviewData> getDownPhaseHitsForReview() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        List<DownHitReviewData> result = new ArrayList<>();
        if (contextTree.getPhase() != Phases.DOWN_PHASE) {
            return result;
        }
        Collection<Node> nodes = contextTree.getAllNodes();
        for (Node node : nodes) {
            if (node.isDownPhaseDone(appConfig.getReplicationFactor())) {
                continue;
            }
            String downHitId = node.getDownHitId();
            if (downHitId == null) {
                continue;
            }
            List<String> parentsSummaries = getParentsSummaries(node);
            Map<String, String> childIdToSummary =
                    node.getChildren().stream()
                            .collect(toMap(Node::getId, Node::getBestSummary));

            List<DownHitResult> downHitResults = hitManager.getDownHitForReview(downHitId);
            for (DownHitResult downHitResult : downHitResults) {
                DownHitReviewData downHitReviewData = new DownHitReviewData();
                result.add(downHitReviewData);
                downHitReviewData.setHitId(downHitId);
                downHitReviewData.setAssignmentId(downHitResult.getAssignmentId());
                downHitReviewData.setNodeId(node.getId());
                downHitReviewData.setParentsSummaries(parentsSummaries);
                downHitReviewData.setChildIdToSummary(childIdToSummary);
                List<Triple<String, Integer, String>> idsAndScoresAndEvents = downHitResult.getIdsAndScoresAndEvents()
                        .stream()
                        .map(ise -> Triple.of(ise.getNodeId(), ise.getScore(), ise.getMostImportantEvent()))
                        .collect(toList());

                downHitReviewData.setIdsAndScoresAndEvents(idsAndScoresAndEvents);
            }
        }
        return result;
    }

    public void relaunchHit(String hitId) throws IOException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(hitId), "hitId cannot be empty");
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNodeRepository().values()
                .stream().filter(n -> hitId.equals(n.getDownHitId()))
                .findFirst().get();

        int replicationFactor = appConfig.getReplicationFactor();
        int assignmentDoneCount = node.getCompletedDownAssignmentsIds().size();
        createHitForNode(node, replicationFactor - assignmentDoneCount);
        contextTreeManager.save();
    }

}
