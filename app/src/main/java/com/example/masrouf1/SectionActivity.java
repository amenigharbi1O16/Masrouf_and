/*
 * ============================================================================
 * FICHIER : SectionActivity.java
 * MISSION : Activity de base abstraite utilisée par des écrans secondaires
 *           (ex. FamilyBudgetsAnalyticsActivity, RewardsActivity). Charge
 *           activity_section.xml (en-tête + bouton retour + conteneur FrameLayout),
 *           applique un contrôle d'accès par rôle (PARENT requis ou interdit)
 *           et injecte le contenu fourni par la sous-classe.
 * IMPLÉMENTE : AppCompatActivity avec vérification du rôle dans SharedPreferences
 *              ("masrouf_preferences"), méthode template createSectionContent()
 *              et navigation retour goHome() vers MainActivity.
 * EFFET À L'ÉCRAN : En-tête de section avec flèche retour, titre centré et
 *                   zone de contenu verticale ou défilante injectée en Java.
 * COMMUNICATIONS :
 *   ← FamilyBudgetsAnalyticsActivity, RewardsActivity, AnalyticsActivity
 *   → MainActivity (retour via goHome avec CLEAR_TOP | SINGLE_TOP)
 *   ← SharedPreferences "masrouf_preferences" (lecture du rôle actif)
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity de base abstraite s'appuyant sur activity_section.xml (carte crème + barre retour)
 * et vérifiant éventuellement les restrictions de profil (Parent uniquement ou non-Parent).
 */
public abstract class SectionActivity extends AppCompatActivity {
    /** Vérifie les autorisations de profil, gonfle activity_section.xml et injecte la vue enfant. */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRole role=AppRole.from(getSharedPreferences("masrouf_preferences", MODE_PRIVATE)
                .getString("role", AppRole.USER.value()));
        // Bloque immédiatement l'écran si le rôle courant ne respecte pas les contraintes de la sous-classe.
        if ((requiresParentRole() && role != AppRole.PARENT) || (disallowParentRole() && role == AppRole.PARENT)) {
            Toast.makeText(this, R.string.nav_children, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_section);
        ((TextView) findViewById(R.id.section_title)).setText(titleResource());
        View content = createSectionContent();
        if (content != null) ((FrameLayout) findViewById(R.id.section_content)).addView(content,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /** Indique si l'écran est réservé au seul profil PARENT (déclenche un blocage sinon). */
    protected boolean requiresParentRole() { return false; }
    /** Indique si l'écran est interdit au profil PARENT (réservé aux comptes USER/CHILD). */
    protected boolean disallowParentRole() { return false; }
    /** Retourne le titre R.string.* affiché dans la barre supérieure de section. */
    protected abstract int titleResource();
    /** Construit la vue enfant injectée dans le conteneur FrameLayout R.id.section_content. */
    protected abstract View createSectionContent();

    /** Revient vers MainActivity en nettoyant la pile d'activités intermédiaires. */
    public void goHome(View ignored) {
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /** Fabrique un TextView de texte secondaire réutilisable par les sous-classes. */
    protected TextView bodyText(String text) {
        TextView body = new TextView(this); body.setText(text); body.setTextColor(getColor(R.color.muted_green));
        body.setTextSize(15f); body.setGravity(Gravity.CENTER_VERTICAL); return body;
    }
}
