package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.contexttree.DownHitReviewData;
import il.ac.idc.yonatan.causality.contexttree.DownPhaseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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

    @PostMapping("contextTree/reviewsDownPhase")
    public String commitReviewDownPhase(@RequestParam Map<String, String> result) throws IOException {
        Set<String> hitIds = getHitIdsFromMap(result);

        for (String hitId : hitIds) {
            boolean approved = StringUtils.equals(result.get(hitId + "_approve"), "1");
            String reason = result.get(hitId + "_reason");
            String nodeId = result.get(hitId + "_nodeid");
            String mostImportantEvent = result.get(hitId + "_event");
            Integer importanceScore = NumberUtils.createInteger(result.get(hitId + "_score"));
            downPhaseManager.handleDownPhaseReview(nodeId, hitId, approved, reason, importanceScore, mostImportantEvent);
        }
        return "redirect:/contextTree/reviewsDownPhase";
    }

    @PostMapping("contextTree/progressDown")
    public String progressDownHits(RedirectAttributes redir) throws IOException {
        return processHits(redir, downPhaseManager);
    }
}
