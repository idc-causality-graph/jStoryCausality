package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import il.ac.idc.yonatan.causality.config.AppConfig;
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
import java.util.List;

@Service
@Slf4j
public class ContextTreeManager {

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    private final ResourceLoader resourceLoader;

    private Configuration fmConfig;

    @Autowired
    public ContextTreeManager(
            AppConfig appConfig, ObjectMapper objectMapper, ResourceLoader resourceLoader,
            Configuration fmConfig) {
        this.fmConfig = fmConfig;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @SneakyThrows
    public ContextTree getContextTree() {
        init();
        return contextTree;
    }

    public void reset() throws IOException {
        appConfig.getContextFile().delete();
        init();
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
            save();
        } else {
            log.debug("Loading context file from {}", contextFile);
            try (InputStream is = new FileInputStream(contextFile)) {
                contextTree = objectMapper.readValue(is, ContextTree.class);
            }
        }
    }

    private ContextTree contextTree;

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
        return new ContextTreeHtml(contextTree, fmConfig).create();
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
        node.getBestSummaryVotes().add(0);
        return node;
    }

}