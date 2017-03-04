package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

@Service
@Slf4j
public class UpPhaseManager implements PhaseManager {
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

    public List<String> canCreateHits() {

        ContextTree contextTree = contextTreeManager.getContextTree();
        List<String> errors = new ArrayList<>();
        Phases phase = contextTree.getPhase();
        if (phase != Phases.UP_PHASE) {
            errors.add("Tree in phase " + phase);
            return errors;
        }
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel(contextTree);
        if (nodeLevel == null) {
            errors.add("Already at the top level");
            return errors;
        }

        List<Node> nodes = nodeLevel.getNodes();
        for (Node node : nodes) {
            if (node.getUpHitId() != null && !node.isUpPhaseDone(appConfig.getReplicationFactor())) {
                errors.add("Node <a href=\"#" + node.getId() + "\">" + node.getId() + "</a> still not done");
            }
        }
        return errors;
    }

    private void createHitForNode(Node node, int replicas) {
        List<Node> children = node.getChildren();
        LinkedHashMap<String, List<String>> childIdToSummaries = new LinkedHashMap<>();

        for (Node child : children) {
            for (String summary : child.getSummaries()) {
                List<String> summaries = childIdToSummaries.getOrDefault(child.getId(), new ArrayList<>(0));
                summaries.add(summary);
                childIdToSummaries.put(child.getId(), summaries);
            }
        }
        node.setUpHitTaskData(childIdToSummaries);
        String hitId = hitManager.createUpHit(childIdToSummaries, replicas);
        node.setUpHitId(hitId);
    }

    public void createHits() throws IOException {
        int replicationFactor = appConfig.getReplicationFactor();
        ContextTree contextTree = contextTreeManager.getContextTree();
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel(contextTree);
        if (nodeLevel == null) {
            log.warn("Trying to create hits on root node");
            return;
        }
        List<Node> nodes = nodeLevel.getNodes();
        for (Node node : nodes) {
            createHitForNode(node, replicationFactor);
        }
        contextTreeManager.save();
    }

    public void handleUpPhaseReview(String nodeId, String hitId, String assignmentId, String summary, boolean hitApproved,
                                    String reason, Map<String, Integer> chosenChildrenSummaries) throws IOException {
        Preconditions.checkNotNull(nodeId, "nodeId must not be null");
        Preconditions.checkNotNull(hitId, "hitId must not be null");
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNode(nodeId);

        log.debug("Handling review for hit {} node {} approved {}", hitId, nodeId, hitApproved);
        log.debug("Summary {}", summary);
        log.debug("chosenChildrenSummaries: {}", chosenChildrenSummaries);

        if (hitApproved) {
            node.getSummaries().add(summary);
            node.getCompletedUpAssignmentsIds().add(hitId + ":" + assignmentId);
            for (Node childNode : node.getChildren()) {
                // this is why children get so many votes.
                String childId = childNode.getId();
                Integer vote = chosenChildrenSummaries.get(childId);
                childNode.getBestSummaryVotes().add(vote);
            }
            //Check if all nodes are done in level. If so, perform the next step
            NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel(contextTree);
            boolean isAllNodesDone = nodeLevel.getNodes().stream()
                    .allMatch(n -> n.isUpPhaseDone(appConfig.getReplicationFactor()));
            if (isAllNodesDone) {
                contextTree.setUpLevelStep(contextTree.getUpLevelStep() + 1);
                if (getCurrentUpPhaseNodeLevel(contextTree) == null) {
                    // starting down phase
                    contextTree.setPhase(Phases.DOWN_PHASE);
                }
            }
        }
        contextTreeManager.save();
        hitManager.submitReviewUpHit(hitId, assignmentId, hitApproved, reason);
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

    private NodeLevel getCurrentUpPhaseNodeLevel(ContextTree contextTree) {
        int upLevelStep = contextTree.getUpLevelStep();
        LinkedList<NodeLevel> nodeLevels = contextTree.getNodeLevels();
        if (upLevelStep >= nodeLevels.size()) {
            return null;
        }
        return nodeLevels.get(upLevelStep);
    }

    public List<UpHitReviewData> getUpPhaseHitsForReviews() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        List<UpHitReviewData> result = new ArrayList<>();
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel(contextTree);
        if (nodeLevel == null) {
            return result;
        }
        List<Node> nodes = nodeLevel.getNodes(); //getNodes(nodeLevel);
        for (Node node : nodes) {
            if (node.isUpPhaseDone(appConfig.getReplicationFactor())) {
                continue;
            }
            String upHitId = node.getUpHitId();
            if (upHitId == null) {
                continue;
            }
            List<UpHitResult> upHitsForReview = hitManager.getUpHitForReview(upHitId);
            for (UpHitResult upHitForReview : upHitsForReview) {
                UpHitReviewData upHitReviewData = new UpHitReviewData();
                result.add(upHitReviewData);
                upHitReviewData.setNodeId(node.getId());
                upHitReviewData.setHitId(upHitId);
                upHitReviewData.setAssignmentId(upHitForReview.getAssignmentId());
                upHitReviewData.setChosenChildrenSummariesJsonBase64(
                        getObjectInJsonBase64(upHitForReview.getChosenChildrenSummaries()));

                upHitReviewData.setSummary(upHitForReview.getHitSummary());

                LinkedHashMap<String, List<String>> upHitTaskData = node.getUpHitTaskData();
                Set<String> childIds = upHitTaskData.keySet();
                StringBuilder taskText = new StringBuilder();
                for (String childId : childIds) {
                    Integer chosenSummaryNumber = upHitForReview.getChosenChildrenSummaries().get(childId);
                    String chosenSummary = upHitTaskData.get(childId).get(chosenSummaryNumber);
                    taskText.append(chosenSummary);
                    taskText.append("<br>");
                }
                upHitReviewData.setTaskText(taskText.toString());

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

    public void relaunchHit(String hitId) throws IOException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(hitId), "hitId must not be empty");
        ContextTree contextTree = contextTreeManager.getContextTree();
        Node node = contextTree.getNodeRepository().values()
                .stream().filter(n -> hitId.equals(n.getUpHitId()))
                .findFirst().get();

        int replicationFactor = appConfig.getReplicationFactor();
        int assignmentDoneCount = node.getCompletedUpAssignmentsIds().size();
        createHitForNode(node, replicationFactor - assignmentDoneCount);
        contextTreeManager.save();
    }
}
