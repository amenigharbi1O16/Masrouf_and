package com.masrouf.controller;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.GiftEnvelopeData;
import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.NotificationsActivity;
import com.example.masrouf1.OperationResult;
import com.example.masrouf1.R;
import com.example.masrouf1.SystemIntents;
import com.example.masrouf1.db.BudgetHistoryRow;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.MoneyRequestSummary;
import com.example.masrouf1.ui.AlertBannerView;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parent household & children management screen ("Enfants") aligned with RN reference:
 * - Header w/ back navigation, notification bell w/ unread badge, and profile icon
 * - Eyebrow + "Mes Enfants (N)" + dark pill "Ajouter un enfant"
 * - Styled empty-state card with emerald "+ Ajouter un enfant" CTA
 * - Rich child cards w/ email, creation date, "Enfant" badge, allocated/spent/remaining progress,
 *   Give Money, Reset Password, Call (ACTION_DIAL), SMS (ACTION_SENDTO), and 4-month comparison
 * - Pending money request approval/rejection and voice-enabled warm-beige Add Child modal
 */
public final class ChildrenManagementActivity extends AppCompatActivity {
    private MasroufViewModel viewModel;
    private VoiceFieldHelper voiceFieldHelper;
    private long parentId;
    private RecyclerView recycler;
    private ChildAdapter adapter;
    private LinearLayout requestRows;
    private View requestCard, emptyCard;
    private TextView countTitle, bellBadge;
    private MaterialButton createTopBtn, createEmptyBtn;
    private boolean authorizedParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_children_management);
        parentId = SessionStore.userId(this);
        voiceFieldHelper = new VoiceFieldHelper(this, this, null);

        findViewById(R.id.children_back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.children_bell_btn).setOnClickListener(
                v -> startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.children_profile_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_CENTRAL_TAB, "settings")
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });

        bellBadge = findViewById(R.id.children_bell_badge);
        countTitle = findViewById(R.id.children_count_title);
        if (countTitle != null) {
            countTitle.setText(getString(R.string.children_my_children_count, 0));
        }
        emptyCard = findViewById(R.id.children_empty_card);
        requestRows = findViewById(R.id.parent_money_requests_rows);
        requestCard = findViewById(R.id.parent_money_requests_card);

        recycler = findViewById(R.id.children_management_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildAdapter();
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this, new MasroufViewModelFactory(this)).get(MasroufViewModel.class);

        createTopBtn = findViewById(R.id.child_management_create);
        createEmptyBtn = findViewById(R.id.child_management_create_empty);
        if (createTopBtn != null) createTopBtn.setEnabled(false);
        if (createEmptyBtn != null) createEmptyBtn.setEnabled(false);

        View.OnClickListener openAddChild = v -> {
            if (authorizedParent) showAddChildDialog();
        };
        if (createTopBtn != null) createTopBtn.setOnClickListener(openAddChild);
        if (createEmptyBtn != null) createEmptyBtn.setOnClickListener(openAddChild);

        viewModel.<UserEntity>request(repository -> repository.user(parentId), state -> {
            if (state.loading) return;
            UserEntity account = state.data;
            AppRole role = account == null ? AppRole.USER : AppRole.from(account.role);
            authorizedParent = role == AppRole.PARENT;
            if (!authorizedParent) {
                Toast.makeText(this, R.string.parent_budget_required, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (createTopBtn != null) createTopBtn.setEnabled(true);
            if (createEmptyBtn != null) createEmptyBtn.setEnabled(true);
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authorizedParent) refresh();
    }

    @Override
    protected void onDestroy() {
        if (voiceFieldHelper != null) voiceFieldHelper.destroy();
        super.onDestroy();
    }

    private void refresh() {
        viewModel.<Integer>request(repository -> repository.unreadNotificationCount(parentId), state -> {
            if (bellBadge == null || state.data == null) return;
            if (state.data > 0) {
                bellBadge.setText(state.data > 99 ? "99+" : String.valueOf(state.data));
                bellBadge.setVisibility(View.VISIBLE);
            } else {
                bellBadge.setVisibility(View.GONE);
            }
        });

        viewModel.<List<ChildSpendingSummary>>request(repository -> repository.familyChildren(parentId), state -> {
            if (state.loading) return;
            if (state.failed) {
                Toast.makeText(this, R.string.dashboard_read_error, Toast.LENGTH_SHORT).show();
                return;
            }
            List<ChildSpendingSummary> rows = state.data == null ? Collections.emptyList() : state.data;
            adapter.submit(rows);
            if (countTitle != null) {
                countTitle.setText(getString(R.string.children_my_children_count, rows.size()));
            }
            boolean isEmpty = rows.isEmpty();
            if (emptyCard != null) emptyCard.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
        loadMoneyRequests();
    }

    private void showAddChildDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_add_child, null, false);
        EditText name = form.findViewById(R.id.dialog_child_name);
        EditText email = form.findViewById(R.id.dialog_child_email);
        EditText password = form.findViewById(R.id.dialog_child_password);
        EditText phone = form.findViewById(R.id.dialog_child_phone);
        EditText allocation = form.findViewById(R.id.dialog_child_allocation);
        TextView error = form.findViewById(R.id.dialog_child_error);
        AlertBannerView voiceBanner = form.findViewById(R.id.dialog_child_voice_banner);

        voiceFieldHelper.setStatusBanner(voiceBanner);
        voiceFieldHelper.bindField(form.findViewById(R.id.dialog_child_name_mic), name);
        voiceFieldHelper.bindField(form.findViewById(R.id.dialog_child_allocation_mic), allocation);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_child)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.add_child, null)
                .create();
        dialog.setOnDismissListener(d -> voiceFieldHelper.setStatusBanner(null));
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            error.setVisibility(View.GONE);
            String childName = name.getText().toString().trim();
            String childEmail = InputValidation.normalizeGmail(email.getText().toString());
            String secret = password.getText().toString();
            String parentPhone = phone.getText().toString().trim();
            if (childName.isEmpty()) {
                error.setText(R.string.child_name_hint);
                error.setVisibility(View.VISIBLE);
                return;
            }
            if (!InputValidation.isStrictGmail(childEmail)) {
                error.setText(R.string.gmail_invalid);
                error.setVisibility(View.VISIBLE);
                return;
            }
            if (!InputValidation.isValidPassword(secret)) {
                error.setText(R.string.password_rules);
                error.setVisibility(View.VISIBLE);
                return;
            }
            long initial = 0L;
            String allocText = allocation.getText().toString().trim();
            if (!allocText.isEmpty()) {
                try {
                    initial = MoneyInput.toMilli(allocText);
                    if (initial < 0L) throw new NumberFormatException();
                } catch (NumberFormatException invalid) {
                    error.setText(R.string.money_invalid);
                    error.setVisibility(View.VISIBLE);
                    return;
                }
            }
            createChildAccount(dialog, error, childName, childEmail, secret, parentPhone, initial);
        }));
        dialog.show();
    }

    private void createChildAccount(AlertDialog dialog, TextView error, String childName, String childEmail,
                                    String secret, String parentPhone, long initial) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        viewModel.<OperationResult<Long>>request(repository -> {
            long now = System.currentTimeMillis();
            BudgetEntity parentBudget = repository.activeBudget(parentId);
            UserEntity child = new UserEntity();
            child.name = childName;
            child.email = childEmail;
            child.role = AppRole.CHILD.value();
            child.accountType = AppRole.CHILD.value();
            child.balanceMilli = initial;
            child.createdAt = now;
            ChildProfileEntity profile = new ChildProfileEntity();
            profile.parentUserId = parentId;
            profile.nickname = childName;
            profile.parentPhone = parentPhone;
            profile.allocatedTotalMilli = initial;
            profile.createdAt = now;
            BudgetEntity initialBudget = new BudgetEntity();
            initialBudget.name = getString(R.string.child_total_label);
            initialBudget.monthlyLimitMilli = initial;
            initialBudget.periodStart = parentBudget == null ? now : parentBudget.periodStart;
            initialBudget.periodEnd = parentBudget == null ? now : parentBudget.periodEnd;
            initialBudget.status = "ACTIVE";
            return repository.createChildAccount(child, profile, initialBudget, Collections.emptyList(), secret);
        }, state -> {
            if (state.loading) return;
            OperationResult<Long> result = state.data;
            if (result == null || !result.succeeded()) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                String failure = result == null ? "save" : result.error;
                if ("gmail".equals(failure)) error.setText(R.string.gmail_invalid);
                else if ("password".equals(failure)) error.setText(R.string.password_rules);
                else if ("duplicate".equals(failure)) error.setText(R.string.email_error);
                else if ("parent".equals(failure)) error.setText(R.string.parent_budget_required);
                else if (failure != null && failure.contains("budget")) error.setText(R.string.parent_allocation_cap);
                else error.setText(R.string.child_creation_invalid);
                error.setVisibility(View.VISIBLE);
                return;
            }
            dialog.dismiss();
            refresh();
            showCredentialsDialog(childEmail, secret);
        });
    }

    private void showCredentialsDialog(String childEmail, String secret) {
        String message = getString(R.string.child_credentials_message, childEmail, secret);
        new AlertDialog.Builder(this)
                .setTitle(R.string.child_credentials_title)
                .setMessage(message)
                .setNegativeButton(android.R.string.copy, (d, which) -> SystemIntents.copyToClipboard(this, message))
                .setNeutralButton(R.string.share_receipt, (d, which) -> SystemIntents.sendSms(this, "", message))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showGiveMoneyDialog(ChildSpendingSummary child) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_give_money, null, false);
        EditText amountInput = form.findViewById(R.id.dialog_gift_amount);
        EditText noteInput = form.findViewById(R.id.dialog_gift_note);
        TextView error = form.findViewById(R.id.dialog_gift_error);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.give_money_title, child.nickname))
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.donate_money, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            error.setVisibility(View.GONE);
            long amount;
            try {
                amount = MoneyInput.toMilli(amountInput.getText().toString());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException invalid) {
                error.setText(R.string.money_invalid);
                error.setVisibility(View.VISIBLE);
                return;
            }
            String note = noteInput.getText().toString().trim();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            inspectChildEnvelope(dialog, error, child, amount, note);
        }));
        dialog.show();
    }

    private void inspectChildEnvelope(AlertDialog dialog, TextView error, ChildSpendingSummary child, long amount, String note) {
        viewModel.<GiftEnvelopeData>request(repository -> repository.giftEnvelope(parentId), state -> {
            if (state.loading) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            GiftEnvelopeData envelope = state.data;
            if (envelope == null || envelope.budget == null) {
                error.setText(R.string.parent_budget_required);
                error.setVisibility(View.VISIBLE);
                return;
            }
            if (envelope.ready && amount > envelope.remainingMilli) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.donate_money)
                        .setMessage(getString(R.string.budget_envelope_warning,
                                CurrencyFormatter.formatMilli(amount),
                                CurrencyFormatter.formatMilli(Math.max(0L, envelope.remainingMilli))))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.donate_money, (w, which) -> commitGift(dialog, error, child, amount, note))
                        .show();
                return;
            }
            commitGift(dialog, error, child, amount, note);
        });
    }

    private void commitGift(AlertDialog dialog, TextView error, ChildSpendingSummary child, long amount, String note) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        viewModel.<Integer>request(repository -> repository.giveMoney(parentId, child.childProfileId, amount, note, System.currentTimeMillis()), state -> {
            if (state.loading) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            Integer result = state.data;
            if (result != null && result == -2) {
                error.setText(R.string.parent_allocation_cap);
                error.setVisibility(View.VISIBLE);
                return;
            }
            if (result == null || result != 0) {
                error.setText(R.string.parent_budget_required);
                error.setVisibility(View.VISIBLE);
                return;
            }
            dialog.dismiss();
            refresh();
        });
    }

    private void showResetPasswordDialog(ChildSpendingSummary child) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setBackgroundColor(getColor(R.color.form_beige));
        int p = dp(20);
        wrap.setPadding(p, dp(10), p, dp(12));

        TextView label = new TextView(this);
        label.setText(R.string.password);
        label.setTextColor(getColor(R.color.black));
        label.setTextSize(13f);
        label.setTypeface(null, Typeface.BOLD);
        wrap.addView(label);

        EditText passwordField = new EditText(this);
        passwordField.setBackgroundResource(R.drawable.bg_allocation_input);
        passwordField.setHint(R.string.hint_password);
        passwordField.setTextColor(getColor(R.color.black));
        passwordField.setHintTextColor(getColor(R.color.ink_secondary));
        passwordField.setMinHeight(dp(48));
        passwordField.setPadding(dp(12), dp(10), dp(12), dp(10));
        passwordField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, -2);
        inputLp.topMargin = dp(6);
        wrap.addView(passwordField, inputLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.reset_child_password)
                .setMessage(R.string.password_rules)
                .setView(wrap)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reset_child_password, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String next = passwordField.getText().toString();
            if (!InputValidation.isValidPassword(next)) {
                passwordField.setError(getString(R.string.password_rules));
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            viewModel.<OperationResult<Boolean>>request(repository -> repository.resetChildPassword(child.childProfileId, parentId, next), result -> {
                if (result.loading) return;
                if (result.data == null || !result.data.succeeded()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, R.string.transaction_saved_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                viewModel.<String>request(repo -> repo.childEmail(parentId, child.childProfileId), email -> {
                    if (!email.loading) showCredentialsDialog(email.data == null ? "" : email.data, next);
                });
            });
        }));
        dialog.show();
    }

    private void loadMoneyRequests() {
        viewModel.<List<MoneyRequestSummary>>request(repository -> repository.moneyRequestSummariesForParent(parentId), state -> {
            if (state.loading) return;
            requestRows.removeAllViews();
            List<MoneyRequestSummary> requests = state.data;
            if (state.failed || requests == null || requests.isEmpty()) {
                requestCard.setVisibility(View.GONE);
                return;
            }
            requestCard.setVisibility(View.VISIBLE);
            for (MoneyRequestSummary req : requests) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setBackgroundResource(R.drawable.bg_card_form_beige);
                int pad = dp(12);
                row.setPadding(pad, pad, pad, pad);

                String reqHeadline = getString(R.string.money_request_from_child, req.childName,
                        CurrencyFormatter.formatMilli(req.amountMilli));
                if (req.reason != null && !req.reason.trim().isEmpty()) {
                    reqHeadline += " · " + req.reason.trim();
                }
                TextView title = new TextView(this);
                title.setText(reqHeadline);
                title.setTextColor(getColor(R.color.black));
                title.setTextSize(14f);
                title.setTypeface(null, Typeface.BOLD);
                row.addView(title);

                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(-1, -2);
                actLp.topMargin = dp(8);

                MaterialButton approve = new MaterialButton(this);
                approve.setText(R.string.money_request_approve);
                approve.setAllCaps(false);
                approve.setTextColor(Color.WHITE);
                approve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.emerald)));
                approve.setCornerRadius(dp(12));
                approve.setOnClickListener(v -> resolveMoneyRequest(req.id, true));

                MaterialButton reject = new MaterialButton(this);
                reject.setText(R.string.money_request_reject);
                reject.setAllCaps(false);
                reject.setTextColor(Color.WHITE);
                reject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.coral)));
                reject.setCornerRadius(dp(12));
                reject.setOnClickListener(v -> resolveMoneyRequest(req.id, false));

                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, -2, 1f);
                btnLp.setMarginEnd(dp(8));
                actions.addView(approve, btnLp);
                actions.addView(reject, new LinearLayout.LayoutParams(0, -2, 1f));
                row.addView(actions, actLp);

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = dp(8);
                requestRows.addView(row, rowLp);
            }
        });
    }

    private void resolveMoneyRequest(long requestId, boolean approve) {
        viewModel.<Integer>request(repository -> repository.resolveMoneyRequest(parentId, requestId, approve, ""), result -> {
            if (result.loading) return;
            Integer code = result.data;
            if (code != null && code == -2) {
                Toast.makeText(this, R.string.parent_allocation_cap, Toast.LENGTH_SHORT).show();
                return;
            }
            if (code == null || code != 0) {
                Toast.makeText(this, R.string.transaction_saved_error, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, approve ? R.string.money_request_accepted : R.string.money_request_rejected, Toast.LENGTH_SHORT).show();
            refresh();
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private final class ChildAdapter extends RecyclerView.Adapter<ChildHolder> {
        private final List<ChildSpendingSummary> rows = new ArrayList<>();
        private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        private final DateFormat createdFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

        void submit(List<ChildSpendingSummary> children) {
            rows.clear();
            if (children != null) rows.addAll(children);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ChildHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChildHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_management, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ChildHolder holder, int position) {
            ChildSpendingSummary child = rows.get(position);
            holder.name.setText(child.nickname);

            if (holder.email != null) {
                String mail = child.email == null ? "" : child.email.trim();
                holder.email.setText(mail);
                holder.email.setVisibility(mail.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (holder.created != null) {
                if (child.createdAt > 0L) {
                    holder.created.setText(getString(R.string.children_created_on, createdFormat.format(new Date(child.createdAt))));
                    holder.created.setVisibility(View.VISIBLE);
                } else {
                    holder.created.setVisibility(View.GONE);
                }
            }

            long remaining = Math.max(0L, child.availableMilli());
            holder.ratio.setText(getString(R.string.child_budget_ratio,
                    CurrencyFormatter.formatMilli(child.spentMilli),
                    CurrencyFormatter.formatMilli(child.allocatedTotalMilli)));
            holder.available.setText(getString(R.string.child_available_summary,
                    CurrencyFormatter.formatMilli(remaining)));
            int percent = MoneyMath.percentOf(child.spentMilli, child.allocatedTotalMilli);
            holder.percent.setText(percent + "%");
            holder.progress.setProgress(Math.min(100, Math.max(0, percent)));
            holder.balance.setText(getString(R.string.child_balance_summary,
                    CurrencyFormatter.formatMilli(child.balanceMilli)));

            holder.give.setOnClickListener(v -> showGiveMoneyDialog(child));
            holder.reset.setOnClickListener(v -> showResetPasswordDialog(child));

            if (holder.call != null) {
                holder.call.setOnClickListener(v -> {
                    String phone = child.parentPhone == null ? "" : child.parentPhone.trim();
                    if (phone.isEmpty()) {
                        Toast.makeText(ChildrenManagementActivity.this, R.string.children_no_phone, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SystemIntents.dial(ChildrenManagementActivity.this, phone);
                });
            }
            if (holder.sms != null) {
                holder.sms.setOnClickListener(v -> {
                    String phone = child.parentPhone == null ? "" : child.parentPhone.trim();
                    String msg = getString(R.string.children_sms_greeting, child.nickname);
                    SystemIntents.sendSms(ChildrenManagementActivity.this, phone, msg);
                });
            }

            holder.history.setVisibility(View.GONE);
            holder.toggle.setOnClickListener(v -> toggleHistory(holder, child));
        }

        private void toggleHistory(ChildHolder holder, ChildSpendingSummary child) {
            if (holder.history.getVisibility() == View.VISIBLE) {
                holder.history.setVisibility(View.GONE);
                return;
            }
            viewModel.<List<BudgetHistoryRow>>request(repository -> repository.childHistory(child.childUserId, parentId), state -> {
                if (state.loading) return;
                List<BudgetHistoryRow> history = state.data;
                holder.history.removeAllViews();
                holder.history.setVisibility(View.VISIBLE);
                if (history == null || history.isEmpty()) {
                    TextView empty = new TextView(ChildrenManagementActivity.this);
                    empty.setText(R.string.history_none);
                    empty.setTextColor(getColor(R.color.black));
                    empty.setTextSize(13f);
                    holder.history.addView(empty);
                    return;
                }
                int limit = Math.min(4, history.size());
                for (int i = 0; i < limit; i++) {
                    BudgetHistoryRow row = history.get(i);
                    long rem = Math.max(0L, MoneyMath.subtract(row.allocatedMilli, row.spentMilli));
                    int pct = MoneyMath.percentOf(row.spentMilli, row.allocatedMilli);
                    TextView line = new TextView(ChildrenManagementActivity.this);
                    line.setText(getString(R.string.history_month_row,
                            monthFormat.format(new Date(row.periodStart)),
                            CurrencyFormatter.formatMilli(row.allocatedMilli),
                            CurrencyFormatter.formatMilli(row.spentMilli),
                            CurrencyFormatter.formatMilli(rem),
                            pct));
                    line.setTextColor(getColor(R.color.black));
                    line.setTextSize(13f);
                    holder.history.addView(line);
                }
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class ChildHolder extends RecyclerView.ViewHolder {
        final TextView name, email, created, ratio, available, percent, balance;
        final ProgressBar progress;
        final MaterialButton give, reset, call, sms, toggle;
        final LinearLayout history;

        ChildHolder(View view) {
            super(view);
            name = view.findViewById(R.id.child_row_name);
            email = view.findViewById(R.id.child_row_email);
            created = view.findViewById(R.id.child_row_created);
            ratio = view.findViewById(R.id.child_row_ratio);
            available = view.findViewById(R.id.child_row_available);
            percent = view.findViewById(R.id.child_row_percent);
            balance = view.findViewById(R.id.child_row_balance);
            progress = view.findViewById(R.id.child_row_progress);
            give = view.findViewById(R.id.child_row_give);
            reset = view.findViewById(R.id.child_row_reset);
            call = view.findViewById(R.id.child_row_call);
            sms = view.findViewById(R.id.child_row_sms);
            toggle = view.findViewById(R.id.child_row_history_toggle);
            history = view.findViewById(R.id.child_row_history);
        }
    }
}
