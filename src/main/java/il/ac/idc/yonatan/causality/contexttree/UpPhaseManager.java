package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ygraber on 2/4/17.
 */
@Service
@Slf4j
public class UpPhaseManager {
    private ContextTreeManager contextTreeManager;

    private AppConfig appConfig;

    private HitManager hitManager;

    private ObjectMapper objectMapper;

    @Autowired
    public UpPhaseManager(AppConfig appConfig, ContextTreeManager contextTreeManager, HitManager hitManager,
                          ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.contextTreeManager = contextTreeManager;
        this.hitManager = hitManager;
        this.objectMapper = objectMapper;
    }

    public List<String> canCreateHitsForUpPhase() {

        List<String> errors = new ArrayList<>();
        Phases phase = contextTreeManager.getPhase();
        if (phase != Phases.UP_PHASE) {
            errors.add("Tree in phase " + phase);
            return errors;
        }
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
        if (nodeLevel == null) {
            errors.add("Already at the top level");
            return errors;
        }

        List<Node> nodes = nodeLevel.getNodes();
        for (Node node : nodes) {
            if (node.getUpHitIds().size() != node.getCompletedUpHitIds().size()) {
                errors.add("Node " + node.getId() + " still need review");
            }
        }
        return errors;
    }

    public void createHitsForUpPhase() throws IOException {
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
        if (nodeLevel == null) {
            log.warn("Trying to create hits on root node");
            return;
        }
        List<Node> nodes = nodeLevel.getNodes();
        boolean saveNeeded = false;
        for (Node node : nodes) {
            List<Node> children = node.getChildren();
            LinkedHashMap<String, List<String>> childIdToSummaries = new LinkedHashMap<>();

            for (Node child : children) {
                for (String summary : child.getSummaries()) {
                    List<String> summaries = childIdToSummaries.getOrDefault(child.getId(), new ArrayList<>(0));
                    summaries.add(summary);
                    childIdToSummaries.put(child.getId(), summaries);
                }
            }
            //TODO assert that all children have the same number of summaries
            for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
                node.setUpHitTaskData(childIdToSummaries);
                String hitId = hitManager.createUpHit(childIdToSummaries);
                node.getUpHitIds().add(hitId);
                saveNeeded = true;
            }
        }
        if (saveNeeded) {
            contextTreeManager.save();
        }
    }

    public void handleUpPhaseReview(String nodeId, String hitId, String summary, boolean hitApproved,
                                    String reason, Map<String, Integer> chosenChildrenSummaries) throws IOException {
        Preconditions.checkNotNull(nodeId, "nodeId must not be null");
        Preconditions.checkNotNull(hitId, "hitId must not be null");
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNode(nodeId);
        log.debug("NodeId {}", nodeId);
        log.debug("Summary {}", summary);
        log.debug("chosenChildrenSummaries: {}", chosenChildrenSummaries);

        if (hitApproved) {
            node.getSummaries().add(summary);
            node.getCompletedUpHitIds().add(hitId);
            for (Node childNode : node.getChildren()) {
                String childId = childNode.getId();
                Integer vote = chosenChildrenSummaries.get(childId);
                childNode.getBestSummaryVotes().add(vote);
            }
            //Check if all nodes are done in level. If so, perform the next step
            NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
            boolean isAllNodesDone = nodeLevel.getNodes().stream()
                    .allMatch(Node::isUpPhaseDone);
            if (isAllNodesDone) {
                contextTree.setUpLevelStep(contextTree.getUpLevelStep() + 1);
                if (getCurrentUpPhaseNodeLevel() == null) {
                    // starting down phase
                    contextTree.setPhase(Phases.DOWN_PHASE);
                }
            }
            contextTreeManager.save();
        }
        hitManager.submitReviewUpHit(hitId, hitApproved, reason);
    }

    public void choseRootNodeUpHitSummary(int chosenResult) throws IOException {
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node rootNode = contextTree.getRootNode();
        List<Integer> bestSummaryVotes = rootNode.getBestSummaryVotes();
        if (bestSummaryVotes.isEmpty()) {
            bestSummaryVotes.add(chosenResult);
        } else {
            bestSummaryVotes.set(0, chosenResult);
        }
        contextTreeManager.save();
    }

    private NodeLevel getCurrentUpPhaseNodeLevel() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        int upLevelStep = contextTree.getUpLevelStep();
        LinkedList<NodeLevel> nodeLevels = contextTree.getNodeLevels();
        if (upLevelStep >= nodeLevels.size()) {
            return null;
        }
        return nodeLevels.get(upLevelStep);
    }

    public List<UpHitReviewData> getUpPhaseHitsForReviews() {
        List<UpHitReviewData> result = new ArrayList<>();
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
        if (nodeLevel == null) {
            return result;
        }
        List<Node> nodes = nodeLevel.getNodes(); //getNodes(nodeLevel);
        for (Node node : nodes) {
            List<String> upHitIds = node.getUpHitIds();
            Set<String> completedUpHitIds = node.getCompletedUpHitIds();
            for (String upHitId : upHitIds) {
                if (!completedUpHitIds.contains(upHitId)) {
                    UpHitReviewData upHitReviewData = new UpHitReviewData();
                    upHitReviewData.setNodeId(node.getId());
                    result.add(upHitReviewData);
                    UpHitResult upHitResult = hitManager.getUpHitForReview(upHitId);
                    upHitReviewData.setHitDone(upHitResult.isHitDone());
                    upHitReviewData.setHitId(upHitId);
                    upHitReviewData.setChosenChildrenSummariesJsonBase64(
                            getObjectInJsonBase64(upHitResult.getChosenChildrenSummaries()));

                    if (upHitResult.isHitDone()) {
                        upHitReviewData.setSummary(upHitResult.getHitSummary());

                        LinkedHashMap<String, List<String>> upHitTaskData = node.getUpHitTaskData();
                        Set<String> childIds = upHitTaskData.keySet();
                        StringBuilder taskText = new StringBuilder();
                        for (String childId : childIds) {
                            Integer chosenSummaryNumber = upHitResult.getChosenChildrenSummaries().get(childId);
                            String chosenSummary = upHitTaskData.get(childId).get(chosenSummaryNumber);
                            taskText.append(chosenSummary);
                            taskText.append("<br>");
                        }
                        upHitReviewData.setTaskText(taskText.toString());
                    }
                }
            }
        }
        return result;
    }

    @SneakyThrows
    private String getObjectInJsonBase64(Object object) {
        if (object == null) {
            return "";
        }
        String json = objectMapper.writeValueAsString(object).replace('\n', ' ');
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

}
