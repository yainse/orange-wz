package orange.wz.gui.form.data;

import lombok.Getter;

@Getter
public class FloatFormData extends NodeFormData {
    private final float value;

    public FloatFormData(String name, String type, float value) {
        super(name, type);
        this.value = value;
    }
}
