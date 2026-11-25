package com.masrouf.controller;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.masrouf1.CategoryAllocationData;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.google.android.material.tabs.TabLayout;

/** Product and expense custom categories with optional allocation percentage (can be added before/after budget creation). */
public final class CustomCategoryDialogFragment extends DialogFragment {
    private String selectedType = "product";
    private int usedPercent;
    private boolean hasActiveBudget;
    private MasroufViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(requireContext()))
                .get(MasroufViewModel.class);
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(8), dp(22), 0);

        TabLayout tabs = new TabLayout(requireContext());
        tabs.addTab(tabs.newTab().setText(R.string.category_type_products));
        tabs.addTab(tabs.newTab().setText(R.string.category_type_expenses));
        form.addView(tabs, new LinearLayout.LayoutParams(-1, dp(48)));

        EditText name = new EditText(requireContext());
        name.setId(View.generateViewId());
        name.setHint(R.string.hint_category_name);
        name.setTextColor(requireContext().getColor(R.color.ink));
        name.setHintTextColor(requireContext().getColor(R.color.muted_green));
        addLabeledField(form, name, R.string.category_name);

        EditText allocation = new EditText(requireContext());
        allocation.setId(View.generateViewId());
        allocation.setHint(R.string.hint_allocation_percent);
        allocation.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        allocation.setTextColor(requireContext().getColor(R.color.ink));
        allocation.setHintTextColor(requireContext().getColor(R.color.muted_green));
        addLabeledField(form, allocation, R.string.allocation_percent);

        TextView cap = new TextView(requireContext());
        cap.setPadding(0, dp(5), 0, dp(4));
        form.addView(cap);

        TextView error = new TextView(requireContext());
        error.setTextColor(requireContext().getColor(R.color.coral));
        form.addView(error);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_category)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                selectedType = tab.getPosition() == 0 ? "product" : "expense";
                loadUsed(cap, allocation, dialog);
            }
            public void onTabUnselected(TabLayout.Tab tab) { }
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                refreshCap(name, allocation, cap, dialog);
            }
            public void afterTextChanged(Editable e) { }
        };
        name.addTextChangedListener(watcher);
        allocation.addTextChangedListener(watcher);

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                    v -> save(name, allocation, cap, error, dialog));
            loadUsed(cap, allocation, dialog);
        });
        return dialog;
    }

    private void loadUsed(TextView cap, EditText allocation, AlertDialog dialog) {
        long userId = SessionStore.userId(requireContext());
        String kind = selectedType;
        viewModel.<CategoryAllocationData>request(
                repository -> repository.categoryAllocation(userId, kind),
                state -> {
                    if (!isAdded()) return;
                    CategoryAllocationData result = state.data;
                    usedPercent = result == null ? 0 : result.usedPercent;
                    hasActiveBudget = result != null && result.budget != null;
                    refreshCap(null, allocation, cap, dialog);
                });
    }

    private void refreshCap(EditText nameInput, EditText allocation, TextView cap, AlertDialog dialog) {
        if (cap == null) return;
        String rawAlloc = allocation.getText().toString().trim();
        int requested = 0;
        boolean invalidNum = false;
        if (!rawAlloc.isEmpty()) {
            try {
                requested = Integer.parseInt(rawAlloc);
                if (requested < 0) invalidNum = true;
            } catch (RuntimeException ignored) {
                invalidNum = true;
            }
        }
        if (!hasActiveBudget) {
            cap.setText(R.string.category_budget_required);
            cap.setTextColor(requireContext().getColor(R.color.muted_green));
        } else {
            int safeReq = Math.max(0, requested);
            int sum = usedPercent + safeReq;
            cap.setText(getString(R.string.allocation_cap_live, usedPercent, safeReq, Math.max(0, 100 - sum)));
            cap.setTextColor(requireContext().getColor(R.color.muted_green));
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!invalidNum);
        }
    }

    private void save(EditText name, EditText allocation, TextView cap, TextView error, AlertDialog dialog) {
        String label = name.getText().toString().trim();
        if (label.isEmpty()) {
            error.setText(R.string.allocation_invalid);
            return;
        }
        String rawAlloc = allocation.getText().toString().trim();
        int percent = 0;
        if (!rawAlloc.isEmpty()) {
            try {
                percent = Integer.parseInt(rawAlloc);
            } catch (NumberFormatException ex) {
                error.setText(R.string.allocation_invalid);
                return;
            }
        }
        if (percent < 0) {
            error.setText(R.string.allocation_invalid);
            return;
        }

        long userId = SessionStore.userId(requireContext());
        CategoryEntity category = new CategoryEntity();
        category.userId = userId;
        category.name = label;
        category.type = selectedType;
        category.allocationPercent = percent;
        category.colorHex = "#2E7D43";
        category.isEssential = false;

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        viewModel.<Boolean>request(
                repository -> repository.addCategory(category, userId),
                state -> {
                    if (!isAdded()) return;
                    if (Boolean.TRUE.equals(state.data)) {
                        android.widget.Toast.makeText(requireContext(), R.string.category_saved,
                                android.widget.Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().setFragmentResult("masrouf-categories-updated", new Bundle());
                        dialog.dismiss();
                    } else {
                        error.setText(R.string.allocation_overflow);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void addLabeledField(LinearLayout parent, EditText input, int labelResource) {
        TextView label = new TextView(requireContext());
        label.setText(labelResource);
        label.setLabelFor(input.getId());
        label.setTextColor(requireContext().getColor(R.color.muted_green));
        label.setTextSize(12);
        parent.addView(label, new LinearLayout.LayoutParams(-1, dp(22)));
        parent.addView(input, new LinearLayout.LayoutParams(-1, dp(54)));
    }
}
