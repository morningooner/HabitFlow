package com.example.habitflows;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

public class SystemEntranceAnim {
    public static void applySystemEntranceAnimation(View title, ViewGroup container, View logoutBtn) {
        // Initial setup
        title.setAlpha(0f);
        title.setTranslationY(-100f);

        container.setAlpha(0f);
        container.setScaleX(0f);
        container.setScaleY(0f);

        logoutBtn.setAlpha(0f);

        // Hide all children recursively
        setChildrenAlphaRecursive(container, 0f);

        // Phase 1: Title materializes
        title.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Phase 2: System Ignition (Point to Horizontal Line)
        container.postDelayed(() -> {
            container.setAlpha(1f);
            container.animate()
                    .scaleX(1f)
                    .scaleY(0.005f)
                    .setDuration(400)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        // Phase 3: Morphing Burst (Vertical expansion from center)
                        container.animate()
                                .scaleY(1f)
                                .setDuration(800)
                                .setInterpolator(new AnticipateOvershootInterpolator(1.2f))
                                .withEndAction(() -> {
                                    // Phase 4: Staggered Digital Render (Scan effect)
                                    animateChildrenCascading(container, 0);

                                    // Phase 5: Reveal Logout button
                                    logoutBtn.animate().alpha(1f).setDuration(1000).setStartDelay(500).start();
                                })
                                .start();
                    })
                    .start();
        }, 300);
    }

    private static void setChildrenAlphaRecursive(ViewGroup parent, float alpha) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.setAlpha(alpha);
            if (child instanceof ViewGroup) {
                setChildrenAlphaRecursive((ViewGroup) child, alpha);
            }
        }
    }

    private static int animateChildrenCascading(ViewGroup parent, int startDelay) {
        int currentDelay = startDelay;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.setTranslationY(20f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(currentDelay)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            currentDelay += 20;

            if (child instanceof ViewGroup && !(child instanceof Button)) {
                currentDelay = animateChildrenCascading((ViewGroup) child, currentDelay);
            }
        }
        return currentDelay;
    }
}
