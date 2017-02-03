package il.ac.idc.yonatan.causality.contexttree;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by ygraber on 1/28/17.
 */
public class ContextTreeHtml {
    private ContextTree contextTree;
    private StringBuilder output;
    private boolean isRoot;

    public ContextTreeHtml(ContextTree contextTree) {
        this.contextTree = contextTree;
    }

    private String nodeToHtml(Node node) {
        StringBuilder sb = new StringBuilder();
        boolean isRoot = node.getId().equals(contextTree.getRootNodeId());

        sb.append("<div class='card'>")
                .append("<div class='card-header'>").append("<b>Id:</b>&nbsp;")
                .append("<a name='").append(node.getId()).append("'>")
                .append(node.getId())
                .append("</a>");
        if (node.isLeaf()) {
            sb.append("&nbsp;&nbsp;&nbsp;<b>LEAF</b>");
        }

        sb.append("</div><div class='card-block'>");
        if (node.getParentNodeId() != null) {
            sb.append("<b>Parent:</b>&nbsp;")
                    .append("<a href='#").append(node.getParentNodeId()).append("'>").append(node.getParentNodeId()).append("</a><br>");
        }
        sb.append("<b>up phase done?</b>&nbsp;")
                .append(node.isUpPhaseDone())
                .append("<br><b>Down phase done?</b>&nbsp;")
                .append(node.isDownPhaseDone())
                .append("<br><b>Up HIT ids:</b>&nbsp;")
                .append(node.getUpHitIds())
                .append("<br><b>Completed Up HIT ids:</b>&nbsp;")
                .append(node.getCompletedUpHitIds())
                .append("<br><b>Down HIT ids:</b>&nbsp;")
                .append(node.getDownHitIds())
                .append("<br><b>Completed Up HIT ids:</b>&nbsp;")
                .append(node.getCompletedDownHitIds())
                .append("<br><b>Down phase rates:</b>&nbsp;")
                .append(node.getSummaryRates())
                .append("<br><b>Average rate:</b>&nbsp;")
                .append(node.getAverageRate())
                .append("<br><b>Norm average rate:&nbsp;")
                .append(String.format("%.2f", node.getNormalizedAverageRate()))
                .append("</b>");

        boolean hasPicked = node.getChosenSummary() != null;
        if (isRoot) {
            sb.append("<form id='root_node_form'>");
            if (hasPicked) {
                sb.append("<button id='edit_root_btn' type='button' class='btn btn-secondary'>Edit</button>");
            }
        }
        for (int i = 0; i < node.getSummaries().size(); i++) {
            String summary = StringEscapeUtils.escapeHtml4(node.getSummaries().get(i));

            boolean isPicked = node.getChosenSummary() != null && node.getChosenSummary() == i;
            sb.append("<br>");

            if (isRoot) {
                sb.append("<input ");
                if (isPicked) {
                    sb.append("checked ");
                }
                if (hasPicked) {
                    sb.append("disabled ");
                }
                sb.append("type='radio' name='rootChosen' value='" + i + "'>&nbsp;");
            }

            sb.append("<b>")
                    .append(isPicked && !isRoot ? "<u>" : "")
                    .append("Summary #")
                    .append(i + 1)
                    .append(isPicked && !isRoot ? "</u>" : "")
                    .append(":</b>&nbsp;")
                    .append(StringUtils.replace(summary, "\n", "<br>"));
        }
        if (isRoot) {
            sb.append("</form>");
        }
        if (!node.isLeaf()) {
            sb.append("<br><b>Children:</b>&nbsp;");
            for (String childId : node.getChildIds()) {
                sb.append("<a href='#").append(childId).append("'>")
                        .append(childId)
                        .append("</a>")
                        .append("&nbsp;");
            }
        }


        sb.append("</div></div>");
        return sb.toString();
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
                .append("<div>")
                .append(nodeToHtml(node))
                .append("</div>")
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
