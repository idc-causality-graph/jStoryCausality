package il.ac.idc.yonatan.causality.contexttree;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ygraber on 2/4/17.
 */
public class NodeTest {

    private ContextTree contextTree;
    private Node node;

    @Before
    public void init() {
        contextTree = new ContextTree();
        node = new Node();
        Node.contextTree = contextTree;
    }

    @Test
    public void getBestSummary_shouldGetBestSummary_whenSingleVote() {
        node.getBestSummaryVotes().add(1);
        node.getSummaries().addAll(Arrays.asList("summary0","summary1"));
        assertThat(node.getBestSummary(), equalTo("summary1"));
    }

    @Test
    public void getBestSummary_shouldGetBestSummary_whenMultiVotes(){
        node.getBestSummaryVotes().add(1);
        node.getBestSummaryVotes().add(1);
        node.getBestSummaryVotes().add(0);
        node.getSummaries().addAll(Arrays.asList("summary0","summary1"));
        assertThat(node.getBestSummary(), equalTo("summary1"));
    }

    @Test
    public void getBestSummary_shouldGetBestSummary_whenMultiVotesWithTie(){
        node.getBestSummaryVotes().add(1);
        node.getBestSummaryVotes().add(1);
        node.getBestSummaryVotes().add(0);
        node.getBestSummaryVotes().add(0);
        node.getSummaries().addAll(Arrays.asList("summary0","summary1"));
        assertThat(node.getBestSummary(), equalTo("summary0"));
    }
}