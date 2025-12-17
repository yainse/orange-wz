package orange.wz.gui.form.data;

import lombok.Getter;

@Getter
public class IntFormData extends NodeFormData {
    private final int value;

    public IntFormData(String name, String type, int value) {
        super(name, type);
        this.value = value;
    }
}
