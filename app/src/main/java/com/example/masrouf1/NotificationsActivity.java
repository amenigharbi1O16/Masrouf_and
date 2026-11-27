/*
 * ============================================================================
 * FICHIER : NotificationsActivity.java
 * MISSION : Centre de notifications locales Room (boîte de réception filtrable par compte connecté).
 * IMPLÉMENTE : Hérite de SectionActivity et gonfle activity_notifications.xml + RecyclerView NotificationAdapter.
 * EFFET À L'ÉCRAN : Affiche le nombre d'alertes non lues, les puces Chip « Toutes » / « Non lues »,
 *                   le bouton « Tout marquer lu », marque comme lue la carte cliquée et lance CongratulationsActivity
 *                   lorsqu'une notification de type "budget_closed" est ouverte.
 * COMMUNICATIONS : Observe MasroufViewModel.notifications(), appelle markNotificationRead / markAllNotificationsRead.
 * ============================================================================
 */
package com.example.masrouf1;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.masrouf.controller.SessionStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Centre de notifications locales Room héritant de SectionActivity.
 */
public final class NotificationsActivity extends SectionActivity {
    private long userId;
    private List<NotificationEntity> all=new ArrayList<>();
    private MasroufViewModel viewModel;
    private NotificationAdapter adapter;
    private ChipGroup filters;
    private TextView count,empty;
    private int allChipId,unreadChipId;

    /** Titre affiché dans la barre supérieure de SectionActivity. */
    @Override protected int titleResource(){return R.string.page_notifications;}

    /** Construit l'écran, configure les filtres Chip, la liste RecyclerView et l'observation LiveData Room. */
    @Override protected View createSectionContent(){
        View root=getLayoutInflater().inflate(R.layout.activity_notifications,null,false);userId=SessionStore.userId(this);
        filters=root.findViewById(R.id.notifications_filter);count=root.findViewById(R.id.notifications_unread_count);empty=root.findViewById(R.id.notifications_empty);
        // Crée dynamiquement les deux puces de filtrage ("Toutes" et "Non lues") et coche "Toutes" par défaut.
        Chip allChip=new Chip(this);allChip.setId(View.generateViewId());allChip.setText(R.string.notifications_filter_all);allChip.setCheckable(true);filters.addView(allChip);allChipId=allChip.getId();
        Chip unreadChip=new Chip(this);unreadChip.setId(View.generateViewId());unreadChip.setText(R.string.notifications_filter_unread);unreadChip.setCheckable(true);filters.addView(unreadChip);unreadChipId=unreadChip.getId();filters.check(allChipId);
        // Configure la liste des notifications ; au clic, marque lue en base et redirige vers CongratulationsActivity si clôture.
        RecyclerView list=root.findViewById(R.id.notifications_list);list.setLayoutManager(new LinearLayoutManager(this));adapter=new NotificationAdapter(row->{if(!row.read){viewModel.markNotificationRead(row.id,userId);row.read=true;applyFilter();updateCount();}if("budget_closed".equals(row.kind)){startActivity(new android.content.Intent(this,com.masrouf.controller.CongratulationsActivity.class).putExtra("POINTS_EARNED",100));}});list.setAdapter(adapter);
        empty.setVisibility(View.GONE);
        // Réapplique le filtre à chaque bascule de puce Chip.
        filters.setOnCheckedStateChangeListener((group,checked)->applyFilter());
        // Bouton marquant toutes les notifications du compte comme lues dans SQLite.
        ((MaterialButton)root.findViewById(R.id.notifications_mark_all_read)).setOnClickListener(v->{viewModel.markAllNotificationsRead(userId);for(NotificationEntity row:all)row.read=true;applyFilter();updateCount();});
        // Observe le flux LiveData notifications() de MasroufViewModel pour mettre à jour la liste en direct.
        viewModel=new ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        viewModel.notifications().observe(this,state->{if(state.loading)return;if(state.failed||state.data==null){all=new ArrayList<>();empty.setText(R.string.dashboard_read_error);empty.setVisibility(View.VISIBLE);adapter.submit(all);updateCount();return;}all=new ArrayList<>(state.data);applyFilter();updateCount();});
        viewModel.loadNotifications(userId);return root;
    }

    /** Recalcule le compteur d'alertes non lues et l'affiche au format localisé. */
    private void updateCount(){int unread=0;for(NotificationEntity row:all)if(!row.read)unread++;count.setText(getString(R.string.notifications_unread_count,unread));}

    /** Filtre la liste selon l'état du ChipGroup puis gère l'affichage de l'état vide éventuel. */
    private void applyFilter(){
        if(adapter==null)return;boolean onlyUnread=filters.getCheckedChipId()==unreadChipId;ArrayList<NotificationEntity> visible=new ArrayList<>();for(NotificationEntity row:all)if(!onlyUnread||!row.read)visible.add(row);
        adapter.submit(visible);if(all.isEmpty()){empty.setText(R.string.notifications_empty);empty.setVisibility(View.VISIBLE);}else if(visible.isEmpty()){empty.setText(R.string.notifications_no_unread);empty.setVisibility(View.VISIBLE);}else empty.setVisibility(View.GONE);
    }
}
