package com.terraforged.mod.client.gui.screen.page;

import com.terraforged.mod.client.gui.screen.SettingsDraft;
import java.util.function.Supplier;

/** Named alias for a {@link ScrollPage} bound to one Settings subsection. */
public final class SettingsSectionPage extends ScrollPage {
    public SettingsSectionPage(
            String titleKey,
            SettingsDraft draft,
            String sectionKey,
            Supplier<Object> sectionSupplier,
            Runnable onChange
    ) {
        super(titleKey, draft, sectionKey, sectionSupplier, onChange);
    }
}
