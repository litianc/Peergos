package peergos.server.space;

import peergos.shared.crypto.hash.*;

public interface UserUsageStore extends WriterUsageStore {

    void addUserIfAbsent(String username);

    UserUsage getUsage(String owner);

    void confirmUsage(String owner, PublicKeyHash writer, long usageDelta);

    void clearPendingUsage(String username, PublicKeyHash writer);

    void addPendingUsage(String username, PublicKeyHash writer, int size);

    void setErrored(boolean errored, String username, PublicKeyHash writer);

}
