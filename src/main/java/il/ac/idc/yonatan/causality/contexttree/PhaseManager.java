package il.ac.idc.yonatan.causality.contexttree;

import java.io.IOException;
import java.util.List;

public interface PhaseManager {

    List<String> canCreateHits();

    void createHits() throws IOException;
}
