package il.ac.idc.yonatan.causality.contexttree;

import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by ygraber on 2/4/17.
 */
@Service
public class DownPhaseManager {
    private ContextTreeManager contextTreeManager;

    private AppConfig appConfig;

    private HitManager hitManager;

    public DownPhaseManager(AppConfig appConfig, ContextTreeManager contextTreeManager, HitManager hitManager) {
        this.appConfig = appConfig;
        this.hitManager = hitManager;
        this.contextTreeManager = contextTreeManager;
    }

    public List<String> canCreateHitsForDownPhase() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Phases phase = contextTree.getPhase();
        if (phase != Phases.DOWN_PHASE) {
            return newArrayList("Context tree in phase " + phase);
        }
        Node rootNode = contextTree.getRootNode();
        if (!rootNode.getDownHitIds().isEmpty()) {
            return newArrayList("HITs already created for " + phase);
        }
        return Collections.emptyList();
    }

    public void createHitsForDownPhase() throws IOException {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node rootNode = contextTree.getRootNode();
        List<String> allRootSummaries = rootNode.getSummaries();

        Collection<Node> allNodes = contextTree.getAllNodes();
        for (Node node : allNodes) {
            if (node.getId().equals(contextTree.getRootNodeId())) {
                //root node, a single maximum importance rating
                node.getEventImportanceScores().add(7);
                continue;
            }
            List<String> nodeSummaries = node.getSummaries();
            for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
                String hitId = hitManager.createDownHit(allRootSummaries, nodeSummaries, node.isLeaf());
                node.getDownHitIds().add(hitId);
            }
        }
        contextTreeManager.save();

    }

    public void handleDownPhaseReview(String nodeId, String hitId, boolean approved, String reason,
                                      Integer importanceScore, String mostImportantEvent) throws IOException {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNode(nodeId);
        if (approved) {
            node.getEventImportanceScores().add(importanceScore);
            node.getMostImportantEvents().add(mostImportantEvent);
            node.getCompletedDownHitIds().add(hitId);

            //Check if are done. If so, perform the next step
            boolean isAllNodesDone = contextTree.getAllNodes().stream()
                    .allMatch(Node::isDownPhaseDone);
            if (isAllNodesDone) {
                // down phase is done. we have a full context tree.
                contextTree.setPhase(Phases.DONE);
            }
            contextTreeManager.save();
        }
        hitManager.submitDownHitReview(hitId, approved, reason);

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
            for (String hitId : downHitIds) {
                if (completedDownHitIds.contains(hitId)) {
                    continue;
                }
                DownHitReviewData downHitReviewData = new DownHitReviewData();
                downHitReviewData.setHitId(hitId);
                downHitReviewData.setNodeId(node.getId());
                downHitReviewData.setRootNodeSummaries(rootNodeSummaries);
                downHitReviewData.setNodeSummaries(node.getSummaries());

                DownHitResult downHitResult = hitManager.getDownHitForReview(hitId);

                downHitReviewData.setHitDone(downHitResult.isHitDone());
                if (downHitResult.isHitDone()) {
                    downHitReviewData.setImportanceScore(downHitResult.getImportanceScore());
                    downHitReviewData.setMostImportantEvent(downHitResult.getMostImportantEvent());
                }

                result.add(downHitReviewData);
            }
        }
        Collections.sort(result, (o1, o2) -> Boolean.compare(o1.isHitDone(),o2.isHitDone()));
        return result;
    }

}