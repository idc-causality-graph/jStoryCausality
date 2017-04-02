package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.contexttree.ContextTree;
import il.ac.idc.yonatan.causality.contexttree.ContextTreeManager;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@Slf4j
public class MainController {

    private final ContextTreeManager contextTreeManager;

    private final HitManager hitManager;
    private final AppConfig appConfig;

    @Autowired
    public MainController(ContextTreeManager contextTreeManager, HitManager hitManager, AppConfig appConfig) {
        this.contextTreeManager = contextTreeManager;
        this.hitManager = hitManager;
        this.appConfig = appConfig;
    }

    @GetMapping("contextTree")
    public String getContextTree(Model model, @RequestParam(value = "leafOnly", defaultValue = "false") boolean leafOnly) {
        ContextTree contextTree = contextTreeManager.getContextTree();
        model.addAttribute("htmlTree", contextTreeManager.dumpHtml(leafOnly));
        model.addAttribute("phase", contextTree.getPhase());
        model.addAttribute("leafOnly", leafOnly);
        model.addAttribute("completedCausalityHitIds", contextTree.getCompletedCausalityHits());
        model.addAttribute("causalityHitIds", contextTree.getCausalityHits());
        model.addAttribute("sandbox", hitManager.isSandbox());
        return "contextTree";
    }

    @PostMapping("contextTree/reset")
    public String resetEverything() throws IOException {
        contextTreeManager.reset();
        hitManager.reset();
        return "redirect:/contextTree";
    }

    @GetMapping("contextTree/causalityGraph")
    public String generateGraph(Model model) {
        int causalityReplicaFactor = appConfig.getCausalityReplicaFactor();
        model.addAttribute("causalityReplicaFactor", causalityReplicaFactor);
        String causalityGraphJson = contextTreeManager.getCausalityGraphJson(appConfig.isUseMetaLeafs());
        model.addAttribute("rawData", causalityGraphJson);
        return "outputGraph";
    }

}
