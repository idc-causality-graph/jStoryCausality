package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.contexttree.DownHitReviewData;
import il.ac.idc.yonatan.causality.contexttree.DownPhaseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ygraber on 2/11/17.
 */
@Controller
@Slf4j
public class DownPhaseController extends AbsPhaseController {
    private DownPhaseManager downPhaseManager;

    @Autowired
    public DownPhaseController(DownPhaseManager downPhaseManager) {
        this.downPhaseManager = downPhaseManager;
    }


    @GetMapping("contextTree/reviewsDownPhase")
    public String reviewDownPhase(Model model) {
        List<DownHitReviewData> hitsForReview = downPhaseManager.getDownPhaseHitsForReview();
        model.addAttribute("hitsForReview", hitsForReview);
        return "reviewDownHits";
    }

    @PostMapping(value = "contextTree/downPhase/relaunch/{hitId}")
    public String relaunchUpHit(@PathVariable String hitId) throws IOException {
        downPhaseManager.relaunchHit(hitId);
        return "redirect:/contextTree";

    }

    @PostMapping("contextTree/reviewsDownPhase")
    public String commitReviewDownPhase(@RequestParam Map<String, String> result) throws IOException {
        Set<String> hitAssignmentIds = getHitIdsFromMap(result);

        for (String hitAssignmentId : hitAssignmentIds) {
            Pair<String, String> pair = splitToHitAssignmentId(hitAssignmentId);

            String hitId = pair.getLeft();
            String assignmentId = pair.getRight();
            boolean approved = StringUtils.equals(result.get(hitAssignmentId + "_approve"), "1");
            String reason = result.get(hitAssignmentId + "_reason");
            String nodeId = result.get(hitAssignmentId + "_nodeid");
            String hitEncodedData = result.get(hitAssignmentId+"_data");
            List<Triple<String, Integer, String>> idsAndScoresAndEvents = decodeDataToIdsAndScoresAndEvents(hitEncodedData);

            downPhaseManager.handleDownPhaseReview(nodeId, hitId, assignmentId, approved, reason, idsAndScoresAndEvents);
        }
        return "redirect:/contextTree/reviewsDownPhase";
    }

    private List<Triple<String, Integer, String>> decodeDataToIdsAndScoresAndEvents(String encodedData){
        List<Triple<String, Integer, String>> result=new ArrayList<>();
        String[] split = StringUtils.split(encodedData, ':');
        for (String part : split) {
            String data = new String(Base64.getDecoder().decode(part));
            String[] singleData = StringUtils.split(data, ":",3);
            result.add(Triple.of(singleData[0],Integer.parseInt(singleData[1]),singleData[2]));
        }
        return result;
    }

    @PostMapping("contextTree/progressDown")
    public String progressDownHits(RedirectAttributes redir) throws IOException {
        return processHits(redir, downPhaseManager);
    }
}
