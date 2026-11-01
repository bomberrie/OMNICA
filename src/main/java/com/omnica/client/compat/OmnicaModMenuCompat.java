package com.omnica.client.compat;

import com.omnica.client.gui.OmnicaConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class OmnicaModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OmnicaConfigScreen::new;
    }
}
