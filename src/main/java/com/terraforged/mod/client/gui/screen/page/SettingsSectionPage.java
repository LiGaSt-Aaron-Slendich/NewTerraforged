package com.terraforged.mod.client.gui.screen.page;

import com.terraforged.mod.client.gui.screen.SettingsDraft;
import java.util.Collections;
import java.util.Set;
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
        super(titleKey, draft, sectionKey, sectionSupplier, onChange, Collections.emptySet());
    }

    public SettingsSectionPage(
            String titleKey,
            SettingsDraft draft,
            String sectionKey,
            Supplier<Object> sectionSupplier,
            Runnable onChange,
            Set<String> skipKeys
    ) {
        super(titleKey, draft, sectionKey, sectionSupplier, onChange, skipKeys);
    }

    /** Nested section under a parent NBT compound (e.g. {@code world.oceanLandscape}). */
    public SettingsSectionPage(
            String titleKey,
            SettingsDraft draft,
            String parentKey,
            String sectionKey,
            Supplier<Object> sectionSupplier,
            Runnable onChange
    ) {
        super(titleKey, draft, sectionKey, parentKey, sectionSupplier, onChange, Collections.emptySet());
    }
}
