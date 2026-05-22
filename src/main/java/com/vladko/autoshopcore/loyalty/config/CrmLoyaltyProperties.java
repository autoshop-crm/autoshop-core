package com.vladko.autoshopcore.loyalty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crm.loyalty")
public class CrmLoyaltyProperties {
    private boolean enabled = true;
    private boolean earnEnabled = true;
    private boolean spendEnabled = true;
    private boolean visible = true;

    public boolean enabled() {
        return enabled;
    }

    public boolean earnEnabled() {
        return earnEnabled;
    }

    public boolean spendEnabled() {
        return spendEnabled;
    }

    public boolean visible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEarnEnabled() {
        return earnEnabled;
    }

    public void setEarnEnabled(boolean earnEnabled) {
        this.earnEnabled = earnEnabled;
    }

    public boolean isSpendEnabled() {
        return spendEnabled;
    }

    public void setSpendEnabled(boolean spendEnabled) {
        this.spendEnabled = spendEnabled;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
