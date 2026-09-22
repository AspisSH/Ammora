package com.ammora.mod.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for pending corporate invitations.
 * Ensures players cannot be forcibly added to a company without their consent.
 */
public final class CompanyInviteManager {

    private static final CompanyInviteManager INSTANCE = new CompanyInviteManager();
    public static CompanyInviteManager getInstance() { return INSTANCE; }

    public record CompanyInvite(String companyId, String companyName, UUID inviterUuid, String inviterName, long timestamp) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000L;
        }
    }

    private final Map<UUID, CompanyInvite> pendingInvites = new ConcurrentHashMap<>();

    private CompanyInviteManager() {}

    public void createInvite(UUID targetUuid, String companyId, String companyName, UUID inviterUuid, String inviterName) {
        pendingInvites.put(targetUuid, new CompanyInvite(companyId, companyName, inviterUuid, inviterName, System.currentTimeMillis()));
    }

    public CompanyInvite getPendingInvite(UUID targetUuid) {
        CompanyInvite invite = pendingInvites.get(targetUuid);
        if (invite != null && !invite.isExpired()) {
            return invite;
        }
        if (invite != null) {
            pendingInvites.remove(targetUuid);
        }
        return null;
    }

    public CompanyInvite removeInvite(UUID targetUuid) {
        return pendingInvites.remove(targetUuid);
    }

    public boolean hasPendingInvite(UUID targetUuid, String companyId) {
        CompanyInvite invite = getPendingInvite(targetUuid);
        return invite != null && invite.companyId().equals(companyId);
    }
}
