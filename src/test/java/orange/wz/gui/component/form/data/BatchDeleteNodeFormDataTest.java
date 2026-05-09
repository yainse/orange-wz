package orange.wz.gui.component.form.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchDeleteNodeFormDataTest {

    @Test
    void parityModeShouldBeEnabledWhenOddOrEvenSelected() {
        assertFalse(new BatchDeleteNodeFormData("foo", false, false).isParityMode());
        assertTrue(new BatchDeleteNodeFormData("foo", true, false).isParityMode());
        assertTrue(new BatchDeleteNodeFormData("foo", false, true).isParityMode());
        assertTrue(new BatchDeleteNodeFormData("foo", true, true).isParityMode());
    }
}
