package orange.wz.gui.form.data;

import lombok.Getter;

@Getter
public class LongFormData extends NodeFormData {
    private final long value;

    public LongFormData(String name, String type, long value) {
        super(name, type);
        this.value = value;
    }
}
