package il.ac.idc.yonatan.causality.causality;

import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.contexttree.ContextTree;
import il.ac.idc.yonatan.causality.contexttree.Node;
import il.ac.idc.yonatan.causality.contexttree.NodeLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;

/**
 * Created by ygraber on 2/5/17.
 */
@Service
public class CausalityGraphManager {
    @Autowired
    private AppConfig appConfig;

    public CausalityGraph createCausalityGraph(ContextTree contextTree) {
        double t1 = appConfig.getImportanceThreshold();
        double t2 = t1 / 2;

        CausalityGraph causalityGraph = new CausalityGraph();
        LinkedHashSet<Node> potentiallyImportantNodes = new LinkedHashSet<>();
        NodeLevel leafs = contextTree.getNodeLevels().getFirst();
        double previousScore = -1;
        Node previousNode = null;
        for (Node leaf : leafs.getNodes()) {
            Double score = leaf.getNormalizedImportanceScore();
            if (score > t1) {
                potentiallyImportantNodes.add(leaf);
            } else {
                if (previousScore > score && previousNode != null && !potentiallyImportantNodes.contains(previousNode)) {
                    // local maxima
                    potentiallyImportantNodes.add(previousNode);
                }
            }
            previousNode = leaf;
            previousScore = score;
        }
        potentiallyImportantNodes.forEach(node -> causalityGraph.getPotentialImportant().add(node.getId()));
        return causalityGraph;
    }

}
