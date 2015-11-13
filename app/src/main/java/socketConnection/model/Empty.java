package socketConnection.model;

import com.baidao.data.Jsonable;

/**
 * Created by rjhy on 15-9-29.
 */
public class Empty implements Jsonable {
    @Override
    public String toJson() {
        return "{}";
    }
}
