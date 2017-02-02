package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multisets;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManagerImpl;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * Created by ygraber on 1/28/17.
 */

@Service
@Slf4j
public class ContextTreeManager {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private HitManagerImpl hitManager;

    @PostConstruct
    public void init() throws IOException {
        File contextFile = appConfig.getContextFile();
        if (!contextFile.exists()) {
            this.contextTree = new ContextTree();
            log.info("Starting new context file {} from resource {}", contextFile, appConfig.getInputResource());
            try (InputStream inputResource = resourceLoader.getResource(appConfig.getInputResource()).getInputStream()) {
                initialize(inputResource);
            }
        } else {
            log.info("Loading context file from {}", contextFile);
            try (InputStream is = new FileInputStream(contextFile)) {
                ContextTree contextTree = objectMapper.readValue(is, ContextTree.class);
                this.contextTree = contextTree;
            }
        }
    }

    private ContextTree contextTree;

    public List<String> canCreateHitsForUpPhase() {
        System.out.println(MultimapBuilder.ListMultimapBuilder
                .linkedHashKeys()
                .arrayListValues()
                .build().getClass());

        List<String> errors = new ArrayList<>();
        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
        if (nodeLevel==null){
            errors.add("Already at the top level");
            return errors;
        }
//        if (nodeLevel.getNodeIds().size() <= 1) {
//            errors.add("Already at the top level");
//            return errors;
//        }
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
        if (nodeLevel==null){
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
//        HashMultiset<Integer> multisetVotes = HashMultiset.create(votes);
        return Multisets.copyHighestCountFirst(ImmutableMultiset.copyOf(votes))
                .iterator().next();
//        int bestCount = 0;
//        int best = 0;
//        for (Integer vote : multisetVotes.elementSet()) {
//            if (multisetVotes.count(vote) > bestCount) {
//                best = vote;
//            }
//        }
//        return best;
    }


//    public void reviewHitsForUpPhase() {
//        NodeLevel nodeLevel = getCurrentUpPhaseNodeLevel();
//        List<Node> nodes = getNodes(nodeLevel);
//        // go over all nodes in the level
//        for (Node node : nodes) {
//            List<String> upHitIds = node.getUpHitIds();
//            // go over all hits for node
//            for (String upHitId : upHitIds) {
//                if (node.getCompletedUpHitIds().contains(upHitId)) {
//                    continue;
//                }
//                UpHitResult hitResult = hitManager.getUpHitForReview(upHitId);
//                boolean hitDone = hitResult.isHitDone();
//                if (hitDone) {
//                    node.getCompletedUpHitIds().add(upHitId);
//                    node.getSummaries().add(hitResult.getHitSummary());
//
//                    // add to vote counts of summaries
//                    Map<String, Integer> chosenChildrenSummaries = hitResult.getChosenChildrenSummaries();
//                    Multimap<String, Integer> chosenSummariesForChildren = node.getChosenSummariesForChildren();
//                    for (Map.Entry<String, Integer> entry : chosenChildrenSummaries.entrySet()) {
//                        chosenSummariesForChildren.put(entry.getKey(), entry.getValue());
//                    }
//                    // if all hits are done
//                    if (node.getCompletedUpHitIds().size() == upHitIds.size()) {
//                        // mark node as done for phase
//                        node.setUpPhaseDone(true);
//                        for (String childId : node.getChildIds()) {
//                            // calculate best summary
//                            int vote = getMaxVotes(chosenSummariesForChildren.get(childId));
//                            getNode(childId).setChosenSummary(vote);
//                        }
//                    }
//                }
//            }
//        }
//    }

    private List<Node> getNodes(NodeLevel nodeLevel) {
        return nodeLevel.getNodeIds().stream().map(id -> this.getNode(id)).collect(toList());
    }

    private List<Node> getChildren(Node node) {
        return node.getChildIds().stream().map(id -> this.getNode(id)).collect(toList());
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
}