package il.ac.idc.yonatan.causality.contexttree;

import com.google.common.collect.Lists;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ygraber on 2/5/17.
 */
@Service
public class CausalityGraphManager {
    @Autowired
    private ContextTreeManager contextTreeManager;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private HitManager hitManager;

    public void initCausalityGraph() throws IOException {
        double t1 = appConfig.getImportanceThreshold();
        double t2 = t1 / 2;

        ContextTree contextTree = contextTreeManager.getContextTree();
        NodeLevel leafLevel = contextTree.getNodeLevels().getLast();
        List<Node> leafs = leafLevel.getNodes();
        double previousScore = -1;
        Node previousNode = null;
        for (Node leaf : leafs) {
            Double score = leaf.getNormalizedImportanceScore();
            if (score > t1) {
                leaf.getCausalityData().setPotentiallyImportant(true);
            } else {
                if (previousScore > score && previousNode != null) {
                    // local maxima
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
        contextTree.setCauslityLevelStep(-1);
        contextTreeManager.save();
    }

    private void markParentsPotentiallyImportant(Collection<Node> nodes) {
        nodes
                .stream()
                .filter(node -> node.getParent() != null)
                .map(Node::getCausalityData)
                .filter(CausalityData::isPotentiallyImportant)
                .forEach(causalityData -> {
                    causalityData.getNode().getParent().getCausalityData().setPotentiallyImportant(true);
                });

        Set<Node> importantParents = nodes.stream().filter(node -> node.getParent() != null)
                .map(Node::getCausalityData)
                .filter(CausalityData::isPotentiallyImportant)
                .map(causalityData -> causalityData.getNode().getParent())
                .collect(toSet());
        markParentsPotentiallyImportant(importantParents);
    }

    private List<NodeLevel> createPruneNodeLevels(List<NodeLevel> unprunedNodeLevels) {
        List<NodeLevel> pruneNodeLevels = new ArrayList<>();
        for (NodeLevel unprunedNodeLevel : unprunedNodeLevels) {
            List<String> prunedNodeIds = unprunedNodeLevel.getNodes().stream()
                    .map(Node::getCausalityData)
                    .filter(CausalityData::isPotentiallyImportant)
                    .map(CausalityData::getNodeId)
                    .collect(toList());
            NodeLevel newNodeLevel = new NodeLevel();
            newNodeLevel.getNodeIds().addAll(prunedNodeIds);
            pruneNodeLevels.add(newNodeLevel);
        }
        return pruneNodeLevels;

    }

    public void produceHitsForLevel() {
        int r = appConfig.getCausalityReplicaFactor();

        ContextTree contextTree = contextTreeManager.getContextTree();
        int currentLevel = contextTree.getCauslityLevelStep();

        int level = contextTree.getCauslityLevelStep() + 1;
        contextTree.setCauslityLevelStep(level);

        List<Node> queryNodes = contextTree.getPrunedLeafNodeLevel().getNodes();
        NodeLevel potCausalityNodeLevel = contextTree.getPrunedNodeLevels().get(level);

        for (Node potCausalityNode : potCausalityNodeLevel.getNodes()) {
            Node parent = potCausalityNode.getParent();

            // if no parent, this is root, create HITs
            if (parent != null) {
                CausalityData parentCausalityData = parent.getCausalityData();
                if (!parentCausalityData.getTargetNodeIds().contains(potCausalityNode.getId())) {
                    // (p(v),q) is not an edge in CG_i
                    continue;
                }
            }

            for (Node queryNode : queryNodes) {
                for (int i = 0; i < r; i++) {
                    String hitId = createCausalityHit(potCausalityNode, queryNode);
                    potCausalityNode.getCausalityData().getHits().add(hitId);
                }
            }
        }
    }

    private String createCausalityHit(Node sourceNode, Node queryNode) {
        //TODO
        return null;
    }

    public List<String> canProduceHitsForLevel() {
        ContextTree contextTree = contextTreeManager.getContextTree();
        if (contextTree.getPhase() != Phases.CAUSALITY_GRAPH) {
            return Lists.newArrayList("Not in phase CAUSALITY_GRAPH");
        }
        int level = contextTree.getCauslityLevelStep();
        if (level < 0) {
            return Collections.emptyList();
        }

        NodeLevel currentNodeLevel = contextTree.getPrunedNodeLevels().get(level);
        List<String> errors = new ArrayList<>();
        for (Node node : currentNodeLevel.getNodes()) {
            CausalityData causalityData = node.getCausalityData();
            if (!causalityData.isLevelCompleted()) {
                List<String> moreErrors = causalityData.getHits().stream()
                        .filter(hitId -> !causalityData.getCompletedHits().contains(hitId))
                        .map(hitId -> "HIT " + hitId + " not completed")
                        .collect(toList());
                errors.addAll(moreErrors);
            }
        }
        return errors;
    }

//    public void handleDownPhaseReview(String nodeId, String hitId, boolean approved, String reason,

}
