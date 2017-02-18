package il.ac.idc.yonatan.causality.contexttree;

import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        // All hits for down phase are created together.
        Node aLeafNode = contextTree.getLeafNodeLevel().getNodes().get(0);
        if (!aLeafNode.getDownHitIds().isEmpty()) {
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
            if (node.getId().equals(contextTree.getRootNodeId())) {
                //root node, a single maximum importance rating
                node.getEventImportanceScores().add(7);
                node.getEventImportanceWorkerNormalizedScores().add(1.0);
                node.setNormalizedImportanceScore(1.0);
            }

            List<String> parentsSummaries = getParentsSummaries(node);

            List<Node> children = node.getChildren();
            List<Pair<String, String>> childrenIdsAndSummaries = children.stream()
                    .map(child -> Pair.of(child.getId(), child.getBestSummary()))
                    .collect(toList());

            for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
                String hitId = hitManager.createDownHit(parentsSummaries, childrenIdsAndSummaries, node.isLeaf());
                node.getDownHitIds().add(hitId);
            }
        }
        contextTreeManager.save();

    }

    public void handleDownPhaseReview(String nodeId, String hitId, boolean approved, String reason,
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
            }
            node.getCompletedDownHitIds().add(hitId);

            //Check if are done. If so, perform the next step
            boolean isAllNodesDone = contextTree.getAllNodes().stream()
                    .allMatch(Node::isDownPhaseDone);
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
        hitManager.submitDownHitReview(hitId, approved, reason);

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
        List<String> rootNodeSummaries = contextTree.getRootNode().getSummaries();
        Collection<Node> nodes = contextTree.getAllNodes();
        for (Node node : nodes) {
            List<String> downHitIds = node.getDownHitIds();
            Set<String> completedDownHitIds = node.getCompletedDownHitIds();
            List<String> parentsSummaries = getParentsSummaries(node);
            Map<String, String> childIdToSummary =
                    node.getChildren().stream()
                            .collect(toMap(Node::getId, Node::getBestSummary));
            for (String hitId : downHitIds) {
                if (completedDownHitIds.contains(hitId)) {
                    continue;
                }
                DownHitReviewData downHitReviewData = new DownHitReviewData();
                downHitReviewData.setHitId(hitId);
                downHitReviewData.setNodeId(node.getId());
                downHitReviewData.setParentsSummaries(parentsSummaries);
                downHitReviewData.setChildIdToSummary(childIdToSummary);


                DownHitResult downHitResult = hitManager.getDownHitForReview(hitId);

                downHitReviewData.setHitDone(downHitResult.isHitDone());
                if (downHitResult.isHitDone()) {
                    List<Triple<String, Integer, String>> idsAndScoresAndEvents = downHitResult.getIdsAndScoresAndEvents()
                            .stream()
                            .map(ise -> Triple.of(ise.getNodeId(), ise.getScore(), ise.getMostImportantEvent()))
                            .collect(toList());

                    downHitReviewData.setIdsAndScoresAndEvents(idsAndScoresAndEvents);
                }

                result.add(downHitReviewData);
            }
        }
        Collections.sort(result, (o1, o2) -> Boolean.compare(o1.isHitDone(), o2.isHitDone()));
        return result;
    }

}
