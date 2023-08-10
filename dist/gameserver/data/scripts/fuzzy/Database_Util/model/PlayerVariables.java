package fuzzy.Database_Util.model;

/**
 * Created by a.kiperku
 * Date: 10.08.2023
 */

public class PlayerVariables {
    private long obj_id;
    private String type;
    private String name;
    private String value;
    private long expire_time;

    public PlayerVariables(long obj_id, String type, String name, String value, long expire_time) {
        this.obj_id = obj_id;
        this.type = type;
        this.name = name;
        this.value = value;
        this.expire_time = expire_time;
    }

    public long getObj_id() {
        return obj_id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public long getExpire_time() {
        return expire_time;
    }
}
