package il.ac.idc.yonatan.causality.mturk.data;

import lombok.Data;

import java.util.List;

/**
 * Created by ygraber on 2/2/17.
 */
@Data
public class DownHitResult {
    private boolean hitDone;
    private List<Integer> grades;
}
