package com.example.masrouf1;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.masrouf1.db.MasroufEntities.PointsLogEntity;
import com.example.masrouf1.db.MasroufEntities.RewardEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.masrouf.controller.SessionStore;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Points and real, parent-provided rewards. The reward sound only plays after a Room redemption. */
public final class RewardsActivity extends SectionActivity {
    private MasroufViewModel viewModel;
    private TextView points,level,next,emptyRewards,emptyHistory;
    private ProgressBar progress;
    private LinearLayout rewardRows,historyRows;
    private long userId;

    @Override protected int titleResource(){return R.string.page_rewards;}
    @Override protected boolean disallowParentRole(){return true;}
    @Override protected View createSectionContent(){
        View root=getLayoutInflater().inflate(R.layout.activity_rewards,null,false);
        points=root.findViewById(R.id.rewards_points);level=root.findViewById(R.id.rewards_level);next=root.findViewById(R.id.rewards_next_level);progress=root.findViewById(R.id.rewards_progress);
        emptyRewards=root.findViewById(R.id.rewards_empty);emptyHistory=root.findViewById(R.id.points_history_empty);rewardRows=root.findViewById(R.id.rewards_rows);historyRows=root.findViewById(R.id.points_history_rows);
        userId=SessionStore.userId(this);viewModel=new ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);refresh();return root;
    }
    private void refresh(){
        viewModel.<RewardsData>request(repository->new RewardsData(repository.user(userId),repository.pointsHistory(userId),repository.rewardsForUser(userId)),state->{
            if(state.data==null){emptyHistory.setText(R.string.dashboard_read_error);emptyHistory.setVisibility(View.VISIBLE);return;}
            UserEntity user=state.data.user;int score=user==null?0:Math.max(0,user.points);points.setText(getString(R.string.points_balance,score));
            if(score<100){level.setText(R.string.points_level_beginner);progress.setProgress(Math.min(100,score));next.setText(getString(R.string.points_next_level,100-score));}
            else if(score<300){level.setText(R.string.points_level_saver);progress.setProgress(Math.min(100,(score-100)/2));next.setText(getString(R.string.points_next_level,300-score));}
            else{level.setText(R.string.points_level_expert);progress.setProgress(100);next.setText(R.string.points_level_max);}
            renderRewards(state.data.rewards,score);renderHistory(state.data.history);
        });
    }
    private void renderRewards(List<RewardEntity> rewards,int score){
        rewardRows.removeAllViews();if(rewards==null||rewards.isEmpty()){emptyRewards.setVisibility(View.VISIBLE);return;}emptyRewards.setVisibility(View.GONE);
        for(RewardEntity reward:rewards){
            MaterialCardView card=new MaterialCardView(this);card.setCardBackgroundColor(getColor(R.color.alabaster));card.setRadius(dp(16));card.setStrokeWidth(dp(1));card.setStrokeColor(getColor(R.color.card_border));
            LinearLayout row=new LinearLayout(this);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);TextView title=new TextView(this);title.setText(reward.title==null?"":reward.title);title.setTextColor(getColor(R.color.forest));title.setTypeface(null,android.graphics.Typeface.BOLD);copy.addView(title);
            TextView cost=new TextView(this);cost.setText(getString(R.string.reward_points_cost,reward.pointsCost));cost.setTextColor(getColor(R.color.muted_green));copy.addView(cost);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            if(reward.redeemed){TextView status=new TextView(this);status.setText(R.string.reward_redeemed);status.setTextColor(getColor(R.color.emerald));row.addView(status);}
            else if(score>=reward.pointsCost){MaterialButton redeem=new MaterialButton(this);redeem.setText(R.string.reward_redeem);redeem.setAllCaps(false);redeem.setOnClickListener(v->redeemReward(reward.id,redeem));row.addView(redeem);}
            else{TextView status=new TextView(this);status.setText(R.string.reward_locked);status.setTextColor(getColor(R.color.muted_green));row.addView(status);}
            card.addView(row);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(8);rewardRows.addView(card,params);
        }
    }
    private void renderHistory(List<PointsLogEntity> history){
        historyRows.removeAllViews();if(history==null||history.isEmpty()){emptyHistory.setVisibility(View.VISIBLE);return;}emptyHistory.setVisibility(View.GONE);
        DateFormat date=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT);
        for(PointsLogEntity entry:history){TextView row=new TextView(this);String reason="budget-period-completed".equals(entry.reason)?getString(R.string.points_reason_budget):"reward-redeemed".equals(entry.reason)?getString(R.string.points_reason_reward):entry.reason;
            row.setText(getString(R.string.points_history_row,entry.points,date.format(new Date(entry.createdAt)),reason));row.setTextColor(getColor(R.color.ink));row.setPadding(dp(8),dp(8),dp(8),dp(8));historyRows.addView(row,new LinearLayout.LayoutParams(-1,-2));}
    }
    private void redeemReward(long rewardId,MaterialButton button){
        button.setEnabled(false);viewModel.<Boolean>request(repository->repository.redeemReward(userId,rewardId),state->{
            if(Boolean.TRUE.equals(state.data)){AchievementPlayer.play(this);refresh();}
            else{button.setEnabled(true);android.widget.Toast.makeText(this,R.string.reward_redeem_failed,android.widget.Toast.LENGTH_SHORT).show();}
        });
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private static final class RewardsData {final UserEntity user;final List<PointsLogEntity> history;final List<RewardEntity> rewards;RewardsData(UserEntity user,List<PointsLogEntity> history,List<RewardEntity> rewards){this.user=user;this.history=history==null?new ArrayList<>():history;this.rewards=rewards==null?new ArrayList<>():rewards;}}
}
