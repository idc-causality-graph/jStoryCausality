package il.ac.idc.yonatan.causality;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import il.ac.idc.yonatan.causality.contexttree.ContextTreeManager;
import il.ac.idc.yonatan.causality.mturk.Mturk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Created by ygraber on 1/28/17.
 */
@SpringBootApplication
public class Main {

    @Bean
    public Module guavaModule(){
        return new GuavaModule();
    }
    @Autowired
    private Mturk mturk;
    
    @Autowired
    private ContextTreeManager contextTreeManagerFactory;
    
    @Autowired
    private ResourceLoader resourceLoader;
//    @Override
//    public void run(String... strings) throws Exception {
////        CreateHitResponse chr = mturk.createHit("titlie", "desc", "ques", 0.01, 1,
////                30, 30, Arrays.asList("123", "4"), "none");
////        System.out.println(chr);
////        System.out.println(mturk.getHit("3EN4YVUOUCO41O0L0Z4DNPUZAK2JXE"));
//        ContextTreeManagerFactory.ContextTreeManager contextTreeManager = contextTreeManagerFactory.createNew(resourceLoader.getResource("classpath:stories/magi.txt").getInputStream());
//        contextTreeManager.createHitsForUpPhase();
//        contextTreeManager.reviewHitsForUpPhase();
//        FileOutputStream fos=new FileOutputStream("./ctx.json");
//        contextTreeManager.save(fos);
//        fos.close();
//
//        FileUtils.write(new File("/Users/ygraber/Downloads/a.html"), contextTreeManager.dumpHtml());
////        System.out.println(contextTreeManager.dumpHtml());
//
////        ContextTree contextTree = contextTreeManager.createNew(resourceLoader.getResource("classpath:stories/magi.txt").getInputStream());
//
//    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
