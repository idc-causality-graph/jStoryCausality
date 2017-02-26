package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.contexttree.CausalityHitReviewData;
import il.ac.idc.yonatan.causality.contexttree.CausalityPhaseManager;
import il.ac.idc.yonatan.causality.mturk.data.CauseAndAffect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ygraber on 2/11/17.
 */
@Controller
@Slf4j
public class CausalityPhaseController extends AbsPhaseController {

    private CausalityPhaseManager causalityPhaseManager;

    @Autowired
    public CausalityPhaseController(CausalityPhaseManager causalityPhaseManager) {
        this.causalityPhaseManager = causalityPhaseManager;
    }

    @GetMapping("contextTree/reviewsCausalityPhase")
    public String reviewCausalityPhase(Model model) {
        List<CausalityHitReviewData> hitsForReview = causalityPhaseManager.getCausalityPhaseHitsForReview();
        model.addAttribute("hitsForReview", hitsForReview);
        return "reviewCausalityHits";
    }

    @PostMapping("contextTree/reviewsCausalityPhase")
    public String commitReviewCausalityPhase(@RequestParam Map<String, String> result) throws IOException {
        Set<String> hitAssignmentIds = getHitIdsFromMap(result);

        for (String hitAssignmentId : hitAssignmentIds) {
            Pair<String, String> pair = splitToHitAssignmentId(hitAssignmentId);
            String hitId = pair.getLeft();
            String assignmentId = pair.getRight();
            boolean approved = StringUtils.equals(result.get(hitAssignmentId + "_approve"), "1");
            String reason = result.get(hitAssignmentId + "_reason");
            String pairString = result.get(hitAssignmentId + "_pairs");
            String[] pairs = StringUtils.split(pairString, ";");
            List<CauseAndAffect> causeAndAffects = new ArrayList<>();
            for (String aPair : pairs) {
                String[] split = StringUtils.split(aPair, ":");
                causeAndAffects.add(new CauseAndAffect(split[1], split[0]));
            }
            causalityPhaseManager.handleCausalityPhaseReview(hitId, assignmentId, approved, reason, causeAndAffects);
        }
        return "redirect:/contextTree/reviewsCausalityPhase";
    }

    @PostMapping("contextTree/progressCausality")
    public String progressCausalityHits(RedirectAttributes redir) throws IOException {
        return processHits(redir, causalityPhaseManager);
    }

}
