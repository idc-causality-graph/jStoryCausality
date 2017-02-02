package il.ac.idc.yonatan.causality.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import il.ac.idc.yonatan.causality.contexttree.ContextTreeManager;
import il.ac.idc.yonatan.causality.contexttree.UpHitReviewData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

/**
 * Created by ygraber on 1/29/17.
 */
@Controller
@Slf4j
public class Ctrl {

    @Autowired
    private ContextTreeManager contextTreeManager;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("contextTree")
    public String getContextTree(Model model) {
        model.addAttribute("htmlTree", contextTreeManager.dumpHtml());
        return "contextTree";
    }

    @PostMapping("contextTree/save")
    public String saveContextTree() throws IOException {
        contextTreeManager.save();
        return "redirect:/contextTree";
    }

    @PostMapping("contextTree/reload")
    public String reloadContextTree() throws IOException {
        contextTreeManager.reload();
        return "redirect:/contextTree";
    }

    @PostMapping("contextTree/progressUp")
    public String progressUpHits(RedirectAttributes redir) throws IOException {
        log.info("Progressing up");
        List<String> errors = contextTreeManager.canCreateHitsForUpPhase();
        if (errors.isEmpty()) {
            contextTreeManager.createHitsForUpPhase();
        } else {
            redir.addFlashAttribute("errors", errors);
        }
        log.info("Errors: {}", errors);
        return "redirect:/contextTree";
    }

    @GetMapping("contextTree/reviewsUpPhase")
    public String reviewUpPhase(Model model) {
        List<UpHitReviewData> hitsForReview = contextTreeManager.getUpPhaseHitsForReviews();
        model.addAttribute("hitsForReview", hitsForReview);
        return "reviews";
    }

    @PostMapping(value = "contextTree/reviewsUpPhase")
    public String commitReviewUpPhase(@RequestParam Map<String, String> result) throws IOException {
        Set<String> hitIds = result.keySet().stream()
                .filter(x -> x.endsWith("_approve"))
                .map(x -> StringUtils.substringBeforeLast(x, "_approve"))
                .collect(Collectors.toSet());

        for (String hitId : hitIds) {
            boolean approved = StringUtils.equals(result.get(hitId + "_approve"), "1");
            String reason = result.get(hitId + "_reason");
            String nodeId = result.get(hitId + "_nodeid");
            String summaryBase64 = result.get(hitId + "_summary");
            Map<String, Integer> chosenChildrenSummaries =
                    getChosenChildrenSummariesFromParam(result.get(hitId + "_chosenChildrenSummaries"));
            String summary;
            if (StringUtils.isNotEmpty(summaryBase64)) {
                summary = new String(Base64.getDecoder().decode(summaryBase64));
            } else {
                summary = null;
            }
            contextTreeManager.handleUpPhaseReview(nodeId, hitId, summary, approved, reason, chosenChildrenSummaries);
        }
        return "redirect:/contextTree/reviewsUpPhase";
    }

    private Map<String, Integer> getChosenChildrenSummariesFromParam(String param) throws IOException {
        if (StringUtils.isEmpty(param)) {
            return null;
        }
        String jsonStr = new String(Base64.getDecoder().decode(param));
        MapType mapType = TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Integer.class);
//        MapType mapType = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Integer.class);
        return objectMapper.readValue(jsonStr, mapType);
    }

}
