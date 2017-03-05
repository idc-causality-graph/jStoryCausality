package il.ac.idc.yonatan.causality.contexttree;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.SneakyThrows;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ygraber on 1/28/17.
 */
public class ContextTreeHtml {
    private ContextTree contextTree;
    private StringBuilder output;
    private boolean isRoot;
    private Template template;
    private int repFactor;
    private int causRepFactor;

    @SneakyThrows
    public ContextTreeHtml(ContextTree contextTree, Configuration freeMarkerConfig, int repFactor, int causRepFactor) {
        this.contextTree = contextTree;
        this.repFactor = repFactor;
        this.causRepFactor = causRepFactor;
        this.template = freeMarkerConfig.getTemplate("nodeCard.ftl");
    }

    @SneakyThrows
    private String nodeToHtml(Node node, boolean leafOnly) {
        Map<String, Object> fmContext = new HashMap<>();
        fmContext.put("node", node);
        fmContext.put("leafOnly", leafOnly);
        fmContext.put("repFactor", repFactor);
        fmContext.put("causRepFactor", causRepFactor);
        StringWriter writer = new StringWriter();
        template.process(fmContext, writer);
        return writer.toString();
    }

    @SneakyThrows
    public String create(boolean leafOnly) {
        output = new StringBuilder();
        Node rootNode = contextTree.getNodeRepository().get(contextTree.getRootNodeId());
        isRoot = true;
        visitSubtree(rootNode, false, leafOnly);
        return output.toString();
    }

    private void visitSubtree(Node node, boolean last, boolean leafOnly) {
        if (!leafOnly) {
            if (isRoot) {
                output.append("<ul class='tree'>");
                isRoot = false;
            } else {
                if (last) {
                    output.append("<ul class='last'>\n");
                } else {
                    output.append("<ul>\n");
                }
            }
            output.append("\n");
            if (last) {
                output.append("<li class='last'>\n");
            } else {
                output.append("<li>\n");
            }
        }
        if (!leafOnly || node.isLeaf()) {
            output.append(nodeToHtml(node, leafOnly));
        }
        if (!leafOnly) {
            output.append("</li>\n");
        }

        List<String> childIds = node.getChildIds();
        for (int i = 0; i < childIds.size(); i++) {
            String childId = childIds.get(i);
            boolean lastInGroup = (i + 1 == childIds.size());
            visitSubtree(contextTree.getNodeRepository().get(childId), lastInGroup, leafOnly);
        }
        if (!leafOnly) {
            output.append("</ul>\n");
        }
    }
}
