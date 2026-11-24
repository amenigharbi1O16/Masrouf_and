package com.example.masrouf1.db;

import com.example.masrouf1.MoneyMath;

public final class ChildSpendingSummary {
    public long childProfileId;
    public long childUserId;
    public String nickname;
    public long allocatedTotalMilli;
    public long spentMilli;
    public long balanceMilli;
    public long availableMilli() { return MoneyMath.subtract(allocatedTotalMilli,spentMilli); }
}
