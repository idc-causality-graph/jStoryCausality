package il.ac.idc.yonatan.causality.contexttree;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.CauseAndAffect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class CausalityPhaseManager implements PhaseManager {
    private ContextTreeManager contextTreeManager;

    private AppConfig appConfig;

    private HitManager hitManager;

    @Autowired
    public CausalityPhaseManager(ContextTreeManager contextTreeManager, AppConfig appConfig, HitManager hitManager) {
        this.contextTreeManager = contextTreeManager;
        this.appConfig = appConfig;
        this.hitManager = hitManager;
    }

    void initCausalityGraph() throws IOException {
        double t1 = appConfig.getImportanceThreshold();
        double t2 = t1 / 2;

        ContextTree contextTree = contextTreeManager.getContextTree();
        NodeLevel leafLevel = contextTree.getLeafNodeLevel();
        List<Node> leafs = leafLevel.getNodes();
        double previousScore = -1;
        Node previousNode = null;
        for (Node leaf : leafs) {
            Double score = leaf.getNormalizedImportanceScore();
            if (score > t1) {
                leaf.getCausalityData().setPotentiallyImportant(true);
            } else {
                if (previousScore > score && previousScore > t2 && previousNode != null) {
                    // a local maximum with threshold greater than t2
                    previousNode.getCausalityData().setPotentiallyImportant(true);
                }
            }
            previousNode = leaf;
            previousScore = score;
        }
        markParentsPotentiallyImportant(leafs);
        List<NodeLevel> pruneNodeLevels = createPruneNodeLevels(contextTree.getNodeLevels());
        contextTree.getPrunedNodeLevels().clear();
        contextTree.getPrunedNodeLevels().addAll(pruneNodeLevels);
        contextTree.setCausalityLevelStep(contextTree.getPrunedNodeLevels().size());
        contextTreeManager.save();
    }

    private void markParentsPotentiallyImportant(Collection<Node> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        nodes
                .stream()
                .filter(node -> node.getParent() != null)
                .filter(node -> node.getCausalityData().isPotentiallyImportant())
                .forEach(node -> node.getParent().getCausalityData().setPotentiallyImportant(true));

        Set<Node> importantParents = nodes.stream()
                .filter(node -> node.getParent() != null)
                .filter(node -> node.getCausalityData().isPotentiallyImportant())
                .map(Node::getParent)
                .collect(toSet());
        markParentsPotentiallyImportant(importantParents);
    }

    private List<NodeLevel> createPruneNodeLevels(List<NodeLevel> unprunedNodeLevels) {
        List<NodeLevel> pruneNodeLevels = new ArrayList<>();
        for (NodeLevel unprunedNodeLevel : unprunedNodeLevels) {
            List<String> prunedNodeIds = unprunedNodeLevel.getNodes().stream()
                    .filter(node -> node.getCausalityData().isPotentiallyImportant())
                    .map(Node::getId)
                    .collect(toList());
            NodeLevel newNodeLevel = new NodeLevel();
            newNodeLevel.getNodeIds().addAll(prunedNodeIds);
            pruneNodeLevels.add(newNodeLevel);
        }
        return pruneNodeLevels;

    }

    /**
     * Go through all query nodes, and for each query node create a list of potential cause nodes.
     * Out of every B query nodes, create a single HIT - which asks for to check all actual causes out of the cause nodes.
     */
    public void createHits() throws IOException {
        int r = appConfig.getCausalityReplicaFactor();
        int b = appConfig.getBranchFactor();

        ContextTree contextTree = contextTreeManager.getContextTree();

        Node rootNode = contextTree.getRootNode();
        int level = contextTree.getCausalityLevelStep() - 1;
        contextTree.setCausalityLevelStep(level);

        List<Node> queryNodes = contextTree.getPrunedLeafNodeLevel().getNodes();
        NodeLevel potCausalityNodeLevel = contextTree.getPrunedNodeLevels().get(level);

        List<Pair<Node, List<Node>>> allQueryNodeToPotentialCauseNodes = new ArrayList<>();

        for (Node queryNode : queryNodes) {
            List<Node> causalityNodesToCheck = new ArrayList<>();
            int queryNodeIdx = queryNode.getLeftmostLeafIndex();
            for (Node potCausalityNode : potCausalityNodeLevel.getNodes()) {
                if (potCausalityNode == queryNode) {
                    // no need to query about myself
                    continue;
                }
                if (potCausalityNode.getLeftmostLeafIndex() > queryNodeIdx) {
                    // The earliest child happens after the query node happens.
                    // So None of my children can affectively be the cause for the
                    // query node. Skip it.
                    continue;
                }
                Node parent = potCausalityNode.getParent();
                if (parent != null) {
                    CausalityData parentCausalityData = parent.getCausalityData();
                    if (!parentCausalityData.getTargetNodeIds().contains(queryNode.getId())) {
                        // (p(v),q) is not an edge in CG_i
                        continue;
                    }
                }
                causalityNodesToCheck.add(potCausalityNode);
            }
            if (!causalityNodesToCheck.isEmpty()) {
                Pair<Node, List<Node>> queryNodeToPotentialCauseNodes = Pair.of(queryNode, causalityNodesToCheck);
                allQueryNodeToPotentialCauseNodes.add(queryNodeToPotentialCauseNodes);
            }
        }

        List<List<Pair<Node, List<Node>>>> partitionedQueryToPotCauses =
                Lists.partition(allQueryNodeToPotentialCauseNodes, b);

        for (List<Pair<Node, List<Node>>> questionsData : partitionedQueryToPotCauses) {
            // partitionedQueryToPotCause contains no more than b pairs!
            List<CausalityQuestion> causalityHitQuestions = createCausalityHitQuestions(questionsData);
            String globalSummary = rootNode.getBestSummary();
            for (int i = 0; i < r; i++) {
                String hitId = hitManager.createCausalityHit(globalSummary, causalityHitQuestions);
                contextTree.getCausalityHits().add(hitId);
            }
        }
        contextTreeManager.save();
    }

    private List<CausalityQuestion> createCausalityHitQuestions(List<Pair<Node, List<Node>>> questionsData) {
        List<CausalityQuestion> causalityQuestions = new ArrayList<>();
        for (Pair<Node, List<Node>> questionData : questionsData) {
            Node queryNode = questionData.getLeft();
            String question = queryNode.getBestSummary();
            String questionNodeId = queryNode.getId();

            List<Node> causeNodes = questionData.getRight();
            List<CausalityQuestion.CauseAndNodeId> causes =
                    causeNodes.stream()
                            .map(node -> new CausalityQuestion.CauseAndNodeId(node.getBestSummary(), node.getId()))
                            .collect(toList());
            causalityQuestions.add(new CausalityQuestion(question, causes, questionNodeId));
        }
        return causalityQuestions;
    }

    public List<String> canCreateHits() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        if (contextTree.getPhase() != Phases.CAUSALITY_PHASE) {
            return newArrayList("Not in phase CAUSALITY_PHASE");
        }
        List<String> errors = new ArrayList<>();
        Set<String> uncompletedCausalityHits = contextTree.getUncompletedCausalityHits();
        for (String uncompletedCausalityHit : uncompletedCausalityHits) {
            errors.add("HIT " + uncompletedCausalityHit + " not completed");
        }
        return errors;
    }

    public List<CausalityHitReviewData> getCausalityPhaseHitsForReview() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        List<CausalityHitReviewData> causalityHitReviewDataList = new ArrayList<>();
        Set<String> uncompletedCausalityHits = contextTree.getUncompletedCausalityHits();
        for (String uncompletedCausalityHit : uncompletedCausalityHits) {
            // this hit contains more than one query node, and of each query node more than one cause.
            // this code turn in into map from query node to list of causes and non causes.
            CausalityHitReviewData causalityHitReviewData = new CausalityHitReviewData();
            CausalityHitResult causalityHitResult = hitManager.getCausalityHitForReview(uncompletedCausalityHit);
            causalityHitReviewData.setHitDone(causalityHitResult.isHitDone());
            causalityHitReviewData.setHitId(uncompletedCausalityHit);
            Multimap<String, String> queryToCauses = queryToCausesNodeIds(causalityHitResult.getCauseAndAffects());
            Multimap<String, String> queryToNonCauses = queryToCausesNodeIds(causalityHitResult.getNonCauseAndAffects());

            Map<String, CausalityHitReviewData.CausalityData> queryNodeIdToCausalityData = new HashMap<>();
            List<CausalityHitReviewData.CausalityData> resultCauses =
                    convertToCausalityData(contextTree, queryToCauses, queryNodeIdToCausalityData, true);
            List<CausalityHitReviewData.CausalityData> resultNonCauses =
                    convertToCausalityData(contextTree, queryToNonCauses, queryNodeIdToCausalityData, false);

            causalityHitReviewData.getCausalityDataList().addAll(resultCauses);
            causalityHitReviewData.getCausalityDataList().addAll(resultNonCauses);
            causalityHitReviewDataList.add(causalityHitReviewData);
        }
        return causalityHitReviewDataList;
    }

    public void handleCausalityPhaseReview(String hitId, boolean approved, String reason, List<CauseAndAffect> causeAndAffects)
            throws IOException {
        if (approved) {
            ContextTree contextTree = contextTreeManager.getContextTree();
            for (CauseAndAffect causeAndAffect : causeAndAffects) {
                String queryNodeId = causeAndAffect.getAffectNodeId();
                String causeNodeId = causeAndAffect.getCauseNodeId();
                Node causeNode = contextTree.getNode(causeNodeId);
                causeNode.getCausalityData().getTargetNodeIds().add(queryNodeId);
            }
            contextTree.getCompletedCausalityHits().add(hitId);
            if (contextTree.getCausalityLevelStep() == 0 && contextTree.getUncompletedCausalityHits().isEmpty()) {
                // Finished last HIT in the leaf level
                contextTree.setPhase(Phases.DONE);
            }
            contextTreeManager.save();
        }
        hitManager.submitCausalityHitReview(hitId, approved, reason);
    }

    private Multimap<String, String> queryToCausesNodeIds(Set<CauseAndAffect> causeAndAffects) {
        HashMultimap<String, String> mmap = HashMultimap.create();
        for (CauseAndAffect causeAndAffect : causeAndAffects) {
            String queryNodeId = causeAndAffect.getAffectNodeId();
            String causeNodeId = causeAndAffect.getCauseNodeId();
            mmap.put(queryNodeId, causeNodeId);
        }
        return mmap;
    }

    private List<CausalityHitReviewData.CausalityData> convertToCausalityData(ContextTree contextTree, Multimap<String, String> queryToCauses, Map<String, CausalityHitReviewData.CausalityData> queryNodeIdToCausalityData, boolean condition) {
        List<CausalityHitReviewData.CausalityData> result = new ArrayList<>();
        for (String queryNodeId : queryToCauses.keySet()) {
            CausalityHitReviewData.CausalityData causalityData = queryNodeIdToCausalityData.get(queryNodeId);
            if (causalityData == null) {
                causalityData = new CausalityHitReviewData.CausalityData();
                queryNodeIdToCausalityData.put(queryNodeId, causalityData);
                // Add it to result only when created, otherwise it will appear in result twice
                result.add(causalityData);
            }
            queryNodeIdToCausalityData.put(queryNodeId, causalityData);
            causalityData.setQueryNodeId(queryNodeId);
            causalityData.setQueryText(contextTree.getNode(queryNodeId).getBestSummary());
            for (String causeNodeId : queryToCauses.get(queryNodeId)) {
                String causeNodeSummary = contextTree.getNode(causeNodeId).getBestSummary();
                causalityData.getCauseNodesTextRelations().add(Triple.of(causeNodeId, causeNodeSummary, condition));
            }
        }
        return result;
    }

}
