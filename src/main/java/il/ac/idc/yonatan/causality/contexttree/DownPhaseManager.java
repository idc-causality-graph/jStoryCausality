package il.ac.idc.yonatan.causality.contexttree;

import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

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
        Collection<Node> allNodes = contextTree.getAllNodes();
        for (Node node : allNodes) {
            if (node.getId().equals(contextTree.getRootNodeId())) {
                //root node, a single maximum importance rating
                node.getEventImportanceRatings().add(7);
            }
            if (node.isLeaf()) {
                continue;
            }
            //TODO modify according Shai's implementation!
            String summary = node.getBestSummary();
            List<String> childrenSummaries = node.getChildren().stream()
                    .map(Node::getBestSummary)
                    .collect(toList());
            for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
                String hitId = hitManager.createDownHit(summary, childrenSummaries);
                node.getDownHitIds().add(hitId);
            }
        }
        contextTreeManager.save();

    }

    public void handleDownPhaseReview(String nodeId, String hitId, boolean approved, String reason,
                                      List<Integer> grades) throws IOException {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNode(nodeId);
        if (approved) {
            List<Node> childNodes = node.getChildren();
            for (int i = 0; i < childNodes.size(); i++) {
                childNodes.get(i).getEventImportanceRatings().add(grades.get(i));
            }
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
        //TODO modify implementation!
        List<DownHitReviewData> result = new ArrayList<>();
        if (contextTree.getPhase() != Phases.DOWN_PHASE) {
            return result;
        }
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
                downHitReviewData.setNodeSummary(node.getBestSummary());

                DownHitResult downHitForReview = hitManager.getDownHitForReview(hitId);
                downHitReviewData.setHitDone(downHitForReview.isHitDone());
                if (downHitForReview.isHitDone()) {
                    List<Integer> grades = downHitForReview.getGrades();
                    List<Node> children = node.getChildren();
                    for (int i = 0; i < children.size(); i++) {
                        String childSummary = children.get(i).getBestSummary();
                        Integer summaryRank = grades.get(i);
                        downHitReviewData.getRanks().add(Pair.of(childSummary, summaryRank));
                    }
                }

                result.add(downHitReviewData);
            }
        }
        return result;
    }

}
