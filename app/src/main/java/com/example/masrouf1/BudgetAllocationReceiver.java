package com.example.masrouf1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Converts a parent-set allowance increase into the child's local notification. */
public final class BudgetAllocationReceiver extends BroadcastReceiver {
    public static final String ACTION_CHILD_ALLOCATION="com.masrouf.action.CHILD_ALLOCATION";
    public static final String EXTRA_CHILD_USER_ID="child_user_id";
    public static final String EXTRA_AMOUNT_MILLI="amount_milli";
    @Override public void onReceive(Context context,Intent intent) {
        if(intent==null||!ACTION_CHILD_ALLOCATION.equals(intent.getAction())) return;
        long childId=intent.getLongExtra(EXTRA_CHILD_USER_ID,0L), amount=intent.getLongExtra(EXTRA_AMOUNT_MILLI,0L);
        if(childId<=0||amount<=0) return;
        BudgetNotificationManager.allowanceReceived(context,(int)(childId%Integer.MAX_VALUE),childId,amount);
    }
}
