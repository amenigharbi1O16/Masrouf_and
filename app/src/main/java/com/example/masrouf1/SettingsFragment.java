package com.example.masrouf1;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;


/** Persistent account settings summary; detailed validation and contact actions live in SettingsActivity. */
public final class SettingsFragment extends DashboardSectionFragment {
    private TextView summary;
    @Override protected int pageTitle(){return R.string.nav_settings;}
    @Override protected void createContent(LinearLayout content){summary=text(getString(R.string.offline_note));content.addView(summary,new LinearLayout.LayoutParams(-1,dp(90)));MaterialButton details=new MaterialButton(requireContext());details.setText(R.string.page_settings);details.setAllCaps(false);details.setOnClickListener(v->startActivity(new Intent(requireContext(),SettingsActivity.class)));content.addView(details,new LinearLayout.LayoutParams(-1,dp(50)));}
    @Override protected void observeContent(){viewModel.user().observe(getViewLifecycleOwner(),state->{if(state.data!=null)summary.setText(state.data.name+"\n"+state.data.email+"\n"+getString(R.string.offline_note));});viewModel.loadUser(signedInUserId());}
}
