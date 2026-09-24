package com.finsec.accounting.ui;

public class UserSession {
    private static UserSession instance;
    private String activeUserId;
    private String activeOrganizationId;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public String getActiveOrganizationId() { return activeOrganizationId; }
    public void setActiveOrganizationId(String orgId) { this.activeOrganizationId = orgId; }
    public String getActiveUserId() { return activeUserId; }
    public void setActiveUserId(String userId) { this.activeUserId = userId; }
}
