package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multisets;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

/**
 * Created by ygraber on 1/28/17.
 */

@Service
@Slf4j
public class ContextTreeManager {

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    private final ResourceLoader resourceLoader;

    private final HitManager hitManager;

    @Autowired
    public ContextTreeManager(AppConfig appConfig, ObjectMapper objectMapper, HitManager hitManager, ResourceLoader resourceLoader) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.hitManager = hitManager;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        File contextFile = appConfig.getContextFile();
        if (!contextFile.exists()) {
            contextTree = new ContextTree();
            log.info("Starting new context file {} from resource {}", contextFile, appConfig.getInputResource());
            try (InputStream inputResource = resourceLoader.getResource(appConfig.getInputResource()).getInputStream()) {
                initialize(inputResource);
            }
        } else {
            log.info("Loading context file from {}", contextFile);
            try (InputStream is = new FileInputStream(contextFile)) {
                contextTree = objectMapper.readValue(is, ContextTree.class);
            }
        }
    }

    private ContextTree contextTree;

    public List<String> canCreateHitsForUpPhase() {
        List<String> errors = new ArrayList<>();
        Phases phase = contextTree.getPhase();
        if (phase != Phases.UP_PHASE) {
            errors.add("Tree in phase " + phase);
            return errors;
        }
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
        if (nodeLevel == null) {
            errors.add("Already at the top level");
            return errors;
        }

        List<Node> nodes = getNodes(nodeLevel);
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
        List<Node> nodes = getNodes(nodeLevel);
        boolean saveNeeded = false;
        for (Node node : nodes) {
            List<Node> children = getChildren(node);
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
            save();
        }
    }

    @SneakyThrows
    private String getObjectInJsonBase64(Object object) {
        if (object == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(object));
    }

    public List<UpHitReviewData> getUpPhaseHitsForReviews() {
        List<UpHitReviewData> result = new ArrayList<>();
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
        if (nodeLevel == null) {
            return result;
        }
        List<Node> nodes = getNodes(nodeLevel);
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

    private int getMaxVotes(Collection<Integer> votes) {
        return Multisets.copyHighestCountFirst(ImmutableMultiset.copyOf(votes))
                .iterator().next();
    }

    public List<DownHitReviewData> getDownPhaseHitsForReview() {
        List<DownHitReviewData> result = new ArrayList<>();
        if (contextTree.getPhase() != Phases.DOWN_PHASE) {
            return result;
        }
        Collection<Node> nodes = getAllNodes();
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
                downHitReviewData.setNodeSummary(getSummary(node));

                DownHitResult downHitForReview = hitManager.getDownHitForReview(hitId);
                downHitReviewData.setHitDone(downHitForReview.isHitDone());
                if (downHitForReview.isHitDone()) {
                    List<Integer> grades = downHitForReview.getGrades();
                    List<Node> children = getNodes(node.getChildIds());
                    for (int i = 0; i < children.size(); i++) {
                        String childSummary = getSummary(children.get(i));
                        Integer summaryRank = grades.get(i);
                        downHitReviewData.getRanks().add(Pair.of(childSummary, summaryRank));
                    }
                }

                result.add(downHitReviewData);
            }
        }
        return result;
    }

    private List<Node> getNodes(NodeLevel nodeLevel) {
        return nodeLevel.getNodeIds().stream().map(this::getNode).collect(toList());
    }

    private List<Node> getChildren(Node node) {
        return node.getChildIds().stream().map(this::getNode).collect(toList());
    }

    private NodeLevel getCurrentUpPhaseNodeLevel() {
        int upLevelStep = contextTree.getUpLevelStep();
        LinkedList<NodeLevel> nodeLevels = contextTree.getNodeLevels();
        if (upLevelStep >= nodeLevels.size()) {
            return null;
        }
        return nodeLevels.get(upLevelStep);
    }

    private void initialize(InputStream inputStream) throws IOException {
        String input = IOUtils.toString(inputStream, Charset.defaultCharset());
        String[] fragments = StringUtils.splitByWholeSeparator(input, "---");

        NodeLevel nodeLevel = new NodeLevel();
        for (String fragment : fragments) {
            Node leafNode = createLeafNode(fragment);
            addNodeToLevel(nodeLevel, leafNode);
        }
        contextTree.getNodeLevels().add(nodeLevel);
        addUpperLevel(true);
        String rootNodeId = contextTree.getNodeLevels().getLast().getNodeIds().get(0);
        contextTree.setRootNodeId(rootNodeId);
        contextTree.setUpLevelStep(1);
        contextTree.setPhase(Phases.UP_PHASE);
    }

    public void save() throws IOException {
        File contextTreeFile = appConfig.getContextFile();
        try (OutputStream os = new FileOutputStream(contextTreeFile)) {
            String json = objectMapper.writeValueAsString(contextTree);
            IOUtils.write(json, os, Charset.defaultCharset());
        }
    }

    public String dumpHtml() {
        return new ContextTreeHtml(contextTree).create();
    }

    private void addUpperLevel(boolean leafLevel) {
        NodeLevel highestLevel = contextTree.getNodeLevels().getLast();
        List<String> nodeIds = highestLevel.getNodeIds();
        if (nodeIds.size() <= 1) {
            return;
        }

        List<List<String>> partition = Lists.partition(nodeIds,
                leafLevel ? appConfig.getLeafBranchFactor() : appConfig.getBranchFactor());
        NodeLevel newNodeLevel = new NodeLevel();
        for (List<String> nodeIdGroup : partition) {
            Node parentNode = createNonLeafNode();
            addNodeToLevel(newNodeLevel, parentNode);
            String parentId = parentNode.getId();
            for (String nodeId : nodeIdGroup) {
                Node childNode = getNode(nodeId);
                childNode.setParentNodeId(parentId);
                parentNode.getChildIds().add(childNode.getId());
            }
        }
        contextTree.getNodeLevels().add(newNodeLevel);
        addUpperLevel(false);
    }

    private Node getNode(String id) {
        return contextTree.getNodeRepository().get(id);
    }

    private void addNodeToLevel(NodeLevel nodeLevel, Node node) {
        contextTree.getNodeRepository().put(node.getId(), node);
        nodeLevel.getNodeIds().add(node.getId());
    }


    private Node createNonLeafNode() {
        Node node = new Node();
        node.setId(RandomStringUtils.randomAlphanumeric(12));
        node.setLeaf(false);
        return node;
    }

    private Node createLeafNode(String text) {
        Node node = new Node();
        node.setId(RandomStringUtils.randomAlphanumeric(12));
        node.setLeaf(true);
        node.getSummaries().add(text.trim());
        node.setChosenSummary(0);
        return node;
    }

    public void handleUpPhaseReview(String nodeId, String hitId, String summary, boolean hitApproved,
                                    String reason, Map<String, Integer> chosenChildrenSummaries) throws IOException {
        Preconditions.checkNotNull(nodeId, "nodeId must not be null");
        Preconditions.checkNotNull(hitId, "hitId must not be null");
        Node node = getNode(nodeId);
        log.debug("NodeId {}", nodeId);
        log.debug("Summary {}", summary);
        log.debug("chosenChildrenSummaries: {}", chosenChildrenSummaries);

        if (hitApproved) {
            node.getSummaries().add(summary);
            node.getCompletedUpHitIds().add(hitId);
            // add to vote counts of summaries
            Multimap<String, Integer> chosenSummariesForChildren = node.getChosenSummariesForChildren();
            log.debug("chosenSummariesForChildren before: {}", chosenSummariesForChildren);
            for (Map.Entry<String, Integer> entry : chosenChildrenSummaries.entrySet()) {
                chosenSummariesForChildren.put(entry.getKey(), entry.getValue());
            }
            log.debug("chosenSummariesForChildren after: {}", chosenSummariesForChildren);
            if (node.getCompletedUpHitIds().size() == node.getUpHitIds().size()) {
                node.setUpPhaseDone(true);
                for (String childId : node.getChildIds()) {
                    // calculate best summary
                    int vote = getMaxVotes(chosenSummariesForChildren.get(childId));
                    log.debug("Vote for {} is {}", childId, vote);
                    getNode(childId).setChosenSummary(vote);
                }
            }
            //Check if all nodes are done in level. If so, perform the next step
            NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
            boolean isAllNodesDone = getNodes(nodeLevel).stream()
                    .allMatch(Node::isUpPhaseDone);
            if (isAllNodesDone) {
                contextTree.setUpLevelStep(contextTree.getUpLevelStep() + 1);
                if (getCurrentUpPhaseNodeLevel() == null) {
                    // starting down phase
                    contextTree.setPhase(Phases.DOWN_PHASE);
                }
            }
            save();
        }
        hitManager.submitReviewUpHit(hitId, hitApproved, reason);
    }

    public void reload() throws IOException {
        init();
    }

    public void choseRootNodeUpHitSummary(int chosenResult) throws IOException {
        Node rootNode = getNode(contextTree.getRootNodeId());
        rootNode.setChosenSummary(chosenResult);
        save();
    }

    public List<String> canCreateHitsForDownPhase() {
        Phases phase = contextTree.getPhase();
        if (phase != Phases.DOWN_PHASE) {
            return newArrayList("Context tree in phase " + phase);
        }
        Node rootNode = getNode(contextTree.getRootNodeId());
        if (!rootNode.getDownHitIds().isEmpty()) {
            return newArrayList("HITs already created for " + phase);
        }
        return Collections.emptyList();
    }

    private Collection<Node> getAllNodes() {
        return contextTree.getNodeRepository().values();
    }

    public void createHitsForDownPhase() throws IOException {
        Collection<Node> allNodes = getAllNodes();
        for (Node node : allNodes) {
            if (node.getId().equals(contextTree.getRootNodeId())) {
                //root node, maximum grade
                node.getSummaryRates().add(7);
            }
            if (node.isLeaf()) {
                continue;
            }
            String summary = getSummary(node);
            List<Node> children = getNodes(node.getChildIds());
            List<String> childrenSummaries = children.stream().map(this::getSummary).collect(toList());
            for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
                String hitId = hitManager.createDownHit(summary, childrenSummaries);
                node.getDownHitIds().add(hitId);
            }
        }
        save();

    }

    private List<Node> getNodes(List<String> nodeIds) {
        return nodeIds.stream().map(id -> contextTree.getNodeRepository().get(id)).collect(toList());
    }

    private String getSummary(Node node) {
        return node.getSummaries().get(node.getChosenSummary());
    }

    public void handleDownPhaseReview(String nodeId, String hitId, boolean approved, String reason,
                                      List<Integer> grades) throws IOException {
        Node node = getNode(nodeId);
        if (approved) {
            List<Node> nodes = getNodes(node.getChildIds());
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).getSummaryRates().add(grades.get(i));
            }
            node.getCompletedDownHitIds().add(hitId);


            //Check if are done. If so, perform the next step
            boolean isAllNodesDone = getAllNodes().stream()
                    .allMatch(Node::isDownPhaseDone);
            if (isAllNodesDone) {
                // down phase is done. we have a full context tree.
                contextTree.setPhase(Phases.DONE);
            }
            save();
        }
        hitManager.submitDownHitReview(hitId, approved, reason);

    }

    public Phases getPhase() {
        return contextTree.getPhase();
    }
}