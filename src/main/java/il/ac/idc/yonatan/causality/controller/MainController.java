package il.ac.idc.yonatan.causality.controller;

import il.ac.idc.yonatan.causality.contexttree.ContextTree;
import il.ac.idc.yonatan.causality.contexttree.ContextTreeManager;
import il.ac.idc.yonatan.causality.mturk.HitManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

@Controller
@Slf4j
public class MainController {

    private final ContextTreeManager contextTreeManager;

    private final HitManager hitManager;

    @Autowired
    public MainController(ContextTreeManager contextTreeManager, HitManager hitManager) {
        this.contextTreeManager = contextTreeManager;
        this.hitManager = hitManager;
    }

    @GetMapping("contextTree")
    public String getContextTree(Model model) {
        ContextTree contextTree = contextTreeManager.getContextTree();
        model.addAttribute("htmlTree", contextTreeManager.dumpHtml());
        model.addAttribute("phase", contextTree.getPhase());
        model.addAttribute("completedCausalityHitIds", contextTree.getCompletedCausalityHits());
        model.addAttribute("causalityHitIds", contextTree.getCausalityHits());
        return "contextTree";
    }

    @PostMapping("contextTree/reset")
    public String resetEverything() throws IOException {
        contextTreeManager.reset();
        hitManager.reset();
        return "redirect:/contextTree";
    }

}
