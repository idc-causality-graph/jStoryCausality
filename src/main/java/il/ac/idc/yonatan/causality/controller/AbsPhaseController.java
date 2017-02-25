package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.contexttree.PhaseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbsPhaseController {
    /**
     *
     * @param result
     * @return Pair of HitID,AssignmentId
     */
    Set<Pair<String,String>> getHitIdsFromMap(Map<String, String> result) {
        return result.keySet().stream()
                .filter(x -> x.endsWith("_approve"))
                .map(x -> StringUtils.substringBeforeLast(x, "_approve"))
                .map(x-> Pair.of(StringUtils.substringBefore(x,":"),StringUtils.substringAfter(x,":")))
                .collect(Collectors.toSet());
    }

    String processHits(RedirectAttributes redir, PhaseManager phaseManager) throws IOException {
        log.info("Starting phase");
        List<String> errors = phaseManager.canCreateHits();
        if (errors.isEmpty()) {
            phaseManager.createHits();
        } else {
            redir.addFlashAttribute("errors", errors);
        }
        log.info("Errors: {}", errors);
        return "redirect:/contextTree";
    }

}
