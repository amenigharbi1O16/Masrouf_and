package com.example.masrouf1;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.masrouf.controller.SessionStore;

/** Shared programmatic shell for the four non-home central tabs. */
public abstract class DashboardSectionFragment extends Fragment {
    protected LinearLayout page;
    protected MasroufViewModel viewModel;
    protected int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    protected abstract int pageTitle();
    protected abstract void createContent(LinearLayout content);
    protected void observeContent() { }

    @Override public void onCreate(@Nullable Bundle state){super.onCreate(state);viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){super.onViewCreated(view,state);observeContent();}

    @Nullable @Override public View onCreateView(@NonNull android.view.LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        ScrollView scroll=new ScrollView(requireContext());scroll.setFillViewport(true);scroll.setBackgroundColor(requireContext().getColor(R.color.cream));
        page=new LinearLayout(requireContext());page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(20),dp(20),dp(30));scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        LinearLayout header=new LinearLayout(requireContext());header.setGravity(Gravity.CENTER_VERTICAL);header.setOrientation(LinearLayout.HORIZONTAL);
        TextView menu=new TextView(requireContext());menu.setText("☰");menu.setTextSize(21);menu.setTextColor(requireContext().getColor(R.color.forest));menu.setGravity(Gravity.CENTER);menu.setContentDescription(getString(R.string.open_menu));header.addView(menu,new LinearLayout.LayoutParams(dp(44),dp(48)));menu.setOnClickListener(v->{if(requireActivity() instanceof MainActivity)((MainActivity)requireActivity()).openMenu();});
        TextView title=new TextView(requireContext());title.setText(pageTitle());title.setTextColor(requireContext().getColor(R.color.forest));title.setTextSize(23);title.setTypeface(null,1);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));page.addView(header);
        createContent(page);return scroll;
    }
    protected TextView text(String value){TextView view=new TextView(requireContext());view.setText(value);view.setTextColor(requireContext().getColor(R.color.muted_green));view.setTextSize(15);view.setGravity(Gravity.CENTER_VERTICAL);return view;}
    protected long signedInUserId(){return SessionStore.userId(requireContext());}
    protected void ui(Runnable action){if(getActivity()!=null)getActivity().runOnUiThread(()->{if(isAdded()&&getView()!=null)action.run();});}
}
