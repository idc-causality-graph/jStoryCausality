package il.ac.idc.yonatan.causality.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import il.ac.idc.yonatan.causality.contexttree.UpHitReviewData;
import il.ac.idc.yonatan.causality.contexttree.UpPhaseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@Slf4j
public class UpPhaseController extends AbsPhaseController {

    private ObjectMapper objectMapper;
    private UpPhaseManager upPhaseManager;

    @Autowired
    public UpPhaseController(ObjectMapper objectMapper, UpPhaseManager upPhaseManager) {
        this.objectMapper = objectMapper;
        this.upPhaseManager = upPhaseManager;
    }

    @PostMapping(value = "contextTree/reviewsUpPhase")
    public String commitReviewUpPhase(@RequestParam Map<String, String> result) throws IOException {
        Set<Pair<String, String>> hitAssignmentIds = getHitIdsFromMap(result);
        for (Pair<String, String> hitAssignmentId : hitAssignmentIds) {
            String hitId = hitAssignmentId.getLeft();
            String assignmentId = hitAssignmentId.getRight();
            boolean approved = StringUtils.equals(result.get(assignmentId + "_approve"), "1");
            String reason = result.get(assignmentId + "_reason");
            String nodeId = result.get(assignmentId + "_nodeid");
            String summaryBase64 = result.get(assignmentId + "_summary");
            Map<String, Integer> chosenChildrenSummaries =
                    getChosenChildrenSummariesFromParam(result.get(assignmentId + "_chosenChildrenSummaries"));
            String summary;
            if (StringUtils.isNotEmpty(summaryBase64)) {
                summary = new String(Base64.getDecoder().decode(summaryBase64));
            } else {
                summary = null;
            }
            upPhaseManager.handleUpPhaseReview(nodeId, hitId, assignmentId, summary, approved, reason, chosenChildrenSummaries);
        }
        return "redirect:/contextTree/reviewsUpPhase";
    }

    @GetMapping("contextTree/reviewsUpPhase")
    public String reviewUpPhase(Model model) {
        List<UpHitReviewData> hitsForReview = //contextTreeManager.getUpPhaseHitsForReviews();
                upPhaseManager.getUpPhaseHitsForReviews();
        model.addAttribute("hitsForReview", hitsForReview);
        return "reviewUpHits";
    }

    private Map<String, Integer> getChosenChildrenSummariesFromParam(String param) throws IOException {
        if (StringUtils.isEmpty(param)) {
            return null;
        }
        String jsonStr = new String(Base64.getDecoder().decode(param));
        MapType mapType = TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Integer.class);
        return objectMapper.readValue(jsonStr, mapType);
    }

    @PostMapping("contextTree/progressUp")
    public String progressUpHits(RedirectAttributes redir) throws IOException {
        return processHits(redir, upPhaseManager);
    }

    @PostMapping("contextTree/rootNode/choseUpResult")
    public ResponseEntity<Void> choseSummaryUpHitRootNode(@RequestParam("chosenResult") Integer chosenResult) throws IOException {
        upPhaseManager.choseRootNodeUpHitSummary(chosenResult);
        return ResponseEntity.ok(null);
    }

}
