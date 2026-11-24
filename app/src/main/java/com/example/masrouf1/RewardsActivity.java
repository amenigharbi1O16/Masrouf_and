package com.example.masrouf1;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.masrouf.controller.SessionStore;


public final class RewardsActivity extends SectionActivity {
    private TextView points;
    private MasroufViewModel viewModel;
    @Override protected int titleResource() { return R.string.page_rewards; }
    @Override protected View createSectionContent() {
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER);
        points = bodyText(getString(R.string.points_balance,0)); points.setGravity(Gravity.CENTER);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        viewModel.loadUser(SessionStore.userId(this));
        viewModel.user().observe(this,state->{if(state!=null&&state.data!=null&&points!=null)points.setText(getString(R.string.points_balance,state.data.points));});
        content.addView(points, new LinearLayout.LayoutParams(-1, 0, 1f));
        MaterialButton listen = new MaterialButton(this); listen.setText("♪  Écouter la récompense"); listen.setAllCaps(false);
        listen.setOnClickListener(v -> AchievementPlayer.play(this));
        content.addView(listen, new LinearLayout.LayoutParams(-1, dp(52)));
        return content;
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
