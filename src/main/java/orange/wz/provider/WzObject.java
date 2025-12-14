package orange.wz.provider;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
public abstract class WzObject {
    @Getter
    @Setter
    protected WzObject parent;
    @Getter
    @Setter
    protected String name;
    @Getter
    protected String path;

    protected WzObject(String name, WzObject parent) {
        this.name = name;
        this.parent = parent;

        if (parent == null || name.equals(parent.getName()) && name.endsWith(".wz")) {
            this.path = name;
        } else {
            this.path = parent.getPath() + "/" + name;
        }
    }
}
