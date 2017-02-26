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
    private String nodeToHtml(Node node) {
        Map<String, Object> fmContext = new HashMap<>();
        fmContext.put("node", node);
        fmContext.put("repFactor", repFactor);
        fmContext.put("causRepFactor", causRepFactor);
        StringWriter writer = new StringWriter();
        template.process(fmContext, writer);
        return writer.toString();
    }

    @SneakyThrows
    public String create() {
        output = new StringBuilder();
        Node rootNode = contextTree.getNodeRepository().get(contextTree.getRootNodeId());
        isRoot = true;
        visitSubtree(rootNode, false);
        return output.toString();
    }

    private void visitSubtree(Node node, boolean last) {
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
        output
                .append(nodeToHtml(node))
                .append("</li>\n");

        List<String> childIds = node.getChildIds();
        for (int i = 0; i < childIds.size(); i++) {
            String childId = childIds.get(i);
            boolean lastInGroup = (i + 1 == childIds.size());
            visitSubtree(contextTree.getNodeRepository().get(childId), lastInGroup);
        }
        output.append("</ul>\n");
    }
}
